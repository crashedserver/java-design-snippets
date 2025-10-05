package system_design.consensus.raft;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

import system_design.consensus.raft.client.NodeClient;
import system_design.consensus.raft.rpc.RequestVoteRPC;
import system_design.consensus.raft.rpc.AppendEntriesRPC;
import system_design.consensus.raft.server.IRaftServer;
import system_design.consensus.raft.server.NodeServer;
import system_design.consensus.raft.storage.model.NodeId;
import system_design.consensus.raft.storage.IStorageService;
import java.util.List;
import system_design.consensus.raft.storage.model.CommandLog;
import system_design.consensus.raft.storage.model.LogEntry;
import system_design.consensus.raft.storage.model.NodeState;
import system_design.consensus.raft.storage.model.RaftRole;
import java.util.Map;
import system_design.consensus.raft.storage.model.Term;

/**
 * The central class representing a single member of the Raft cluster.
 * It manages state transitions, timers, and communication with other nodes.
 */

public class RaftNode<T> implements IRaftNode<T>, IRaftServer {

    private final NodeState<T> nodeState;
    private final NodeServer server;
    private final NodeClient client;
    private final NodeId selfId;
    private final List<PeerState> peers;
    private final ScheduledExecutorService electionTimerExecutor;
    private ScheduledFuture<?> electionTimerFuture;
    private final ScheduledExecutorService heartbeatExecutor;
    private final ScheduledExecutorService applierExecutor;
    private final long minElectionTimeoutMillis;
    private final long maxElectionTimeoutMillis;
    private final IStorageService<T> storageService;
    private final StateMachine<T> stateMachine;
    private final IRaftLogic<T> raftLogic;
    private final Map<Integer, CompletableFuture<Boolean>> pendingCommands;

