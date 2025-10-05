package system_design.consensus.raft;

import java.util.concurrent.CompletableFuture;

import system_design.consensus.raft.storage.model.NodeId;
import system_design.consensus.raft.storage.model.RaftRole;

/**
 * Defines the public-facing interface for a Raft node.
 * This contract is used by clients and tests to interact with the Raft cluster.
 *
 * @param <T> The type of command the state machine understands.
 */
public interface IRaftNode<T> {
    /**
     * Starts the node's server and the Raft protocol logic.
     */
    CompletableFuture<Void> start();

    /**
     * Stops the node and cleans up its resources.
     */
    void stop();

    /**
     * Submits a command to the Raft cluster.
     * This should only succeed if called on the current leader.
     */
    CompletableFuture<Boolean> submitCommand(T command);

    NodeId getSelfId();

    RaftRole getRole();

    int getPort();

    String getFromStateMachine(String key);
}