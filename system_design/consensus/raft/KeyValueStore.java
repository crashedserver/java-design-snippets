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