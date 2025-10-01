package data_structures.cache;

public class LRUCacheTest {
    public static void main(String[] args) {
        System.out.println("--- Testing Optimized O(1) LRUCache ---");
        // Initialize a cache with a small capacity of 3
        LRUCache cache = new LRUCache(3);

        System.out.println("1. Putting key1, key2, key3...");
        cache.put("key1", "value1");
        cache.put("key2", "value2");
        cache.put("key3", "value3");

        System.out.println("   Getting key2: " + cache.get("key2")); // Expected: value2

        System.out.println("\n2. Cache is full. Adding key4 should evict the LRU item (key1).");
        cache.put("key4", "value4");

        System.out.println("   Getting key1 (should be null): " + cache.get("key1"));
        System.out.println("   Getting key3: " + cache.get("key3")); // Expected: value3

        System.out.println("\n3. Accessing key2 again to make it the most recently used.");
        cache.get("key2"); // Order is now (LRU -> MRU): key4, key3, key2

        System.out.println("\n4. Adding key5. This should evict key4 (which is now the LRU).");
        cache.put("key5", "value5");

        System.out.println("   Getting key4 (should be null): " + cache.get("key4"));
        System.out.println("   Getting key3: " + cache.get("key3")); // Expected: value3
        System.out.println("   Getting key2: " + cache.get("key2")); // Expected: value2

        System.out.println("\n5. Updating value for an existing key (key5).");
        cache.put("key5", "value5_updated");
        System.out.println("   Getting key5 (should be updated): " + cache.get("key5"));

        System.out.println("\n--- Test Complete ---");
    }
}
