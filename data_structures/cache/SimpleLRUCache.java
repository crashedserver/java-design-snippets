package data_structures.cache;

import java.util.Deque;
import java.util.LinkedList;
import java.util.Map;
import java.util.HashMap;
import java.util.concurrent.locks.ReentrantLock;

public class SimpleLRUCache {
    
    private final int capacity;
    private final Map<String, String> cache;
    private final Deque<String> lruQueue;
    private final ReentrantLock lock;

    public SimpleLRUCache(int capacity) {
        if (capacity <= 0) {
            throw new IllegalArgumentException("Capacity must be positive");
        }
        this.capacity = capacity;
        this.cache = new HashMap<>();
        this.lruQueue = new LinkedList<>();
        this.lock = new ReentrantLock();
    }

    public void put(String key, String value) {
        lock.lock();
        try {
            // If the key already exists, it's an update. We must move it to the front.
            if (cache.containsKey(key)) {
                // Note: lruQueue.remove() is an O(n) operation for a LinkedList, as it may have
                // to scan the list.
                // For a high-performance cache, a custom Doubly Linked List is used to achieve
                // O(1) removal. I will provide an optimized version.
                lruQueue.remove(key);
            } else if (cache.size() >= capacity) {
                // If the cache is full, make space by evicting the least recently used item.
                if (!lruQueue.isEmpty()) {
                    String keyToEvict = lruQueue.removeLast();
                    cache.remove(keyToEvict);
                }
            }
            cache.put(key, value);
            // Add the new or updated item to the front of the queue to mark it as most
            // recently used.
            lruQueue.addFirst(key);
        } finally {
            lock.unlock();
        }
    }

    public String get(String key) {
        lock.lock();
        try {
            if (!cache.containsKey(key)) {
                return null;
            }
            // Move the accessed item to the front of the queue to mark it as most recently
            // used.
            // Note: This is also an O(n) operation. See the comment in the put() method for
            // optimization details.
            lruQueue.remove(key);
            lruQueue.offerFirst(key);

            return cache.get(key);
        } finally {
            lock.unlock();
        }

    }

}
