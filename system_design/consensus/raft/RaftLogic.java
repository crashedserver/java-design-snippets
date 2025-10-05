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

package system_design.consensus.raft;

import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import system_design.consensus.raft.client.NodeClient;
import system_design.consensus.raft.rpc.AppendEntriesRPC;
import system_design.consensus.raft.rpc.RequestVoteRPC;
import system_design.consensus.raft.storage.IStorageService;
import system_design.consensus.raft.storage.model.LogEntry;
import system_design.consensus.raft.storage.model.NodeId;
import system_design.consensus.raft.storage.model.NodeState;
import system_design.consensus.raft.storage.model.RaftRole;
import system_design.consensus.raft.storage.model.Term;

/**
 * Encapsulates the core business logic of the Raft consensus algorithm.
 * This class is responsible for handling RPCs and managing state transitions.
 */
public class RaftLogic<T> implements IRaftLogic<T> {

    private final RaftNode<T> raftNode;
    private final NodeState<T> nodeState;
    private final IStorageService<T> storageService;
    private final NodeClient client;
    private final NodeId selfId;
    private final List<PeerState> peers;
    private final ExecutorService networkIoExecutor;
    private AtomicInteger votesReceived;

    /**
     * Initializes the RaftLogic with a reference to its parent RaftNode.
     *
     * @param raftNode The RaftNode that this logic instance belongs to.
     */
    public RaftLogic(RaftNode<T> raftNode) {
        this.raftNode = raftNode;
        this.nodeState = raftNode.getNodeState();
        this.storageService = raftNode.getStorageService();
        this.client = raftNode.getClient();
        this.selfId = raftNode.getSelfId();
        this.peers = raftNode.getPeerStates();
        this.networkIoExecutor = raftNode.getNetworkIoExecutor();
    }

    /**
     * Starts an election process as per the Raft protocol.
     * This method is called when a follower's election timer expires.
     * The node transitions to the CANDIDATE state, increments its term, votes for
     * itself,
     * and sends RequestVote RPCs to all its peers.
     */
    @Override
    public synchronized void startElection() {
        // Only followers or candidates (in case of a split vote timeout) can start an
        // election.
        if (nodeState.getCurrentRole() == RaftRole.LEADER) {
            return;
        }
        // Become a CANDIDATE
        nodeState.setCurrentRole(RaftRole.CANDIDATE);
        nodeState.incrementTerm();
        storageService.saveTerm(nodeState.getCurrentTerm());
        nodeState.setVotedFor(selfId);
        storageService.saveVotedFor(selfId);
        if (raftNode.isVerboseLoggingEnabled()) {
            System.out.printf("Node %s starting election for term %d\n", selfId.getNodeId(),
                    nodeState.getCurrentTerm().getTerm());
        }
        // Self vote
        this.votesReceived = new AtomicInteger(1);

        int lastLogIndex = nodeState.getLog().getLastLogIndex();
        Term lastLogTerm = nodeState.getLog().getLastLogTerm();

        RequestVoteRPC.Request request = new RequestVoteRPC.Request(
                nodeState.getCurrentTerm(), selfId.getNodeId(), lastLogIndex, lastLogTerm);

        // Reset the election timer for the new election round.
        raftNode.resetElectionTimer();

        for (PeerState peerState : peers) {
            CompletableFuture.runAsync(() -> {
                if (nodeState.getCurrentRole() != RaftRole.CANDIDATE) {
                    return;
                }
                try {
                    RaftNode.Peer peer = peerState.getPeer();
                    if (raftNode.isVerboseLoggingEnabled()) {
                        System.out.printf("   -> Node %s sending RequestVote to %s:%d\n", selfId.getNodeId(),
                                peer.host(),
                                peer.port());
                    }
                    RequestVoteRPC.Reply reply = client.sendRequestVote(peer.host(), peer.port(), request);
                    if (raftNode.isVerboseLoggingEnabled()) {
                        System.out.printf("   <- Node %s received vote reply from %s:%d (Granted: %b)\n",
                                selfId.getNodeId(), peer.host(), peer.port(), reply.voteGranted());
                    }

                    if (reply.voteGranted() && nodeState.getCurrentRole() == RaftRole.CANDIDATE) {
                        int currentVotes = votesReceived.incrementAndGet();

                        int majority = (peers.size() + 1) / 2 + 1;
                        // If we have received votes from a majority of the cluster, become the leader.
                        if (currentVotes >= majority) {
                            raftNode.becomeLeader();
                        }
                    }
                } catch (IOException e) {
                    System.err.printf("   !! Node %s failed to send RequestVote to %s:%d: %s\n", selfId.getNodeId(),
                            peerState.getPeer().host(), peerState.getPeer().port(), e.getMessage());
                }
            }, networkIoExecutor);
        }
    }

