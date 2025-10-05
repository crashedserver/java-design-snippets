/*
 * Copyright 2024 crashedserver
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package system_design.consensus.raft.client;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import system_design.consensus.raft.rpc.AppendEntriesRPC;
import system_design.consensus.raft.rpc.RequestVoteRPC;
import system_design.consensus.raft.util.SerializationUtil;

/**
 * A client for sending RPCs to other Raft nodes.
 */
public class NodeClient {

    // A simple connection pool to reuse sockets and avoid ephemeral port
    // exhaustion.
    private final Map<String, Socket> connectionPool = new ConcurrentHashMap<>();

    /**
     * Sends a RequestVote RPC to a target node.
     *
     * @param targetHost The hostname of the target node.
     * @param targetPort The port of the target node.
     * @param request    The RequestVote RPC request object.
     * @return The reply from the target node.
     * @throws IOException if a network error occurs.
     */
    public RequestVoteRPC.Reply sendRequestVote(String targetHost, int targetPort, RequestVoteRPC.Request request)
            throws IOException {
        byte[] payloadBytes = SerializationUtil.serialize(request).getBytes(StandardCharsets.UTF_8);
        byte[] fullRequest = new byte[1 + payloadBytes.length];
        fullRequest[0] = 1; // RPC Type 1 for RequestVote
        System.arraycopy(payloadBytes, 0, fullRequest, 1, payloadBytes.length);
        try {
            byte[] responseBytes = sendRpc(targetHost, targetPort, fullRequest);
            return SerializationUtil.deserializeVoteReply(new String(responseBytes, StandardCharsets.UTF_8));
        } catch (IOException e) {
            // If an error occurs, remove the potentially broken socket from the pool.
            invalidateConnection(targetHost, targetPort);
            throw e;
        }
    }

    public AppendEntriesRPC.Reply sendAppendEntries(String targetHost, int targetPort,
            AppendEntriesRPC.Request<?> request) throws IOException {
        byte[] payloadBytes = SerializationUtil.serialize(request).getBytes(StandardCharsets.UTF_8);
        byte[] fullRequest = new byte[1 + payloadBytes.length];
        fullRequest[0] = 2; // RPC Type 2 for AppendEntries
        System.arraycopy(payloadBytes, 0, fullRequest, 1, payloadBytes.length);
        try {
            byte[] responseBytes = sendRpc(targetHost, targetPort, fullRequest);
            return SerializationUtil.deserializeAppendEntriesReply(new String(responseBytes, StandardCharsets.UTF_8));
        } catch (IOException e) {
            invalidateConnection(targetHost, targetPort);
            throw e;
        }
    }

    private byte[] sendRpc(String host, int port, byte[] requestBytes) throws IOException {
        String target = host + ":" + port;
        Socket socket = connectionPool.get(target);

        // If socket is null or closed, create a new one.
        if (socket == null || socket.isClosed()) {
            synchronized (target.intern()) {
                // Double-check in case another thread created the socket while we were waiting.
                socket = connectionPool.get(target);
                if (socket == null || socket.isClosed()) {
                    socket = new Socket(host, port);
                    connectionPool.put(target, socket);
                }
            }
        }

        // Synchronize on the socket object to ensure that writing a request and
        // reading a response is an atomic operation per socket.
        synchronized (socket) {
            OutputStream out = socket.getOutputStream();
            // Protocol: Send 4-byte length prefix, then the payload.
            out.write(java.nio.ByteBuffer.allocate(4).putInt(requestBytes.length).array());
            out.write(requestBytes);
            out.flush();

            InputStream in = socket.getInputStream();
            byte[] lengthBytes = in.readNBytes(4);
            if (lengthBytes.length < 4) {
                throw new IOException("Connection closed while reading response length.");
            }
            int length = java.nio.ByteBuffer.wrap(lengthBytes).getInt();
            return in.readNBytes(length);
        }
    }

    private void invalidateConnection(String host, int port) {
        String target = host + ":" + port;
        Socket socket = connectionPool.remove(target);
        if (socket != null) {
            try {
                socket.close();
            } catch (IOException e) {
                // It's safe to ignore exceptions when closing a socket during cleanup.
                // The primary goal is to release the resource, and an error here is
                // non-critical.
            }
        }
    }

    public void close() {
        for (Socket socket : connectionPool.values()) {
            try {
                socket.close();
            } catch (IOException e) {
                // It's safe to ignore exceptions during a final shutdown cleanup.
            }
        }
        connectionPool.clear();
    }
}
