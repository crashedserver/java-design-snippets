package system_design.scalability;

import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class ConsistenHashingVNodeTest {
    public static void main(String[] args) {
        // 1. Initialize the consistent hashing ring with virtual nodes
        // Using 100 replicas for better distribution demonstration
        int replicas = 100;
        ConsistenHashingVNode hashing = new ConsistenHashingVNode(replicas);

        // 2. Add initial server nodes
        List<String> nodes = Arrays.asList("server-A", "server-B", "server-C");
        nodes.forEach(hashing::addNode);
        System.out.println("--- Initial State (with " + replicas + " virtual nodes per physical node) ---");
        System.out.println("Physical Nodes on the ring: " + nodes);

        // 3. Define a larger set of keys to map to better observe distribution
        List<String> keys = generateKeys(50); // Generate 50 unique keys

        // 4. Print the initial distribution of keys
        System.out.println("\nInitial key distribution:");
        printKeyDistribution(hashing, keys);

        // 5. Simulate removing a node
        String nodeToRemove = "server-B";
        System.out.println("\n--- Removing node '" + nodeToRemove + "' ---");
        hashing.removeNode(nodeToRemove);

        System.out.println("\nKey distribution after removing '" + nodeToRemove + "':");
        printKeyDistribution(hashing, keys);

        // 6. Simulate adding a new node
        String nodeToAdd = "server-D";
        System.out.println("\n--- Adding node '" + nodeToAdd + "' ---");
        hashing.addNode(nodeToAdd);

        System.out.println("\nFinal key distribution after adding '" + nodeToAdd + "':");
        printKeyDistribution(hashing, keys);

        System.out.println("\n--- Test Complete ---");
    }

    /**
     * Helper method to generate a list of unique keys.
     */
    private static List<String> generateKeys(int count) {
        return java.util.stream.IntStream.range(0, count)
                .mapToObj(i -> "key_" + i)
                .collect(Collectors.toList());
    }

    /**
     * Helper method to print the distribution of keys across nodes.
     */
    private static void printKeyDistribution(IConsistentHashing hashing, List<String> keys) {
        // Group keys by the node they are mapped to
        Map<String, List<String>> distribution = new HashMap<>();
        for (String key : keys) {
            String mappedNode = hashing.getMappedNode(key);
            distribution.computeIfAbsent(mappedNode, k -> new LinkedList<>()).add(key);
        }

        // Sort nodes for consistent output
        distribution.entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .forEach(entry -> {
                    System.out.println("  Node '" + entry.getKey() + "' handles " + entry.getValue().size() + " keys: "
                            + entry.getValue());
                });
        System.out.println("  Total keys mapped: " + keys.size());
    }
}