# Scalability Components

This directory contains practical Java implementations of common components and patterns used in building scalable distributed systems. Each component is designed to be a clear, concise example for learning and interview preparation.

## Components

### 1. Consistent Hashing

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