    public RaftNode(RaftConfig<T> config, IStorageService<T> storageService) {
        this.selfId = config.selfId();
        this.storageService = storageService;
        this.stateMachine = config.stateMachine();
        this.peers = config.initialPeers();

        // Load persistent state from storage on startup.
        Term savedTerm = storageService.loadTerm();
        NodeId savedVotedFor = storageService.loadVotedFor();
        CommandLog<T> savedLog = storageService.loadLog();
        this.nodeState = new NodeState<>(savedTerm, savedVotedFor, savedLog);
        System.out.printf("Node %s starting up. Loaded Term: %d, Voted For: %s, Log Size: %d\n", selfId.getNodeId(),
                savedTerm.getTerm(), savedVotedFor, savedLog.size());

        // Start Node Server to accept the RPCs
        this.server = new NodeServer(config.minPort(), config.maxPort(), this);
        // Node Client to accept send the RPCs to other nodes
        this.client = new NodeClient();

        // Define executers for election, hearbeat and applier of the changes
        this.electionTimerExecutor = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "RaftNode-ElectionTimer-" + selfId.getNodeId());
            t.setDaemon(true);
            return t;
        });
        this.heartbeatExecutor = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "RaftNode-Heartbeat-" + selfId.getNodeId());
            t.setDaemon(true);
            return t;
        });
        // Use a single thread for the applier to ensure committed entries are applied
        // sequentially and in order, which is a core requirement of Raft.
        this.applierExecutor = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "RaftNode-Applier-" + selfId.getNodeId());
            t.setDaemon(true);
            return t;
        });
        this.minElectionTimeoutMillis = 150;
        this.maxElectionTimeoutMillis = 300;
        this.raftLogic = new RaftLogic<>(this);
        this.pendingCommands = new ConcurrentHashMap<>();
    }

    /**
     * Starts the node's server and the Raft protocol logic.
     * 
     * @return A CompletableFuture that completes when the server is running.
     */
    @Override
    public CompletableFuture<Void> start() {
        CompletableFuture<Void> serverFuture = server.start();
        // Once the server is confirmed to be running, start the Raft protocol timers.
        serverFuture.thenRun(this::startProtocol);
        return serverFuture;
    }

    private void startProtocol() {
        System.out.printf("Node %s starting Raft protocol...\n", selfId.getNodeId());
        resetElectionTimer();
        // Start the dedicated applier thread.
        applierExecutor.submit(this::applyCommittedEntries);
    }

    /**
     * Resets the election timer to a new random timeout.
     * This is called when a node starts up, or when it receives a valid heartbeat
     * from a leader.
     */
    void resetElectionTimer() {
        if (electionTimerFuture != null && !electionTimerFuture.isDone()) {
            electionTimerFuture.cancel(false);
        }
        long randomTimeout = ThreadLocalRandom.current().nextLong(minElectionTimeoutMillis,
                maxElectionTimeoutMillis + 1);
        electionTimerFuture = electionTimerExecutor.schedule(this::startElection, randomTimeout, TimeUnit.MILLISECONDS);
    }

    public List<Peer> getPeers() {
        return this.peers.stream().map(PeerState::getPeer).collect(Collectors.toList());
    }

    // --- Getters for RaftLogic ---

    @Override
    public NodeId getSelfId() {
        return selfId;
    }

    List<PeerState> getPeerStates() {
        return this.peers;
    }

    NodeState<T> getNodeState() {
        return nodeState;
    }

    IStorageService<T> getStorageService() {
        return storageService;
    }

    NodeClient getClient() {
        return client;
    }

    @Override
    public int getPort() {
        return server.getPort();
    }

    @Override
    public synchronized void stop() {
        server.stop();
        electionTimerExecutor.shutdownNow();
        heartbeatExecutor.shutdownNow();
        applierExecutor.shutdownNow();
    }

    public CommandLog<T> getLog() {
        return nodeState.getLog();
    }

    /**
     * The entry point for a client to submit a command to the cluster.
     * If this node is not the leader, it should reject the command.
     * 
     * @param command The command to be replicated.
     * @return A CompletableFuture that will complete when the command is committed.
     */
    @Override
    public CompletableFuture<Boolean> submitCommand(T command) {
        if (nodeState.getCurrentRole() != RaftRole.LEADER) {
            // In a real system, you would redirect the client to the leader.
            return CompletableFuture.failedFuture(new IllegalStateException("This node is not the leader."));
        }

        CompletableFuture<Boolean> future = new CompletableFuture<>();
        synchronized (nodeState) {
            // Step 1: Append to the in-memory log first for speed.
            LogEntry<T> newEntry = new LogEntry<>(nodeState.getCurrentTerm(), command);
            int newIndex = nodeState.getLog().append(newEntry);
            pendingCommands.put(newIndex, future);

            // The leader's persistence can happen in parallel with replication.
            CompletableFuture.runAsync(() -> {
                storageService.appendLogEntries(List.of(newEntry));
            });
        }

        raftLogic.replicateLogToPeers();

        return future;
    }

    /**
     * Simulates a node's election timer expiring, causing it to start an election.
     */
    public synchronized void startElection() {
        raftLogic.startElection();
    }

    /**
     * Transitions the node to the Leader state and starts sending heartbeats.
     */
    void becomeLeader() {
        // Use a synchronized block to ensure the transition is atomic.
        synchronized (nodeState) {
            // A node can only become a leader if it is currently a candidate.
            if (nodeState.getCurrentRole() != RaftRole.CANDIDATE) {
                return;
            }
            nodeState.setCurrentRole(RaftRole.LEADER); // Transition to Leader

            // When becoming a leader, clear any pending commands from previous terms.
            pendingCommands.values().forEach(future -> future.completeExceptionally(
                    new IllegalStateException("Leadership changed before command could be committed.")));
            pendingCommands.clear();
        }
        System.out.printf("!!! Node %s has become the LEADER for term %d !!!\n", selfId.getNodeId(),
                nodeState.getCurrentTerm().getTerm());
        // Immediately cancel the election timer, as leaders don't have one.
        if (electionTimerFuture != null) {
            electionTimerFuture.cancel(false);
        }

        // Immediately send a heartbeat to establish authority. This is a synchronous
        // call.
        raftLogic.replicateLogToPeers();

        // Initialize nextIndex for all peers to the leader's last log index + 1.
        synchronized (nodeState) {
            for (PeerState peerState : peers) {
                peerState.setNextIndex(nodeState.getLog().getLastLogIndex() + 1);
            }
        }

        // Start sending heartbeats to all peers.
        long heartbeatInterval = 50; // ms
        heartbeatExecutor.scheduleAtFixedRate(raftLogic::replicateLogToPeers, 0, heartbeatInterval,
                TimeUnit.MILLISECONDS);
    }

    // --- RPC Handler Methods ---
    private void applyCommittedEntries() {
        try {
            // This is a long-running task for the single applier thread.
            while (!applierExecutor.isShutdown()) {
                synchronized (nodeState) {
                    // Wait until there are new entries to apply.
                    while (nodeState.getCommitIndex().get() <= nodeState.getLastAppliedIndex().get()) {
                        nodeState.wait();
                    }

                    // Apply all newly committed entries.
                    while (nodeState.getCommitIndex().get() > nodeState.getLastAppliedIndex().get()) {
                        long indexToApply = nodeState.getLastAppliedIndex().get() + 1;
                        LogEntry<T> entry = nodeState.getLog().getEntry((int) indexToApply);
                        if (entry != null) {
                            stateMachine.apply(entry.getCommand());
                        }
                        nodeState.setLastAppliedIndex(new AtomicLong(indexToApply));

                        if (pendingCommands.containsKey((int) indexToApply)) {
                            pendingCommands.remove((int) indexToApply).complete(true);
                        }
                    }
                }
            }
        } catch (InterruptedException e) {
            // This is expected during shutdown.
            Thread.currentThread().interrupt(); // Preserve the interrupted status.
        } catch (Exception e) {
            System.err.printf("Node %s encountered an error applying committed entries: %s\n", selfId.getNodeId(),
                    e.getMessage());
        }
    }

    public RequestVoteRPC.Reply handleRequestVote(RequestVoteRPC.Request request) {
        return raftLogic.handleRequestVote(request);
    }

    @Override
    @SuppressWarnings("unchecked")
    public AppendEntriesRPC.Reply handleAppendEntries(AppendEntriesRPC.Request<Object> request) {
        return raftLogic.handleAppendEntries((AppendEntriesRPC.Request<T>) request);
    }

    public record Peer(String host, int port) {
    }

    @Override
    public RaftRole getRole() {
        return nodeState.getCurrentRole();
    }

    @Override
    public String getFromStateMachine(String key) {
        return stateMachine.get(key);
    }
}