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

package system_design.consensus.raft.server;

import system_design.consensus.raft.rpc.AppendEntriesRPC;
import system_design.consensus.raft.rpc.RequestVoteRPC;

/**
 * Defines the non-generic interface for a Raft node that the server can
 * interact with,
 * avoiding issues with generic type erasure.
 */
public interface IRaftServer {
    RequestVoteRPC.Reply handleRequestVote(RequestVoteRPC.Request request);

    AppendEntriesRPC.Reply handleAppendEntries(AppendEntriesRPC.Request<Object> request);
}