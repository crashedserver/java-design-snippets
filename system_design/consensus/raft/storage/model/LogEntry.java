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

package system_design.consensus.raft.storage.model;

public class LogEntry<T> {
    private final Term term;
    private final T command;

    public LogEntry(Term term, T command) {
        this.term = term;
        this.command = command;
    }

    public Term getTerm() {
        return term;
    }

    public T getCommand() {
        return command;
    }
}
