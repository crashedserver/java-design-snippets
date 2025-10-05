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

package data_structures.cache;

public class GenericLRUCacheTest {
    public static void main(String[] args) {
        System.out.println("--- Testing GenericLRUCache<Integer, String> ---");
        // Initialize a cache with a small capacity of 3
        GenericLRUCache<Integer, String> cache = new GenericLRUCache<>(3);

        System.out.println("1. Putting keys 1, 2, 3...");
        cache.put(1, "value1");
        cache.put(2, "value2");
        cache.put(3, "value3");

        System.out.println("   Getting key 2: " + cache.get(2)); // Expected: value2

        System.out.println("\n2. Cache is full. Adding key 4 should evict the LRU item (key 1).");
        cache.put(4, "value4");

        System.out.println("   Getting key 1 (should be null): " + cache.get(1));
        System.out.println("   Getting key 3: " + cache.get(3)); // Expected: value3

        System.out.println("\n3. Accessing key 2 again to make it the most recently used.");
        cache.get(2); // Order is now (LRU -> MRU): 4, 3, 2

        System.out.println("\n4. Adding key 5. This should evict key 4 (which is now the LRU).");
        cache.put(5, "value5");

        System.out.println("   Getting key 4 (should be null): " + cache.get(4));
        System.out.println("   Getting key 3: " + cache.get(3)); // Expected: value3
        System.out.println("   Getting key 2: " + cache.get(2)); // Expected: value2

        System.out.println("\n5. Updating value for an existing key (key 5).");
        cache.put(5, "value5_updated");
        System.out.println("   Getting key 5 (should be updated): " + cache.get(5));

        System.out.println("\n--- Test Complete ---");
    }
}