    /**
     * Replicates log entries to all peers. This method is called by the leader on
     * every heartbeat.
     * It serves two purposes: to assert the leader's authority (heartbeat) and to
     * bring followers' logs up-to-date.
     *
     * <p>
     * <b>The Log Reconciliation Process:</b>
     * For each peer, the leader maintains a {@code nextIndex}, which is an
     * optimistic guess of the next log entry the follower needs.
     * <ol>
     * <li>The leader constructs an {@code AppendEntries} RPC containing entries
     * starting from {@code nextIndex}. The RPC also includes
     * the index and term of the entry just before {@code nextIndex}
     * ({@code prevLogIndex} and {@code prevLogTerm}).</li>
     * <li>The follower receives the RPC and checks if its own log has an entry at
     * {@code prevLogIndex} that matches {@code prevLogTerm}.</li>
     * <li><b>If it matches:</b> The follower's log is consistent up to that point.
     * It appends the new entries and replies with success. The leader then advances
     * {@code nextIndex} for that follower.</li>
     * <li><b>If it does not match:</b> The follower's log is inconsistent. It
     * replies with failure. The leader then decrements {@code nextIndex} and
     * retries on the next heartbeat. This "probing" continues until a common log
     * entry is found.</li>
     * </ol>
     * This decrement-and-retry mechanism allows the leader to efficiently find the
     * point of log divergence and repair it.
     */
    @Override
    public boolean replicateLogToPeers(boolean waitForMajority) {
        if (waitForMajority) {
            int majorityCount = (peers.size() / 2) + 1;
            CountDownLatch majorityLatch = new CountDownLatch(majorityCount);
            for (PeerState peerState : peers) {
                networkIoExecutor.submit(() -> {
                    if (replicateToPeer(peerState)) {
                        majorityLatch.countDown();
                    }
                });
            }
            try {
                // Block until a majority of peers have successfully replied.
                long assertionTimeout = raftNode.getMinElectionTimeoutMillis() / 2L;
                boolean majorityReached = majorityLatch.await(assertionTimeout,
                        TimeUnit.MILLISECONDS);
                if (!majorityReached) {
                    System.err
                            .println("Warning: Timed out waiting for majority acknowledgment on leadership assertion.");
                }
                return majorityReached;
            } catch (Exception e) {
                Thread.currentThread().interrupt();
                return false;
            }
        }
        // This path should not be taken for synchronous calls.
        return false;
    }

    @Override
    public void replicateLogToPeers() {
        for (PeerState peerState : peers) {
            networkIoExecutor.submit(() -> replicateToPeer(peerState));
        }
    }

