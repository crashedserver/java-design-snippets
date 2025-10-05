package system_design.consensus.raft.storage.model;

public class LogEntry<T> {
    private final Term term;
    private final T command;

    public LogEntry(Term term,T command){
        this.term=term;
        this.command=command;
    }

    public Term getTerm() {
        return term;
    }

    public T getCommand() {
        return command;
    }
}
