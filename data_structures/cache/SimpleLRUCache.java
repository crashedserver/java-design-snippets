/*
 * Copyright 2025 crashedserver
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law of or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package data_structures.cache;

import java.util.Deque;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
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