    private boolean replicateToPeer(PeerState peerState) {
        int prevLogIndex = peerState.getNextIndex() - 1;
        Term prevLogTerm = new Term(0);
        if (prevLogIndex > 0) {
            LogEntry<T> prevEntry = nodeState.getLog().getEntry(prevLogIndex);
            // Handle the case where the leader's log has been truncated.
            if (prevEntry != null) {
                prevLogTerm = prevEntry.getTerm();
            }
        }

        List<LogEntry<T>> entriesToSend = nodeState.getLog().getEntriesFrom(peerState.getNextIndex());

        AppendEntriesRPC.Request<T> request = new AppendEntriesRPC.Request<>(
                nodeState.getCurrentTerm(), selfId.getNodeId(), prevLogIndex, prevLogTerm, entriesToSend,
                (int) nodeState.getCommitIndex().get());
        try {
            RaftNode.Peer peer = peerState.getPeer();
            AppendEntriesRPC.Reply reply = client.sendAppendEntries(peer.host(), peer.port(), request);

            boolean shouldAdvanceCommit;
            synchronized (nodeState) {
                if (reply.success()) {
                    // If successful, update nextIndex and matchIndex for the follower.
                    peerState.setNextIndex(request.prevLogIndex() + entriesToSend.size() + 1);
                    peerState.setMatchIndex(request.prevLogIndex() + entriesToSend.size());
                    shouldAdvanceCommit = true;
                } else {
                    // If AppendEntries fails because of log inconsistency, decrement nextIndex and
                    // retry.
                    if (reply.term().getTerm() <= nodeState.getCurrentTerm().getTerm()) {
                        // Only decrement if the reply is not from a node with a higher term.
                        peerState.setNextIndex(Math.max(1, peerState.getNextIndex() - 1));
                    }
                    shouldAdvanceCommit = false;
                }
            }
            if (shouldAdvanceCommit) {
                advanceCommitIndex();
            }
            return reply.success();
        } catch (IOException e) {
            // Only log the error if the node is still running. This suppresses expected
            // errors during shutdown.
            if (raftNode.getPort() != -1 && peerState.shouldLogError()) {
                System.err.printf("Leader %s failed to send heartbeat to %s:%d: %s\n", selfId.getNodeId(),
                        peerState.getPeer().host(), peerState.getPeer().port(), e.getMessage());
            }
            return false;
        }
    }

    /**
     * Advances the leader's commit index.
     * A log entry is considered committed if it is stored on a majority of servers.
     * This method iterates backwards from the end of the log to find the highest
     * index
     * that has been replicated on a majority of nodes.
     */
    public void advanceCommitIndex() {
        // A leader can only commit entries from its own term.
        // This is a crucial safety rule from the Raft paper (Section 5.4.2).
        // To find the new commit index, we find the median of all matchIndex values.

        List<Integer> matchIndexes = new java.util.ArrayList<>(peers.size() + 1);
        // Add the leader's own log index
        matchIndexes.add(nodeState.getLog().getLastLogIndex());
        // Add the matchIndex for all followers
        for (PeerState peer : peers) {
            matchIndexes.add(peer.getMatchIndex());
        }

        // Sort the indexes to easily find the median
        Collections.sort(matchIndexes);

        // The commit index is the one replicated on the majority of servers.
        // In a sorted list, this is the one at index (N-1)/2 for a cluster of size N.
        int majorityIndex = (matchIndexes.size() - 1) / 2;
        int newCommitIndex = matchIndexes.get(majorityIndex);

        if (newCommitIndex > nodeState.getCommitIndex().get()) {
            // Check if the entry at the new commit index is from the current term.
            LogEntry<T> entry = nodeState.getLog().getEntry(newCommitIndex);
            if (entry != null && entry.getTerm().getTerm() == nodeState.getCurrentTerm().getTerm()) {
                synchronized (nodeState) {
                    nodeState.setCommitIndex(new AtomicLong(newCommitIndex));
                    nodeState.notifyAll(); // Notify the applier thread that new entries are committed.
                }
            }
        }
    }

