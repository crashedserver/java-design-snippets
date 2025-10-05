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

package system_design.consensus.raft.storage.file_storage;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.List;

import system_design.consensus.raft.storage.IStorageService;
import system_design.consensus.raft.storage.model.CommandLog;
import system_design.consensus.raft.storage.model.LogEntry;
import system_design.consensus.raft.storage.model.NodeId;
import system_design.consensus.raft.storage.model.Term;
import system_design.consensus.raft.util.SerializationUtil;

public class FileStorageService<T> implements IStorageService<T> {

    private static final String TERM_FILE_NAME = "raft_term.dat";
    private static final String VOTED_FOR_FILE_NAME = "raft_voted_for.dat";
    private static final String LOG_FILE_NAME = "raft_log.dat";

    private final File termFile;
    private final File votedForFile;
    private final File commandLogFile;

    public FileStorageService(String storageDir) {
        File dir = new File(storageDir);
        if (!dir.exists()) {
            dir.mkdirs();
        }
        this.termFile = new File(dir, TERM_FILE_NAME);
        this.votedForFile = new File(dir, VOTED_FOR_FILE_NAME);
        this.commandLogFile = new File(dir, LOG_FILE_NAME);
    }

    @Override
    public void saveTerm(Term term) {
        try (FileOutputStream fos = new FileOutputStream(termFile);
                PrintWriter writer = new PrintWriter(fos)) {
            writer.println(term.getTerm());
            writer.flush();
            fos.getFD().sync();
        } catch (IOException e) {
            throw new RuntimeException("Could not save to the term file.", e);
        }
    }

    @Override
    public Term loadTerm() {
        if (!termFile.exists()) {
            return new Term(0);
        }
        try (BufferedReader reader = new BufferedReader(new FileReader(termFile))) {
            String line = reader.readLine();
            return new Term(Long.parseLong(line));
        } catch (IOException | NumberFormatException e) {
            throw new RuntimeException("Could not load term from file.", e);
        }
    }

    @Override
    public void saveVotedFor(NodeId candidateId) {
        try (FileOutputStream fos = new FileOutputStream(votedForFile);
                PrintWriter writer = new PrintWriter(fos)) {
            if (candidateId != null) {
                writer.println(candidateId.getNodeId());
            } else {
                writer.print(""); // Write an empty string to clear the file
            }
            writer.flush();
            fos.getFD().sync();
        } catch (IOException e) {
            throw new RuntimeException("Could not save to the voted for file.", e);
        }
    }

    @Override

    public NodeId loadVotedFor() {
        if (!votedForFile.exists()) {
            return null;
        }
        try (BufferedReader reader = new BufferedReader(new FileReader(votedForFile))) {
            String line = reader.readLine();
            return line != null && !line.isEmpty() ? new NodeId(line) : null;
        } catch (IOException | NumberFormatException e) {
            throw new RuntimeException("Could not load votedFor from file.", e);
        }
    }

    @Override
    public void appendLogEntry(LogEntry<T> logEntry) {
        try (FileOutputStream fos = new FileOutputStream(commandLogFile, true);
                PrintWriter writer = new PrintWriter(fos)) {
            String jsonEntry = SerializationUtil.serialize(logEntry);
            writer.println(jsonEntry);
            writer.flush();
            fos.getFD().sync();
        } catch (IOException e) {
            throw new RuntimeException("Could not append to the log file.", e);
        }
    }

    @Override
    public void appendLogEntries(List<LogEntry<T>> logEntries) {
        if (logEntries == null || logEntries.isEmpty()) {
            return;
        }
        try (FileOutputStream fos = new FileOutputStream(commandLogFile, true);
                PrintWriter writer = new PrintWriter(fos)) {
            for (LogEntry<T> logEntry : logEntries) {
                String jsonEntry = SerializationUtil.serialize(logEntry);
                writer.println(jsonEntry);
            }
            writer.flush();
            fos.getFD().sync(); // Perform a single sync for the entire batch.
        } catch (IOException e) {
            throw new RuntimeException("Could not append batch to the log file.", e);
        }
    }

    @Override
    public void rewriteLog(List<LogEntry<T>> entries) {
        File tempFile = null;
        try {
            // 1. Create a temporary file in the same directory.
            tempFile = File.createTempFile("raft_log_rewrite", ".tmp", commandLogFile.getParentFile());

            // 2. Write the new, truncated log content to the temporary file.
            try (FileOutputStream fos = new FileOutputStream(tempFile);
                    PrintWriter writer = new PrintWriter(fos)) {
                for (LogEntry<T> logEntry : entries) {
                    String jsonEntry = SerializationUtil.serialize(logEntry);
                    writer.println(jsonEntry);
                }
                writer.flush();
                fos.getFD().sync(); // Ensure data is physically written to disk.
            }

            // 3. Atomically replace the old log file with the new one.
            Files.move(tempFile.toPath(), commandLogFile.toPath(), StandardCopyOption.ATOMIC_MOVE,
                    StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            throw new RuntimeException("Could not rewrite the log file.", e);
        }
    }

    @Override

    public CommandLog<T> loadLog() {
        CommandLog<T> log = new CommandLog<>();
        if (!commandLogFile.exists()) {
            return log;
        }
        try (BufferedReader reader = new BufferedReader(new FileReader(commandLogFile))) {
            String line;
            while ((line = reader.readLine()) != null) {
                LogEntry<T> entry = SerializationUtil.deserializeLogEntry(line);
                log.append(entry);
            }
        } catch (IOException e) {
            throw new RuntimeException("Could not load log from file.", e);
        }
        return log;
    }
}
