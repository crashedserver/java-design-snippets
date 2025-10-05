package system_design.consensus.raft.storage;

import java.util.List;
import system_design.consensus.raft.storage.model.CommandLog;
import system_design.consensus.raft.storage.model.LogEntry;
import system_design.consensus.raft.storage.model.NodeId;
import system_design.consensus.raft.storage.model.Term;

public interface IStorageService<T> {
    void saveTerm(Term term);

    Term loadTerm();

    void saveVotedFor(NodeId candidateId);

    NodeId loadVotedFor();

    void appendLogEntry(LogEntry<T> logEntry);

    void appendLogEntries(List<LogEntry<T>> logEntries);

    void rewriteLog(List<LogEntry<T>> entries);

    CommandLog<T> loadLog();
}