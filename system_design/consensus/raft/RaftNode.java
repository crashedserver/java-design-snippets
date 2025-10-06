/*
 * Copyright 2025 crashedserver
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

package system_design.consensus.raft;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

import system_design.consensus.raft.client.NodeClient;
import system_design.consensus.raft.rpc.AppendEntriesRPC;
import system_design.consensus.raft.rpc.RequestVoteRPC;
import system_design.consensus.raft.server.IRaftServer;
import system_design.consensus.raft.server.NodeServer;
import system_design.consensus.raft.storage.IStorageService;
import system_design.consensus.raft.storage.model.CommandLog;
import system_design.consensus.raft.storage.model.LogEntry;
import system_design.consensus.raft.storage.model.NodeId;
import system_design.consensus.raft.storage.model.NodeState;
import system_design.consensus.raft.storage.model.RaftRole;
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
    private final ExecutorService diskIoExecutor;
    private final ExecutorService networkIoExecutor;
    private final ScheduledExecutorService applierExecutor;
    private final long minElectionTimeoutMillis;
    private final long maxElectionTimeoutMillis;
    private final int heartbeatIntervalMillis;
    private final IStorageService<T> storageService;
    private final StateMachine<T> stateMachine;
    private final IRaftLogic<T> raftLogic;
    private final boolean verboseLogging;
    private final Map<Integer, CompletableFuture<Boolean>> pendingCommands;
    private static final int MAX_PENDING_COMMANDS = 20000; // Back-pressure limit

    public RaftNode(RaftConfig<T> config, IStorageService<T> storageService) {
        this.selfId = config.selfId();
        this.storageService = storageService;
        this.stateMachine = config.stateMachine();
        this.peers = config.initialPeers();
        this.verboseLogging = config.verboseLogging();

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
        this.diskIoExecutor = Executors.newFixedThreadPool(4, r -> {
            Thread t = new Thread(r, "RaftNode-DiskIO-" + selfId.getNodeId());
            t.setDaemon(true);
            return t;
        });
        this.networkIoExecutor = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors() * 2, r -> {
            Thread t = new Thread(r, "RaftNode-NetworkIO-" + selfId.getNodeId());
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
        this.minElectionTimeoutMillis = config.minElectionTimeoutMillis();
        this.maxElectionTimeoutMillis = config.maxElectionTimeoutMillis();
        this.heartbeatIntervalMillis = config.heartbeatIntervalMillis();
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
        // Do not schedule a new timer if the executor is shutting down.
        if (electionTimerExecutor.isShutdown()) {
            return;
        }

        if (electionTimerFuture != null && !electionTimerFuture.isDone()) {
            electionTimerFuture.cancel(false);
        }
        long randomTimeout = ThreadLocalRandom.current().nextLong(minElectionTimeoutMillis,
                maxElectionTimeoutMillis + 1);
        electionTimerFuture = electionTimerExecutor.schedule(this::startElection, randomTimeout, TimeUnit.MILLISECONDS);
    }

    // --- Getters for RaftLogic ---

    @Override
    public NodeId getSelfId() {
        return selfId;
    }

    List<PeerState> getPeerStates() {
        return this.peers;
    }

    public List<Peer> getPeers() {
        return this.peers.stream().map(PeerState::getPeer).collect(Collectors.toList());
    }

    @Override
    public NodeState<T> getNodeState() {
        return nodeState;
    }

    IStorageService<T> getStorageService() {
        return storageService;
    }

    NodeClient getClient() {
        return client;
    }

    ExecutorService getDiskIoExecutor() {
        return diskIoExecutor;
    }

    ExecutorService getNetworkIoExecutor() {
        return networkIoExecutor;
    }

    long getMinElectionTimeoutMillis() {
        return minElectionTimeoutMillis;
    }

    boolean isVerboseLoggingEnabled() {
        return verboseLogging;
    }

    Map<Integer, CompletableFuture<Boolean>> getPendingCommands() {
        return pendingCommands;
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
        diskIoExecutor.shutdownNow();
        networkIoExecutor.shutdownNow();
        applierExecutor.shutdownNow();
        client.close();
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
            throw new IllegalStateException("This node is not the leader.");
        }

        // Back-pressure: If too many commands are in-flight, reject new ones.
        if (pendingCommands.size() > MAX_PENDING_COMMANDS) {
            throw new java.util.concurrent.RejectedExecutionException("Too many pending commands.");
        }

        CompletableFuture<Boolean> future = new CompletableFuture<>();
        synchronized (nodeState) {
            // Step 1: Append to the in-memory log first for speed.
            LogEntry<T> newEntry = new LogEntry<>(nodeState.getCurrentTerm(), command);
            int newIndex = nodeState.getLog().append(newEntry);
            pendingCommands.put(newIndex, future);

            // The leader's persistence can happen in parallel with replication.
            CompletableFuture.runAsync(() -> { // Use the dedicated disk I/O executor.
                storageService.appendLogEntries(List.of(newEntry));
            }, diskIoExecutor);
        }

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

        // Immediately cancel the election timer.
        if (electionTimerFuture != null) {
            electionTimerFuture.cancel(false);
        }

        // Initialize peer states.
        for (PeerState peerState : peers) {
            peerState.setNextIndex(nodeState.getLog().getLastLogIndex() + 1);
        }

        // Synchronously send the first heartbeat and wait for a majority to reply.
        // This is critical to establishing authority before accepting client
        // commands.
        boolean authorityEstablished = raftLogic.replicateLogToPeers(true);

        if (authorityEstablished) {
            // Now, start the periodic, asynchronous heartbeats.
            heartbeatExecutor.scheduleAtFixedRate(raftLogic::replicateLogToPeers, heartbeatIntervalMillis,
                    heartbeatIntervalMillis,
                    TimeUnit.MILLISECONDS);
        } else {
            // If authority could not be established, step down immediately to allow a new
            // election to proceed cleanly.
            raftLogic.stepDown(nodeState.getCurrentTerm());
        }
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

                        CompletableFuture<Boolean> future = pendingCommands.remove((int) indexToApply);
                        // Complete the future outside the synchronized block to avoid holding the lock
                        // while running client callback logic.
                        if (future != null) {
                            // Use a separate thread to avoid blocking the applier thread.
                            CompletableFuture.runAsync(() -> future.complete(true), diskIoExecutor);
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