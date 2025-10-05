package system_design.consensus.raft.storage.model;

public class Term {
    private final long term;

    public Term(long term) {
        this.term = term;
    }

    public long getTerm() {
        return term;
    }
}
