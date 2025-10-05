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

package system_design.consensus.raft.util;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import system_design.consensus.raft.rpc.AppendEntriesRPC;
import system_design.consensus.raft.rpc.RequestVoteRPC;
import system_design.consensus.raft.storage.model.LogEntry;
import system_design.consensus.raft.storage.model.Term;

/**
 * A unique, dependency-free utility for serializing and deserializing RPC
 * objects
 * using a simple key-value string format.
 */
public final class SerializationUtil {

    private SerializationUtil() {
    }

    // --- RequestVote ---

    public static String serialize(RequestVoteRPC.Request req) {
        return String.format("term=%d;candidateId=%s;lastLogIndex=%d;lastLogTerm=%d;",
                req.term().getTerm(),
                req.candidateId(),
                req.lastLogIndex(),
                req.lastLogTerm().getTerm());
    }

    public static RequestVoteRPC.Request deserializeRequestVote(String payload) {
        Map<String, String> map = parsePayload(payload);
        Term term = new Term(Long.parseLong(map.get("term")));
        String candidateId = map.get("candidateId");
        int lastLogIndex = Integer.parseInt(map.get("lastLogIndex"));
        Term lastLogTerm = new Term(Long.parseLong(map.get("lastLogTerm")));
        return new RequestVoteRPC.Request(term, candidateId, lastLogIndex, lastLogTerm);
    }

    // --- RequestVote Reply ---

    public static String serialize(RequestVoteRPC.Reply reply) {
        return String.format("term=%d;voteGranted=%b;",
                reply.term().getTerm(),
                reply.voteGranted());
    }

    public static RequestVoteRPC.Reply deserializeVoteReply(String payload) {
        Map<String, String> map = parsePayload(payload);
        Term term = new Term(Long.parseLong(map.get("term")));
        boolean voteGranted = Boolean.parseBoolean(map.get("voteGranted"));
        return new RequestVoteRPC.Reply(term, voteGranted);
    }

    public static String serialize(AppendEntriesRPC.Request<?> req) {
        StringBuilder sb = new StringBuilder();
        sb.append(String.format("term=%d;leaderId=%s;prevLogIndex=%d;prevLogTerm=%d;leaderCommit=%d;",
                req.term().getTerm(),
                req.leaderId(),
                req.prevLogIndex(),
                req.prevLogTerm().getTerm(),
                req.leaderCommit()));

        // Serialize entries: "entries=[term:command|term:command]"
        sb.append("entries=[");
        for (int i = 0; i < req.entries().size(); i++) {
            LogEntry<?> entry = req.entries().get(i);
            sb.append(entry.getTerm().getTerm()).append(":").append(entry.getCommand());
            if (i < req.entries().size() - 1) {
                sb.append("|");
            }
        }
        sb.append("];");
        return sb.toString();
    }

    @SuppressWarnings("unchecked")
    public static <T> AppendEntriesRPC.Request<T> deserializeAppendEntries(String payload) {
        Map<String, String> map = parsePayload(payload);
        Term term = new Term(Long.parseLong(map.get("term")));
        String leaderId = map.get("leaderId");
        int prevLogIndex = Integer.parseInt(map.get("prevLogIndex"));
        Term prevLogTerm = new Term(Long.parseLong(map.get("prevLogTerm")));
        int leaderCommit = Integer.parseInt(map.get("leaderCommit"));

        List<LogEntry<T>> entries = new ArrayList<>();
        String entriesStr = map.get("entries");
        if (entriesStr != null && entriesStr.length() > 2) { // Not "[]"
            String[] entryPairs = entriesStr.substring(1, entriesStr.length() - 1).split("\\|");
            for (String pair : entryPairs) {
                String[] parts = pair.split(":", 2);
                Term entryTerm = new Term(Long.parseLong(parts[0]));
                T command = (T) parts[1]; // Unsafe cast, works for String commands
                entries.add(new LogEntry<>(entryTerm, command));
            }
        }
        return new AppendEntriesRPC.Request<>(term, leaderId, prevLogIndex, prevLogTerm, entries, leaderCommit);
    }

    public static String serialize(AppendEntriesRPC.Reply reply) {
        return String.format("term=%d;success=%b;", reply.term().getTerm(), reply.success());
    }

    public static AppendEntriesRPC.Reply deserializeAppendEntriesReply(String payload) {
        Map<String, String> map = parsePayload(payload);
        Term term = new Term(Long.parseLong(map.get("term")));
        boolean success = Boolean.parseBoolean(map.get("success"));
        return new AppendEntriesRPC.Reply(term, success);
    }

    // --- LogEntry ---

    public static <T> String serialize(LogEntry<T> logEntry) {
        return String.format("term=%d;command=%s;", logEntry.getTerm().getTerm(), logEntry.getCommand());
    }

    @SuppressWarnings("unchecked")
    public static <T> LogEntry<T> deserializeLogEntry(String payload) {
        try {
            Map<String, String> map = parsePayload(payload);
            Term term = new Term(Long.parseLong(map.get("term")));
            // This cast is unsafe in general but works here because we assume the command
            // is a String.
            T command = (T) map.get("command");

            return new LogEntry<>(term, command);
        } catch (Exception e) {
            throw new RuntimeException("Failed to parse log entry: " + payload, e);
        }
    }

    /**
     * A simple parser that converts a "key=value;key2=value2;" string into a Map.
     */
    private static Map<String, String> parsePayload(String payload) {
        Map<String, String> map = new HashMap<>();
        String[] pairs = payload.split(";");
        for (String pair : pairs) {
            if (pair.isEmpty()) {
                continue;
            }
            String[] kv = pair.split("=", 2);
            if (kv.length == 2) {
                map.put(kv[0], kv[1]);
            }
        }
        return map;
    }
}