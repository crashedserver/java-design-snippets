/*
 * Copyright 2025 crashedserver
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

package system_design.consensus.raft;

import system_design.consensus.raft.RaftNode.Peer;

/**
 * A class to manage the state associated with a peer node, such as for logging
 * purposes.
 */
public class PeerState {
    private final Peer peer;
    private long lastErrorLogTimestamp = 0;
    // For each server, index of the next log entry to send to that server
    // (initialized to leader last log index + 1)
    private int nextIndex;
    // For each server, index of highest log entry known to be replicated on server
    // (initialized to 0, increases monotonically)
    private int matchIndex;
    private static final long ERROR_LOG_COOLDOWN_MS = 5000; // Log errors at most once every 5 seconds.

    public PeerState(Peer peer) {
        this.peer = peer;
        this.nextIndex = 1; // Start by assuming the follower needs the first entry.
    }

    public int getNextIndex() {
        return nextIndex;
    }

    public void setNextIndex(int nextIndex) {
        this.nextIndex = nextIndex;
    }

    public int getMatchIndex() {
        return matchIndex;
    }

    public void setMatchIndex(int matchIndex) {
        this.matchIndex = matchIndex;
    }

    public Peer getPeer() {
        return peer;
    }

    /**
     * Checks if an error for this peer should be logged, based on a cooldown
     * period.
     * If it should be logged, it updates the timestamp and returns true.
     */
    public boolean shouldLogError() {
        long now = System.currentTimeMillis();
        if (now - lastErrorLogTimestamp > ERROR_LOG_COOLDOWN_MS) {
            lastErrorLogTimestamp = now;
            return true;
        }
        return false;
    }
}