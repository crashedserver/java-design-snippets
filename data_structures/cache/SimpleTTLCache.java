package data_structures.cache;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * A simple cache implementation where entries expire after a fixed time-to-live (TTL).
 * This cache uses a background thread to periodically scan and evict expired entries.
 */
public class SimpleTTLCache {
    // Use a ConcurrentHashMap for thread-safe get/put operations.
    private final Map<String, CacheEntry> cache = new ConcurrentHashMap<>();
    private final int capacity;
    private final long ttlDurationMillis;
    private final ScheduledExecutorService executorService;

    public SimpleTTLCache(int capacity, long ttlInMillis) {
        this.capacity = capacity;
        this.ttlDurationMillis = ttlInMillis;

        // Create a single-threaded scheduler to run the eviction task.
        // Using a daemon thread ensures it doesn't prevent the JVM from shutting down.
        this.executorService = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "SimpleTTLCache-EvictionThread");
            t.setDaemon(true);
            return t;
        });

        // Schedule the eviction task to run periodically.
        long scheduleIntervalMillis = 3_000;
        long initialDelayForSchedulerMillis = 1_000;
        executorService.scheduleAtFixedRate(new TTLTask(), initialDelayForSchedulerMillis, scheduleIntervalMillis, TimeUnit.MILLISECONDS);
    }

    public void put(String key, String value) {
        // A simple capacity check. In a real-world scenario, you might evict
        // the oldest or least-used item instead of throwing an exception.
        if (cache.size() >= capacity && !cache.containsKey(key)) {
            throw new RuntimeException("Cache is full. Try after sometime!");
        }
        cache.put(key, new CacheEntry(value));
    }

    public String get(String key) {
        CacheEntry entry = cache.get(key);
        if (entry == null) {
            return null;
        }

        // Perform a read-time check for expiration.
        if (Instant.now().toEpochMilli() - entry.creationTimestampMillis >= ttlDurationMillis) {
            // Entry has expired, remove it and return null.
            cache.remove(key);
            return null;
        }
        return entry.value;
    }

    /**
     * Shuts down the background eviction thread. This should be called when the cache
     * is no longer needed to prevent resource leaks.
     */
    public void shutdown() {
        executorService.shutdown();
    }

    /**
     * The background task that periodically scans the cache for expired entries.
     */
    private class TTLTask implements Runnable {
        @Override
        public void run() {
            long now = Instant.now().toEpochMilli();
            List<String> keysToRemove = new ArrayList<>();

            // First, collect all the keys of expired entries.
            for (Map.Entry<String, CacheEntry> entry : cache.entrySet()) {
                if (now - entry.getValue().creationTimestampMillis >= ttlDurationMillis) {
                    keysToRemove.add(entry.getKey());
                }
            }

            // Then, remove them to avoid modifying the map while iterating.
            for (String key : keysToRemove) {
                cache.remove(key);
            }
        }
    }

    /**
     * An inner class to hold the cached value and its creation timestamp.
     */
    private static class CacheEntry{
        String value;
        long creationTimestampMillis;
        public CacheEntry(String value){
            this.value=value;
            this.creationTimestampMillis = Instant.now().toEpochMilli();
        }
    }

}