package system_design.consensus.raft;

/**
 * Defines the interface for a state machine to which commands from the Raft log
 * are applied.
 * 
 * @param <T> The type of the command.
 */
public interface StateMachine<T> {
    /**
     * Applies a command to the state machine.
     * 
     * @param command The command to apply.
     */
    void apply(T command);

    /**
     * Retrieves a value from the state machine for a given key.
     * This is specific to a key-value state machine and might not exist in all
     * implementations.
     * 
     * @param key The key to look up.
     * @return The value associated with the key, or null if not found.
     */
    String get(String key);
}