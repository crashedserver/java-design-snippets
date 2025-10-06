/*
 * Copyright 2025 crashedserver
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

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.locks.ReentrantLock;

public class GenericLRUCache<K, V> {

    private final int capacity;
    private final ListNode<K, V> dummyHead;
    private final ListNode<K, V> dummyTail;
    private final Map<K, ListNode<K, V>> nodeMap;
    private final ReentrantLock lock;

    public GenericLRUCache(int capacity) {
        if (capacity <= 0) {
            throw new IllegalArgumentException("Capacity must be positive");
        }
        this.capacity = capacity;
        this.nodeMap = new HashMap<>(capacity);
        this.dummyHead = new ListNode<>(null, null);
        this.dummyTail = new ListNode<>(null, null);
        // Link dummy nodes to represent an empty list
        this.dummyHead.next = this.dummyTail;
        this.dummyTail.prev = this.dummyHead;
        this.lock = new ReentrantLock();
    }

    public void put(K key, V value) {
        lock.lock();
        try {
            // If the key already exists, update its value and move it to the front.
            if (nodeMap.containsKey(key)) {
                ListNode<K, V> existingNode = nodeMap.get(key);
                existingNode.value = value;
                moveToFront(existingNode);
                return;
            }

            // If the cache is full, evict the least recently used item.
            if (nodeMap.size() >= capacity) {
                removeLast();
            }

            // Add the new item to the front of the list and to the map.
            ListNode<K, V> newNode = new ListNode<>(key, value);
            addFirst(newNode);
            nodeMap.put(key, newNode);
        } finally {
            lock.unlock();
        }
    }

    public V get(K key) {
        lock.lock();
        try {
            if (!nodeMap.containsKey(key)) {
                return null;
            }

            // Get the node, move it to the front, and return its value.
            ListNode<K, V> node = nodeMap.get(key);
            moveToFront(node);
            return node.value;
        } finally {
            lock.unlock();
        }
    }

    // --- Doubly Linked List Helper Methods (all O(1) operations) ---

    private void moveToFront(ListNode<K, V> node) {
        removeNode(node);
        addFirst(node);
    }

    private void addFirst(ListNode<K, V> node) {
        ListNode<K, V> oldFirst = dummyHead.next;
        dummyHead.next = node;
        node.prev = dummyHead;
        node.next = oldFirst;
        oldFirst.prev = node;
    }

    private void removeNode(ListNode<K, V> node) {
        ListNode<K, V> prevNode = node.prev;
        ListNode<K, V> nextNode = node.next;
        prevNode.next = nextNode;
        nextNode.prev = prevNode;
    }

    private void removeLast() {
        // The last actual node is the one before the dummy tail.
        ListNode<K, V> lastNode = dummyTail.prev;
        if (lastNode == dummyHead) {
            return; // List is empty
        }
        removeNode(lastNode);
        nodeMap.remove(lastNode.key);
    }

    // --- Inner Class for the Doubly Linked List Node ---

    private static class ListNode<K, V> {
        ListNode<K, V> prev;
        K key;
        V value;
        ListNode<K, V> next;

        public ListNode(K key, V value) {
            this.key = key;
            this.value = value;
        }
    }
}
