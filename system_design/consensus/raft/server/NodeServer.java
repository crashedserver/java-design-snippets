package system_design.consensus.raft.server;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.atomic.AtomicBoolean;

import system_design.consensus.raft.rpc.AppendEntriesRPC;
import system_design.consensus.raft.rpc.RequestVoteRPC;
import system_design.consensus.raft.util.SerializationUtil;

public class NodeServer implements AutoCloseable {

    private final int minPort;
    private final int maxPort;
    private int port = -1; // The actual port the server is running on.
    private ServerSocket serverSocket;
    private ExecutorService executorService;
    private AtomicBoolean running = new AtomicBoolean(true);
    private IRaftServer raftServer;

    public NodeServer(int minPort, int maxPort, IRaftServer raftServer) {
        this.minPort = minPort;
        this.maxPort = maxPort;
        this.raftServer = raftServer;
    }

    /** @return The actual port the server is bound to, or -1 if not running. */
    public int getPort() {
        return this.port;
    }

    public void setRaftNode(IRaftServer raftServer) {
        this.raftServer = raftServer;
    }

    /**
     * Starts the server in a background thread.
     * 
     * @return A CompletableFuture that completes when the server has successfully
     *         bound to a port, or completes exceptionally if it fails.
     */
    public CompletableFuture<Void> start() {
        CompletableFuture<Void> future = new CompletableFuture<>();
        // Run the server loop in a new thread so this method is not blocking.
        new Thread(() -> runServerLoop(future), "NodeServer-MainLoop-" + minPort).start();
        return future;
    }

    private void runServerLoop(CompletableFuture<Void> startFuture) {
        // Use a fixed-size thread pool for predictable resource usage.
        // The size can be configured based on the expected number of peers.
        executorService = Executors.newFixedThreadPool(10, r -> {
            Thread t = new Thread(r, "NodeServer-ClientHandler");
            t.setDaemon(true);
            return t;
        });

        try {
            // Iterate through the port range to find an available port.
            for (int p = minPort; p <= maxPort; p++) {
                try {
                    serverSocket = new ServerSocket(p);
                    this.port = p; // Port is available, store it.
                    System.out.println("Raft Node server started on port " + this.port);
                    break; // Exit the loop once a port is successfully bound.
                } catch (IOException e) {
                    System.out.println("Port is in use, trying the next one!");
                }
            }

            if (serverSocket == null) {
                IOException ex = new IOException(
                        "Could not find an available port in the range [" + minPort + "-" + maxPort + "]");
                startFuture.completeExceptionally(ex);
                throw ex;
            }

            // Signal that the server is up and running.
            startFuture.complete(null);

            while (running.get()) {
                try {
                    final Socket clientSocket = serverSocket.accept();
                    executorService.submit(new ClientHandler(clientSocket, raftServer));
                } catch (RejectedExecutionException e) {
                    if (running.get()) {
                        System.err.println("Failed to submit client handler to executor: " + e.getMessage());
                    }
                } catch (IOException e) {
                    if (running.get()) {
                        System.err.println("Server error while accepting client connection: " + e.getMessage());
                    }
                }
            }
        } catch (IOException e) {
            if (running.get()) {
                System.err.println("Server error: " + e.getMessage());
            }
        } finally {
            stop();
        }
    }

    @Override
    public void close() {
        stop();
    }

    public void stop() {
        if (!running.get()) {
            return;
        }
        running.set(false);
        try {
            if (serverSocket != null && !serverSocket.isClosed()) {
                serverSocket.close();
            }
        } catch (IOException e) {
            System.err.println("Error closing server socket: " + e.getMessage());
        }
        if (executorService != null) {
            // Use shutdownNow() to interrupt running tasks and ensure the thread pool
            // terminates, preventing the application from hanging.
            executorService.shutdownNow();
        }
        System.out.println("Server on port " + this.port + " stopped.");
    }

    private static class ClientHandler implements Runnable {
        private final Socket clientSocket;

        private final IRaftServer raftServer;

        ClientHandler(Socket socket, IRaftServer raftServer) {
            this.clientSocket = socket;
            this.raftServer = raftServer;
        }

        @Override
        public void run() {
            try (clientSocket;
                    InputStream input = clientSocket.getInputStream();
                    OutputStream output = clientSocket.getOutputStream()) {
                // Keep the connection open and handle multiple RPCs.
                while (!clientSocket.isClosed()) {
                    // Read the 4-byte length prefix.
                    byte[] lengthBytes = input.readNBytes(4);
                    if (lengthBytes.length == 0) {
                        break; // Client closed the connection.
                    }
                    int length = java.nio.ByteBuffer.wrap(lengthBytes).getInt();
                    byte[] requestBytes = input.readNBytes(length);

                    byte[] responseBytes = handleRpc(requestBytes);
                    byte[] responseLength = java.nio.ByteBuffer.allocate(4).putInt(responseBytes.length).array();
                    output.write(responseLength);
                    output.write(responseBytes);
                }
            } catch (Exception e) {
                System.err.println(
                        "Error handling client " + clientSocket.getRemoteSocketAddress() + ": " + e.getMessage());
            } finally {
                try {
                    clientSocket.close();
                } catch (IOException e) {
                    System.out.println("Error during client socket close!");
                }
            }
        }

        private byte[] handleRpc(byte[] request) {
            if (request == null || request.length == 0) {
                return "ERROR: Empty request".getBytes();
            }

            // Protocol: First byte is the RPC type identifier.
            byte rpcType = request[0];
            String payload = new String(request, 1, request.length - 1);

            // The RaftNode would contain the real state and consensus logic.
            switch (rpcType) {
                case 1: // RequestVote RPC
                    RequestVoteRPC.Request voteRequest = SerializationUtil.deserializeRequestVote(payload);
                    // Delegate the RPC to the main RaftNode instance for processing.
                    RequestVoteRPC.Reply voteReply = raftServer.handleRequestVote(voteRequest);
                    return SerializationUtil.serialize(voteReply).getBytes();

                case 2: // AppendEntries RPC
                    AppendEntriesRPC.Reply appendReply = raftServer
                            .handleAppendEntries(SerializationUtil.deserializeAppendEntries(payload));
                    return SerializationUtil.serialize(appendReply).getBytes();

                default:
                    return ("ERROR: Unknown RPC type " + rpcType).getBytes();
            }
        }
    }
}