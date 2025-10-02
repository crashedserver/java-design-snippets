package data_structures.cache;

public class SimpleTTLCacheTest {
    public static void main(String[] args) throws InterruptedException {
        // Configure a TTL cache with a capacity of 3 and a 5-second time-to-live.
        int capacity = 3;
        long ttlInMillis = 5000;
        SimpleTTLCache cache = new SimpleTTLCache(capacity, ttlInMillis);

        System.out.println("--- Simple TTL Cache Demonstration ---");
        System.out.println("Configuration: Capacity=" + capacity + ", TTL=" + ttlInMillis / 1000 + " seconds.");

        // --- Scenario 1: Put and Get items ---
        System.out.println("\n1. Putting items 'key1', 'key2' into the cache.");
        cache.put("key1", "value1");
        cache.put("key2", "value2");

        System.out.println("   Getting 'key1' immediately: " + cache.get("key1")); // Expected: value1

        // --- Scenario 2: Item Expiration ---
        System.out.println("\n2. Waiting for TTL to expire (" + ttlInMillis / 1000 + " seconds)...");
        // Wait for TTL + a little extra for the eviction task to run
        Thread.sleep(ttlInMillis + 1000);

        System.out.println("\nTTL has expired. 'key1' and 'key2' should be evicted by the background task.");
        System.out.println("   Getting 'key1' after TTL: " + cache.get("key1")); // Expected: null
        System.out.println("   Getting 'key2' after TTL: " + cache.get("key2")); // Expected: null

        // --- Scenario 3: Capacity Limit ---
        System.out.println("\n3. Testing capacity limit. Putting 3 new items.");
        cache.put("keyA", "valueA");
        cache.put("keyB", "valueB");
        cache.put("keyC", "valueC");
        System.out.println("   Successfully added 3 items.");

        try {
            System.out.println("   Trying to add a 4th item ('keyD')...");
            cache.put("keyD", "valueD");
        } catch (RuntimeException e) {
            System.out.println("   Caught expected exception: " + e.getMessage());
        }

        // --- Scenario 4: Shutdown ---
        System.out.println("\n4. Shutting down the cache to stop the background thread.");
        cache.shutdown();

        System.out.println("\n--- Test Complete ---");
    }
}