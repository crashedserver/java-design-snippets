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

package system_design.consensus.raft.storage.model;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Holds the volatile and persistent state for a Raft node. This class should be
 * thread-safe,
 * with access to its fields managed via synchronized methods.
 */
public class NodeState<T> {

    private RaftRole currentState;

    // --- Persistent state (must be saved to stable storage) ---
    private Term currentTerm;
    private NodeId votedFor;
    private CommandLog<T> log;

    // --- Im-memory state (re-initialized after a restart) ---
    private AtomicLong commitIndex;
    private AtomicLong lastAppliedIndex;

    // The state machine that commands are applied to.
    private final Map<String, String> stateMachine = new ConcurrentHashMap<>();

    /**
     * Initializes the state for a new Raft node upon startup.
     */
    public NodeState() {
        this.currentState = RaftRole.FOLLOWER;
        this.currentTerm = new Term(0);
        this.votedFor = null; // No one voted for in term 0
        this.log = new CommandLog<>();
        this.commitIndex = new AtomicLong(0);
        this.lastAppliedIndex = new AtomicLong(0);
    }

    /**
     * Initializes the state for a Raft node recovering from a restart, using loaded
     * persistent state.
     * 
     * @param currentTerm The last known term loaded from storage.
     * @param votedFor    The last candidate voted for, loaded from storage.
     * @param log         The command log loaded from storage.
     */
    public NodeState(Term currentTerm, NodeId votedFor, CommandLog<T> log) {
        this.currentState = RaftRole.FOLLOWER; // Always start as a follower
        this.currentTerm = currentTerm;
        this.votedFor = votedFor;
        this.log = log;
        this.commitIndex = new AtomicLong(0);
        // lastApplied is always initialized to 0 on startup. The state machine will be
        // rebuilt by the applier thread, which will apply log entries from index 1
        // up to the commitIndex communicated by the leader. This ensures the state
        // machine is correctly restored after a restart.
        this.lastAppliedIndex = new AtomicLong(0);
    }

    // --- Getters and Setters ---
    // It's crucial that any method that modifies state is synchronized or otherwise
    // protected
    // from concurrent access.

    public RaftRole getCurrentRole() {
        return currentState;
    }

    public void setCurrentRole(RaftRole newRole) {
        this.currentState = newRole;
    }

    public Term getCurrentTerm() {
        return currentTerm;
    }

    public void setCurrentTerm(Term currentTerm) {
        this.currentTerm = currentTerm;
    }

    public NodeId getVotedFor() {
        return votedFor;
    }

    public void setVotedFor(NodeId votedFor) {
        this.votedFor = votedFor;
    }

    public CommandLog<T> getLog() {
        return log;
    }

    public AtomicLong getCommitIndex() {
        return commitIndex;
    }

    public void setCommitIndex(AtomicLong commitIndex) {
        this.commitIndex = commitIndex;
    }

    public AtomicLong getLastAppliedIndex() {
        return lastAppliedIndex;
    }

    public void setLastAppliedIndex(AtomicLong lastAppliedIndex) {
        this.lastAppliedIndex = lastAppliedIndex;
    }

    /**
     * A helper method to increment the current term.
     * This is a common operation when starting an election.
     */
    public void incrementTerm() {
        this.currentTerm = new Term(this.currentTerm.getTerm() + 1);
    }

    public Map<String, String> getStateMachine() {
        return stateMachine;
    }
}
