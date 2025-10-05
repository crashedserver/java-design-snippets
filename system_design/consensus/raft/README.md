# Raft Consensus Algorithm Implementation

This directory contains a complete, from-scratch implementation of the Raft consensus algorithm in Java. It is designed to be a clear, practical guide for understanding how Raft works, covering leader election, log replication, and fault tolerance.

This implementation is based on the principles described in the original Raft paper: [In Search of an Understandable Consensus Algorithm](https://raft.github.io/raft.pdf) by Diego Ongaro and John Ousterhout.

---

## 1. Core Concepts Explained

Raft works by breaking down the problem of consensus into three key subproblems: Leader Election, Log Replication, and Safety.

### Node States & Terms

Every node in a Raft cluster is in one of three states at any given time:

*   **Follower:** The default state. A follower is passive; it responds to RPCs from leaders and candidates. If it doesn't hear from a leader within a certain time, it starts an election.
*   **Candidate:** A node that is actively trying to become the new leader.
*   **Leader:** The active node that handles all client requests and manages log replication to the followers. There can be at most one leader in a given term.

Raft divides time into **Terms**, which act as a logical clock. Each term begins with an election. If a leader is elected, it rules for that term. If not, the term ends, and a new election begins for the next term.

---

## 2. The Algorithm in Action

### Part 1: Leader Election

1.  **Timeout:** A Follower starts a randomized **election timer**. If this timer expires without hearing from a leader, it assumes the leader has failed.
2.  **Candidacy:** The Follower becomes a **Candidate**. It increments the `currentTerm`, votes for itself, and sends `RequestVote` RPCs to all other nodes.
3.  **Voting:** Other nodes will grant their vote if the candidate's term is valid and its log is at least as up-to-date as their own.
4.  **Winning:** If the Candidate receives votes from a **majority** of the cluster, it becomes the **Leader**.
5.  **Losing/Split Vote:** If another node becomes leader first, the Candidate steps down to a Follower. If no one wins (a split vote), the election timer expires, and a new election begins for the next term.

---

### Part 2: Log Replication

1.  **Client Request:** The Leader is the only node that accepts client commands (e.g., `set x=100`).
2.  **Append to Log:** The Leader appends the command to its own log as a new `LogEntry`.
3.  **Replicate:** The Leader sends `AppendEntries` RPCs to all Followers, containing the new log entry.
4.  **Commit:** Once a majority of Followers have successfully written the entry to their logs, the Leader considers the entry **committed**.
5.  **Apply:** The Leader applies the committed command to its own state machine (e.g., the `KeyValueStore`).
6.  **Notify Followers:** In subsequent `AppendEntries` RPCs, the Leader includes the updated `commitIndex`. Followers see this and apply the newly committed entries to their own state machines, ensuring consistency.

---

### Part 3: Safety and Consistency

To ensure logs stay consistent, the `AppendEntries` RPC includes a consistency check. The leader sends the index and term of the entry immediately preceding the new ones (`prevLogIndex`, `prevLogTerm`).

*   **If a Follower's log matches** at that point, it appends the new entries.
*   **If it doesn't match**, the Follower rejects the RPC. The Leader then decrements its index for that follower and retries with an earlier entry. This "probing" process continues until a common point is found, at which point the Leader can repair the Follower's log.

---

## 3. Code Implementation Mapping

This table maps the core Raft concepts to the files in this project:

| Concept / Responsibility                               | Primary File(s)                                                              | Description                                                                                                                              |
| ------------------------------------------------------ | ---------------------------------------------------------------------------- | ---------------------------------------------------------------------------------------------------------------------------------------- |
| **Orchestration & Timers**                             | `RaftNode.java`                                                              | The main class. Manages the election and heartbeat timers, orchestrates state transitions (`becomeLeader`), and handles client commands. |
| **Core Algorithm Logic**                               | `RaftLogic.java`                                                             | The heart of the protocol. Implements the rules for handling `RequestVote` and `AppendEntries` RPCs and advancing the commit index.       |
| **Node State**                                         | `NodeState.java`                                                             | A data class holding the in-memory and persistent state of a node (`currentTerm`, `votedFor`, `log`, `commitIndex`, etc.).                 |
| **Networking (Server)**                                | `NodeServer.java`                                                            | The networking layer. Listens for incoming connections, decodes RPCs, and passes them to `RaftLogic`.                                  |
| **Networking (Client)**                                | `NodeClient.java`                                                            | The networking layer for sending RPCs. Manages a connection pool to other nodes to avoid port exhaustion under load.                   |
| **Persistence Layer**                                  | `IStorageService.java`, `FileStorageService.java`                            | Responsible for saving the `currentTerm`, `votedFor`, and the log to disk to survive crashes.                                          |
| **State Machine**                                      | `StateMachine.java`, `KeyValueStore.java`                                    | The application layer. The `KeyValueStore` is the simple state machine that commands are applied to once they are committed.           |
| **RPC Data Structures**                                | `rpc/` directory (`RequestVoteRPC.java`, `AppendEntriesRPC.java`)            | Simple records that define the structure of the RPC requests and replies.                                                                |

---

## 4. How to Run the Tests

The project includes a correctness test and a high-performance scale test within `NodeServerTest.java`.

### Compiling the Project

From the root `java-design-snippets` directory, create an output directory and compile all Java files into it:

```bash
mkdir -p out # This step is only needed once
javac -d out $(find system_design/consensus/raft/ -name "*.java")
```

### Running the Test Scenarios

To run the tests, execute the `NodeServerTest` class. You can switch between the simple and complex test scenarios by editing the `main` method in `NodeServerTest.java`.

```bash
cd out
java system_design.consensus.raft.server.NodeServerTest
```