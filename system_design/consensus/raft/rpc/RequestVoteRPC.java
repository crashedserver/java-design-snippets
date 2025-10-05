package system_design.consensus.raft.rpc;

import system_design.consensus.raft.storage.model.Term;

public interface RequestVoteRPC {

        public record Request(
                        Term term,
                        String candidateId,
                        int lastLogIndex,
                        Term lastLogTerm) {
        }

        public record Reply(
                        Term term,
                        boolean voteGranted) {
        }
}