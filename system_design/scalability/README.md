# Scalability Components

This directory contains practical Java implementations of common components and patterns used in building scalable distributed systems. Each component is designed to be a clear, concise example for learning and interview preparation.

## Components

### 1. Simple Consistent Hashing

Consistent Hashing is a distributed hashing scheme that minimizes key remapping when servers are added or removed. This is crucial for scalability in systems like distributed caches and databases.

*   **Implementation:** `SimpleConsistenHashing.java`
*   **Description:** A basic, easy-to-understand implementation of a consistent hashing ring using a `TreeMap`. It demonstrates how keys are mapped to nodes and how the system rebalances with minimal disruption when nodes change.
*   **Note for Learners:** This simple version does not use **virtual nodes** (replicas), which are recommended for more even key distribution in production systems.

#### How to Run the Test

From the root `java-design-snippets` directory, you can compile and run the test to see how keys are distributed and re-mapped when nodes are added or removed:

```bash
javac system_design/scalability/SimpleConsistenHashing.java system_design/scalability/SimpleConsistentHahingTest.java
java system_design.scalability.SimpleConsistentHahingTest
```

### 2. Consistent Hashing with Virtual Nodes

Consistent Hashing is a distributed hashing scheme that minimizes key remapping when servers are added or removed. This is crucial for scalability in systems like distributed caches and databases. This is an enhanced implementation of consistent hashing that incorporates virtual nodes (also known as replicas). Virtual nodes are crucial for achieving a more uniform distribution of keys across physical nodes and for smoother rebalancing when nodes are added or removed.

*   **Implementation:** `ConsistenHashingVNode.java`
*   **Description:** A basic, easy-to-understand implementation of a consistent hashing ring using a TreeMap. It demonstrates how keys are mapped to nodes and how the system rebalances with minimal disruption when nodes change.

Instead of placing each physical node on the ring once, it places it multiple times (e.g., "server-A#0", "server-A#1", ..., "server-A#N").
Each virtual node has its own hash but maps back to the same physical node.
This effectively "fills" the ring more evenly, reducing the likelihood of hot spots. +* Benefits:
Better Key Distribution: Keys are distributed more evenly across physical nodes.
Smoother Rebalancing: When a physical node is added or removed, its impact is spread across many points on the ring, leading to a smaller, more distributed remapping of keys.
*   **Note for Learners:** This simple version does not use virtual nodes (replicas), which are recommended for more even key distribution in production systems.

#### How to Run the Test

From the root `java-design-snippets` directory, you can compile and run the test to observe the improved key distribution with virtual nodes:

bash


```bash
javac system_design/scalability/ConsistenHashingVNode.java system_design/scalability/ConsistenHashingVNodeTest.java
java system_design.scalability.ConsistenHashingVNodeTest
```