    /**
     * Handles an incoming RequestVote RPC from a candidate.
     * The vote is granted if:
     * 1. The candidate's term is not older than the current node's term.
     * 2. The current node has not already voted in this term (or has voted for the
     * same candidate).
     * 3. The candidate's log is at least as up-to-date as the current node's log.
     * 
     * @param request The RequestVote RPC from the candidate.
     * @return A reply indicating whether the vote was granted.
     */
    @Override
    public RequestVoteRPC.Reply handleRequestVote(RequestVoteRPC.Request request) {
        synchronized (nodeState) {
            if (raftNode.isVerboseLoggingEnabled()) {
                System.out.printf("Node %s received RequestVote from %s for term %d.\n", selfId.getNodeId(),
                        request.candidateId(), request.term().getTerm());
            }

            if (request.term().getTerm() > nodeState.getCurrentTerm().getTerm()) {
                // If the request has a higher term, this node is stale. Step down to follower
                // and update to the new term.
                System.out.printf(" -> Stale term detected. Stepping down to Follower for new term %d.\n",
                        request.term().getTerm());
                stepDown(request.term()); // Step down and then evaluate the vote.
            }

            if (request.term().getTerm() < nodeState.getCurrentTerm().getTerm()) {
                return new RequestVoteRPC.Reply(nodeState.getCurrentTerm(), false);
            }

            // At this point, request.term() >= currentTerm.
            // Check if we have already voted for someone else in this term.
            boolean hasNotVoted = nodeState.getVotedFor() == null
                    || nodeState.getVotedFor().getNodeId().equals(request.candidateId());

            boolean logIsUpToDate = isCandidateLogUpToDate(request);

            if (hasNotVoted && logIsUpToDate) { // Raft safety check
                // If all checks pass, grant the vote.
                nodeState.setVotedFor(new NodeId(request.candidateId()));
                storageService.saveVotedFor(nodeState.getVotedFor());
                raftNode.resetElectionTimer();
                if (raftNode.isVerboseLoggingEnabled()) {
                    System.out.printf(" -> Granting vote for %s in term %d\n", request.candidateId(),
                            nodeState.getCurrentTerm().getTerm());
                }
                return new RequestVoteRPC.Reply(nodeState.getCurrentTerm(), true);
            } else {
                // Reset the election timer even if the vote is denied. The receipt of a
                // RequestVote RPC from a valid term is a form of communication that
                // should prevent this node from timing out and starting its own election.
                raftNode.resetElectionTimer();
                return new RequestVoteRPC.Reply(nodeState.getCurrentTerm(), false);
            }
        }
    }

    /**
     * Implements the Raft safety rule for voting. A candidate's log is considered
     * "at least as up-to-date" if its last log entry has a higher term, or if
     * the terms are the same and its log is longer or equal in length.
     *
     * @param request The RequestVote RPC from the candidate.
     * @return True if the candidate's log is up-to-date.
     */
    private boolean isCandidateLogUpToDate(RequestVoteRPC.Request request) {
        Term localLastLogTerm = nodeState.getLog().getLastLogTerm();
        int localLastLogIndex = nodeState.getLog().getLastLogIndex();
        return request.lastLogTerm().getTerm() > localLastLogTerm.getTerm() ||
                (request.lastLogTerm().getTerm() == localLastLogTerm.getTerm() &&
                        request.lastLogIndex() >= localLastLogIndex);
    }

    /**
     * Transitions the current node to the Follower state for a new term.
     * This involves updating the term, resetting the role and vote, persisting
     * state, and resetting the election timer.
     * 
     * @param newTerm The new, higher term that was discovered.
     */
    @Override
    public void stepDown(Term newTerm) {
        nodeState.setCurrentTerm(newTerm);
        nodeState.setCurrentRole(RaftRole.FOLLOWER);
        nodeState.setVotedFor(null);
        storageService.saveVotedFor(null);
        storageService.saveTerm(nodeState.getCurrentTerm());

        // When stepping down, fail any pending commands that this node was tracking as
        // a leader.
        raftNode.getPendingCommands().values().forEach(future -> future.completeExceptionally(
                new IllegalStateException("Leadership changed before command could be committed.")));
        raftNode.getPendingCommands().clear();

        raftNode.resetElectionTimer();
    }

