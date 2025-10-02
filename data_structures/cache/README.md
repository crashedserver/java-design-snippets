# LRU Cache Implementations

This directory contains the thread-safe implementations of a Least Recently Used (LRU) Cache, demonstrating a clear progression from a simple concept to a fully-featured, high-performance component.

An LRU Cache is a fixed-size cache that automatically evicts the least recently used item when it becomes full and a new item needs to be added.

All public methods in all the implementations are protected by a `ReentrantLock` to ensure thread safety.

---

## 1. `SimpleLRUCache.java` (O(n) operations)

This version is designed for clarity and is a great starting point for understanding the core logic of an LRU cache.

### How it Works

It uses two standard Java collections:

1.  A `HashMap`: For fast, O(1) average-time lookups of keys.
2.  A `Deque` (Double-Ended Queue), implemented as a `LinkedList`: To maintain the order of item usage. The most recently used items are at the front, and the least recently used items are at the back.

### Performance

The `get()` and `put()` (for updates) operations have a time complexity of **O(n)** because moving an existing item to the front requires scanning the `LinkedList`.

### How to Run the Test

From the root `java-design-snippets` directory, you can compile and run its test:

```bash
javac data_structures/cache/SimpleLRUCache.java data_structures/cache/SimpleLRUCacheTest.java
java data_structures.cache.SimpleLRUCacheTest
```

---

## 2. `LRUCache.java` (Optimized, O(1) operations)

This is a high-performance implementation that achieves constant time complexity for all operations.

### How it Works

It uses a `HashMap` combined with a **custom Doubly Linked List**.

1.  The `HashMap` stores a direct reference to each `ListNode`.
2.  The Doubly Linked List maintains the usage order.

This combination allows any node to be found and moved in **O(1)** time.

### How to Run the Test

From the root `java-design-snippets` directory, you can compile and run its test:

```bash
javac data_structures/cache/LRUCache.java data_structures/cache/LRUCacheTest.java
java data_structures.cache.LRUCacheTest
```

---

## 3. `GenericLRUCache.java` (Optimized & Generic, O(1) operations) 

This is the most advanced version, building on the optimized LRUCache by adding Java generics for full type safety and reusability. 

### How it Works

It uses the same high-performance HashMap + custom Doubly Linked List approach as LRUCache.java, but with type parameters <K, V>. This allows the cache to work with any key-value types (e.g., <Integer, String>, <UUID, UserObject>) while providing compile-time safety. 

### How to Run the Test

From the root `java-design-snippets` directory, you can compile and run its test: 

```bash 
javac data_structures/cache/GenericLRUCache.java data_structures/cache/GenericLRUCacheTest.java 
java data_structures.cache.GenericLRUCacheTest
```

---

## 4. `SimpleTTLCache.java` (Time-To-Live Eviction)

This is a cache implementation where each entry automatically expires and is evicted after a configured Time-To-Live (TTL) duration.

### How it Works

It uses a `ConcurrentHashMap` for thread-safe storage and a `ScheduledExecutorService` to run a background task periodically. This task scans the cache and removes any entries that have lived longer than their TTL.

### How to Run the Test

From the root `java-design-snippets` directory, you can compile and run its test to see items being evicted after their TTL expires:

```bash
javac data_structures/cache/SimpleTTLCache.java data_structures/cache/SimpleTTLCacheTest.java
java data_structures.cache.SimpleTTLCacheTest
```
```