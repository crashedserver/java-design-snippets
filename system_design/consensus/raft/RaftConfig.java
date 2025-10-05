package system_design.consensus.raft;

import java.util.List;
import system_design.consensus.raft.storage.model.NodeId;

/**
 * A configuration class for creating a RaftNode.
 * This encapsulates all the necessary parameters for a node's setup.
 */
public record RaftConfig<T>(
        NodeId selfId,
        int minPort,
        int maxPort,
        List<PeerState> initialPeers,
        StateMachine<T> stateMachine) {
    public RaftConfig(NodeId selfId, int port, List<PeerState> initialPeers, StateMachine<T> stateMachine) {
        this(selfId, port, port, initialPeers, stateMachine);
    }
}