    /**
     * Handles an incoming AppendEntries RPC from a leader.
     * This method is used for both heartbeats and log replication.
     * It performs several checks:
     * 1. Term check: Rejects requests from leaders with a stale term.
     * 2. Log consistency check: Ensures the log matches the leader's at
     * prevLogIndex.
     * 3. Appends new entries and truncates conflicting ones.
     * 4. Updates the commitIndex based on the leader's commitIndex.
     * 
     * @param request The AppendEntries RPC from the leader.
     * @return A reply indicating if the append was successful.
     */
    @Override
    public AppendEntriesRPC.Reply handleAppendEntries(AppendEntriesRPC.Request<T> request) {
        synchronized (nodeState) {
            if (raftNode.isVerboseLoggingEnabled()) {
                if (!request.entries().isEmpty()) {
                    System.out.printf("Node %s received AppendEntries from %s for term %d.\n", selfId.getNodeId(),
                            request.leaderId(), request.term().getTerm());
                }
            }

            // Rule 1: Reply false if term < currentTerm
            if (request.term().getTerm() < nodeState.getCurrentTerm().getTerm()) {
                return new AppendEntriesRPC.Reply(nodeState.getCurrentTerm(), false);
            }

            // If RPC request or response contains term T > currentTerm: set currentTerm =
            // T,
            // convert to follower.
            if (request.term().getTerm() > nodeState.getCurrentTerm().getTerm()) {
                stepDown(request.term());
            } else {
                // If we receive a valid heartbeat, we are not the leader, so reset the timer.
                raftNode.resetElectionTimer();
                if (request.term().getTerm() == nodeState.getCurrentTerm().getTerm()) {
                    if (nodeState.getCurrentRole() == RaftRole.CANDIDATE) {
                        // If a candidate receives an AppendEntries from a new leader in the same
                        // term, it transitions to follower.
                        System.out.printf(" -> Discovered leader %s while in Candidate state. Stepping down.\n",
                                request.leaderId());
                        nodeState.setCurrentRole(RaftRole.FOLLOWER);
                    }
                }
            }

            // Rule 2: Reply false if log doesn’t contain an entry at prevLogIndex whose
            // term matches prevLogTerm.
            if (request.prevLogIndex() > 0) {
                LogEntry<T> prevEntry = nodeState.getLog().getEntry(request.prevLogIndex());
                if (prevEntry == null || prevEntry.getTerm().getTerm() != request.prevLogTerm().getTerm()) {
                    return new AppendEntriesRPC.Reply(nodeState.getCurrentTerm(), false);
                }
            }

            // Rule 3: If an existing entry conflicts with a new one (same index but
            // different terms), delete the existing entry and all that follow it.
            for (int i = 0; i < request.entries().size(); i++) {
                int logIndex = request.prevLogIndex() + 1 + i;
                LogEntry<T> existingEntry = nodeState.getLog().getEntry(logIndex);
                if (existingEntry != null
                        && existingEntry.getTerm().getTerm() != request.entries().get(i).getTerm().getTerm()) {
                    nodeState.getLog().truncateFrom(logIndex);
                    // Persist the truncation to stable storage.
                    storageService.rewriteLog(nodeState.getLog().getEntriesFrom(1));
                    break;
                }
            }

            // Rule 4: Append any new entries not already in the log.
            List<LogEntry<T>> entriesToAppend = new java.util.ArrayList<>();
            for (int i = 0; i < request.entries().size(); i++) {
                int logIndex = request.prevLogIndex() + 1 + i;
                if (nodeState.getLog().getEntry(logIndex) == null) {
                    LogEntry<T> newEntry = request.entries().get(i);
                    entriesToAppend.add(newEntry);
                }
            }
            if (!entriesToAppend.isEmpty()) {
                nodeState.getLog().appendAll(entriesToAppend);
                storageService.appendLogEntries(entriesToAppend);
                if (raftNode.isVerboseLoggingEnabled()) {
                    System.out.printf(" -> Appended %d new entries.\n", entriesToAppend.size());
                }
            }

            // Rule 5: If leaderCommit > commitIndex, set commitIndex = min(leaderCommit,
            // index of last new entry).
            if (request.leaderCommit() > nodeState.getCommitIndex().get()) {
                long newCommitIndex = Math.min(request.leaderCommit(), nodeState.getLog().getLastLogIndex());
                nodeState.setCommitIndex(new AtomicLong(newCommitIndex));
                // Notify the applier thread that new entries are committed and can be applied.
                nodeState.notifyAll();
            }
            return new AppendEntriesRPC.Reply(nodeState.getCurrentTerm(), true);
        }
    }
}