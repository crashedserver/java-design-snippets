package system_design.consensus.raft;

import system_design.consensus.raft.rpc.AppendEntriesRPC;
import system_design.consensus.raft.rpc.RequestVoteRPC;

/**
 * Defines the interface for the core Raft consensus logic.
 * This allows for decoupling the RaftNode from the specific implementation of
 * the
 * algorithm,
 * enhancing testability and modularity.
 */
public interface IRaftLogic<T> {
    void startElection();

    RequestVoteRPC.Reply handleRequestVote(RequestVoteRPC.Request request);

    AppendEntriesRPC.Reply handleAppendEntries(AppendEntriesRPC.Request<T> request);

    void replicateLogToPeers();
}