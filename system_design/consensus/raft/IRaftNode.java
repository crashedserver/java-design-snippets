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

package system_design.consensus.raft;

import java.util.concurrent.CompletableFuture;

import system_design.consensus.raft.storage.model.NodeId;
import system_design.consensus.raft.storage.model.NodeState;
import system_design.consensus.raft.storage.model.RaftRole;

/**
 * Defines the public-facing interface for a Raft node.
 * This contract is used by clients and tests to interact with the Raft cluster.
 *
 * @param <T> The type of command the state machine understands.
 */
public interface IRaftNode<T> {
    /**
     * Starts the node's server and the Raft protocol logic.
     */
    CompletableFuture<Void> start();

    /**
     * Stops the node and cleans up its resources.
     */
    void stop();

    /**
     * Submits a command to the Raft cluster.
     * This should only succeed if called on the current leader.
     */
    CompletableFuture<Boolean> submitCommand(T command);

    NodeId getSelfId();

    RaftRole getRole();

    int getPort();

    String getFromStateMachine(String key);

    NodeState<T> getNodeState();
}