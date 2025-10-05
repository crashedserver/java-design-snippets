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

import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * A simple implementation of a key-value store state machine.
 */
public class KeyValueStore implements StateMachine<String> {
    private final Map<String, String> map = new ConcurrentHashMap<>();

    @Override
    public void apply(String command) {
        // Assumes command is in the format "set key=value"
        // or "del key"
        try {
            String[] commandParts = command.trim().split("\\s+", 2);
            String action = commandParts[0].toLowerCase(Locale.US);

            if ("set".equals(action) && commandParts.length > 1) {
                String[] parts = commandParts[1].split("=", 2);
                if (parts.length == 2) {
                    String key = parts[0].trim();
                    String value = parts[1].trim();
                    map.put(key, value);
                }
            } else if ("del".equals(action) && commandParts.length > 1) {
                String key = commandParts[1].trim();
                map.remove(key);
            }
        } catch (Exception e) {
            throw new RuntimeException("Invalid command - " + e.getMessage(), e);
        }
    }

    @Override
    public String get(String key) {
        return map.get(key);
    }
}