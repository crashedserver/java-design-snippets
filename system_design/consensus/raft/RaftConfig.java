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

import java.util.List;

import system_design.consensus.raft.storage.model.NodeId;

/**
 * A configuration class for creating a RaftNode.
 * This encapsulates all the necessary parameters for a node's setup.
 */
public record RaftConfig<T>(
        NodeId selfId,
        int minPort,
        int maxPort,
        List<PeerState> initialPeers,
        StateMachine<T> stateMachine,
        boolean verboseLogging,
        int minElectionTimeoutMillis,
        int maxElectionTimeoutMillis,
        int heartbeatIntervalMillis) {
    public RaftConfig(NodeId selfId, int port, List<PeerState> initialPeers, StateMachine<T> stateMachine) {
        // Default values tuned for higher-latency, cross-region networks.
        this(selfId, port, port, initialPeers, stateMachine, true, 1000, 2000, 150);
    }

    public RaftConfig(NodeId selfId, int port, List<PeerState> initialPeers, StateMachine<T> stateMachine,
            boolean verboseLogging) {
        this(selfId, port, port, initialPeers, stateMachine, verboseLogging, 1000, 2000, 150);
    }
}