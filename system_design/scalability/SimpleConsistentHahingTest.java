package system_design.scalability;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class SimpleConsistentHahingTest {
    public static void main(String[] args) {
        // 1. Initialize the consistent hashing ring
        SimpleConsistenHashing hashing = new SimpleConsistenHashing();

        // 2. Add initial server nodes
        List<String> nodes = Arrays.asList("server-A", "server-B", "server-C");
        nodes.forEach(hashing::addNode);
        System.out.println("--- Initial State ---");
        System.out.println("Nodes on the ring: " + nodes);

        // 3. Define some keys to map
        List<String> keys = Arrays.asList(
                "user1_session", "product_page_123", "image_asset_456",
                "video_stream_789", "api_key_abc", "cache_entry_def");

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
    }

    private static void printKeyDistribution(IConsistentHashing hashing, List<String> keys) {
        // Group keys by the node they are mapped to
        Map<String, List<String>> distribution = keys.stream()
                .collect(Collectors.groupingBy(hashing::getMappedNode));

        distribution.forEach((node, keyList) -> System.out.println("  Node '" + node + "' handles: " + keyList));
    }
}