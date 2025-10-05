package system_design.consensus.raft.rpc;

import java.util.List;

import system_design.consensus.raft.storage.model.LogEntry;
import system_design.consensus.raft.storage.model.Term;


public interface AppendEntriesRPC<T> {
        public record Request<T>( //
                        Term term,
                        String leaderId,
                        int prevLogIndex,
                        Term prevLogTerm,
                        List<LogEntry<T>> entries,
                        int leaderCommit) {
        }

        public record Reply(
                        Term term,
                        boolean success) {
        }
}