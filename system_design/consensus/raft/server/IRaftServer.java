package system_design.consensus.raft.server;

import system_design.consensus.raft.rpc.AppendEntriesRPC;
import system_design.consensus.raft.rpc.RequestVoteRPC;

/**
 * Defines the non-generic interface for a Raft node that the server can
 * interact with,
 * avoiding issues with generic type erasure.
 */
public interface IRaftServer {
    RequestVoteRPC.Reply handleRequestVote(RequestVoteRPC.Request request);

    AppendEntriesRPC.Reply handleAppendEntries(AppendEntriesRPC.Request<Object> request);
}