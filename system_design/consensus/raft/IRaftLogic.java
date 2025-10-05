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

import system_design.consensus.raft.rpc.AppendEntriesRPC;
import system_design.consensus.raft.rpc.RequestVoteRPC;
import system_design.consensus.raft.storage.model.Term;

/**
 * Defines the interface for the core Raft consensus logic.
 * This allows for decoupling the RaftNode from the specific implementation of
 * the
 * algorithm,
 * enhancing testability and modularity.
 */
public interface IRaftLogic<T> {
    void startElection();

    RequestVoteRPC.Reply handleRequestVote(RequestVoteRPC.Request request);

    AppendEntriesRPC.Reply handleAppendEntries(AppendEntriesRPC.Request<T> request);

    boolean replicateLogToPeers(boolean waitForMajority);

    void replicateLogToPeers(); // Fire-and-forget version

    void stepDown(Term newTerm);
}