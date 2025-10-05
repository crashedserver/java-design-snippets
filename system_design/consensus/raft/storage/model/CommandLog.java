/*
 * Copyright 2024 crashedserver
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

import java.util.ArrayList;
import java.util.List;

public class CommandLog<T> implements ICommandLog<T> {
    private final List<LogEntry<T>> commandLog;

    public CommandLog() {
        this.commandLog = new ArrayList<>();
    }

    @Override
    public int append(LogEntry<T> commandEntry) {
        this.commandLog.add(commandEntry);
        return commandLog.size();
    }

    @Override
    public void appendAll(List<LogEntry<T>> commandEntries) {
        this.commandLog.addAll(commandEntries);
    }

    /**
     * Retrieves a command entry at a specific index.
     *
     * @param index The 1-based index of the entry.
     * @return The CommandEntry at the specified index, or null if the index is out
     *         of bounds.
     */
    @Override
    public LogEntry<T> getEntry(int index) {
        // Convert 1-based index to 0-based for ArrayList access.
        int internalIndex = index - 1;
        if (internalIndex < 0 || internalIndex >= commandLog.size()) {
            return null;
        }
        return commandLog.get(internalIndex);
    }

    /**
     * @return The index of the last entry in the log. Returns 0 if the log is
     *         empty.
     */
    @Override
    public int getLastLogIndex() {
        return commandLog.size();
    }

    /**
     * @return The total number of entries in the log.
     */
    @Override
    public int size() {
        return commandLog.size();
    }

    public Term getLastLogTerm() {
        if (commandLog.isEmpty()) {
            return new Term(0);
        }
        return commandLog.get(commandLog.size() - 1).getTerm();
    }

    /**
     * Deletes all log entries from the given index (inclusive) to the end of the
     * log.
     * 
     * @param index The 1-based index from where to truncate.
     */
    public void truncateFrom(int index) {
        if (index <= 0 || index > commandLog.size()) {
            return;
        }
        commandLog.subList(index - 1, commandLog.size()).clear();
    }

    /**
     * Returns a list of log entries starting from a given index.
     * 
     * @param fromIndex The 1-based starting index.
     * @return A new list containing entries from the index to the end.
     */
    public List<LogEntry<T>> getEntriesFrom(int fromIndex) {
        if (fromIndex <= 0 || fromIndex > commandLog.size()) {
            return new ArrayList<>();
        }
        return new ArrayList<>(commandLog.subList(fromIndex - 1, commandLog.size()));
    }
}

interface ICommandLog<T> {
    int append(LogEntry<T> commandEntry);

    void appendAll(List<LogEntry<T>> commandEntries);

    LogEntry<T> getEntry(int index);

    int getLastLogIndex();

    int size();
}
