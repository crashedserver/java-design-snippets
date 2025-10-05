package system_design.consensus.raft.server;

import java.io.File;
import system_design.consensus.raft.IRaftNode;
import system_design.consensus.raft.RaftNode;
import system_design.consensus.raft.RaftConfig;
import system_design.consensus.raft.KeyValueStore;
import system_design.consensus.raft.PeerState;
import system_design.consensus.raft.storage.IStorageService;
import system_design.consensus.raft.storage.file_storage.FileStorageService;
import system_design.consensus.raft.storage.model.NodeId;
import system_design.consensus.raft.storage.model.RaftRole;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.Collections;
import java.util.function.BiConsumer;
import java.util.concurrent.TimeUnit;

public final class NodeServerTest {

    private NodeServerTest() {
        // Prevents instantiation
    }

    public static void main(String[] args) throws InterruptedException {
        runBasicClusterCorrectnessTest();
        runScaleTest();
    }

    public static void runBasicClusterCorrectnessTest() {
        runTest("Raft Cluster Correctness Test", (cluster, leader) -> {
            // 4. Use public APIs to 'put' data into the cluster
            System.out.println("\n--- Submitting commands to the leader ---");
            submitCommand(leader, "set a=100");
            submitCommand(leader, "set b=200");
            submitCommand(leader, "set a=150"); // Overwrite a value

            try {
                Thread.sleep(1000); // Allow time for replication to followers
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }

            verifyStateConsistency(cluster);
        });
    }

    private static void verifyStateConsistency(List<IRaftNode<String>> cluster) {
        System.out.println("\n--- Verifying state consistency across all nodes ---");
        boolean allConsistent = true;
        for (IRaftNode<String> node : cluster) {
            String valueA = node.getFromStateMachine("a");
            String valueB = node.getFromStateMachine("b");

            System.out.printf("Verifying Node %s (%s): a=%s, b=%s\n",
                    node.getSelfId().getNodeId(), node.getRole(), valueA, valueB);

            if (!"150".equals(valueA) || !"200".equals(valueB)) {
                allConsistent = false;
                System.err.println("  -> INCONSISTENT STATE DETECTED!");
            } else {
                System.out.println("  -> State is consistent.");
            }
        }

        System.out.println("\n--- Test Summary ---");
        if (allConsistent) {
            System.out.println("SUCCESS: All nodes reached a consistent state.");
        } else {
            System.err.println("FAILURE: Cluster is in an inconsistent state.");
        }
    }

    /** A helper to find the leader, retrying until one is elected. */
    private static IRaftNode<String> findLeader(List<IRaftNode<String>> cluster) throws InterruptedException {
        for (int i = 0; i < 10; i++) { // Try for up to 5 seconds
            IRaftNode<String> leader = cluster.stream()
                    .filter(n -> n.getRole() == RaftRole.LEADER && n.getPort() != -1)
                    .findFirst().orElse(null);
            if (leader != null) {
                return leader;
            }
            Thread.sleep(500);
        }
        throw new RuntimeException("Failed to elect a leader in a reasonable time.");
    }

    /** A helper to submit a command to the leader. */
    private static void submitCommand(IRaftNode<String> leader, String command) {
        try {
            System.out.printf("Submitting command '%s' to leader %s...\n", command, leader.getSelfId().getNodeId());
            leader.submitCommand(command).get(2, TimeUnit.SECONDS);
            System.out.printf("  -> SUCCESS: Leader %s confirmed command was committed.\n",
                    leader.getSelfId().getNodeId());
        } catch (Exception e) {
            System.err.printf("  -> FAILED to submit command '%s': %s\n", command, e.getMessage());
        }
    }

    /** A helper to recursively delete a directory. */
    private static void deleteDirectory(File directory) {
        if (!directory.exists()) {
            return;
        }
        File[] allContents = directory.listFiles();
        if (allContents != null) {
            for (File file : allContents) {
                deleteDirectory(file);
            }
        }
        directory.delete();
    }

    public static void runScaleTest() {
        runTest("Raft Scale Test", (cluster, leader) -> {
            int numCommands = 10000;
            // 3. Submit a large number of commands and measure performance
            System.out.printf("\n--- Submitting %d commands to the leader to test scale ---\n", numCommands);

            long startTime = System.currentTimeMillis();

            final List<Long> latencies = Collections.synchronizedList(new ArrayList<>(numCommands));
            List<CompletableFuture<Void>> allFutures = new ArrayList<>();

            for (int i = 0; i < numCommands; i++) {
                String command = "set key_" + i + "=value_" + i;
                long commandStartTime = System.nanoTime();

                CompletableFuture<Boolean> commandFuture = leader.submitCommand(command);

                // Chain a task to calculate latency when the command future completes.
                CompletableFuture<Void> latencyFuture = commandFuture.thenRun(() -> {
                    long latencyNanos = System.nanoTime() - commandStartTime;
                    latencies.add(TimeUnit.NANOSECONDS.toMillis(latencyNanos));
                });
                allFutures.add(latencyFuture);
            }

            // Wait for all commands to be committed and their latencies recorded.
            System.out.println("Waiting for all commands to be committed...");
            awaitAll(allFutures);

            long endTime = System.currentTimeMillis();
            long duration = endTime - startTime;

            System.out.println("\n--- Scale Test Summary ---");
            System.out.printf("Successfully submitted and committed %d commands.\n", numCommands);
            System.out.printf("Total time taken: %,d ms\n", duration);
            if (duration > 0) {
                System.out.printf("Overall Throughput: %,.2f commands/sec\n", (double) numCommands / duration * 1000);
            }

            // Calculate and print latency percentiles
            Collections.sort(latencies);
            System.out.println("\n--- Latency Statistics ---");
            System.out.printf("p50 (Median): %d ms\n", latencies.get(latencies.size() / 2));
            System.out.printf("p95: %d ms\n", latencies.get((int) (latencies.size() * 0.95)));
            System.out.printf("p99: %d ms\n", latencies.get((int) (latencies.size() * 0.99)));
            System.out.printf("Max Latency: %d ms\n", latencies.get(latencies.size() - 1));

            // 4. Verify the final state of a random key to ensure consistency.
            verifyFollowerState(cluster, numCommands);
        });
    }

    private static void verifyFollowerState(List<IRaftNode<String>> cluster, int numCommands) {
        System.out.println("\n--- Final State Verification ---");
        String lastKey = "key_" + (numCommands - 1);
        String expectedValue = "value_" + (numCommands - 1);
        IRaftNode<String> followerToVerify = cluster.get(0);

        // Find the follower to verify, making sure it's not the leader.
        if (followerToVerify.getRole() == RaftRole.LEADER) {
            followerToVerify = cluster.get(1);
        }

        System.out.printf("Waiting for follower %s to apply final entry...\n",
                followerToVerify.getSelfId().getNodeId());
        String actualValue = null;
        try {
            for (int i = 0; i < 20; i++) { // Poll for up to 2 seconds.
                actualValue = followerToVerify.getFromStateMachine(lastKey);
                if (expectedValue.equals(actualValue)) {
                    break;
                }
                Thread.sleep(100);
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        System.out.printf("Verifying last key ('%s'): value = %s\n", lastKey, actualValue);
        if (!expectedValue.equals(actualValue)) {
            System.err.println("  -> FAILED: Follower state is not consistent after waiting.");
        }
    }

    private static void runTest(String testName, BiConsumer<List<IRaftNode<String>>, IRaftNode<String>> testLogic) {
        System.out.println("--- " + testName + " ---");
        List<IRaftNode<String>> cluster = null;

        deleteDirectory(new File("data"));

        try {
            cluster = createCluster(3, 12345);
            startCluster(cluster);

            System.out.println("\n--- Waiting for a leader to be elected ---");
            IRaftNode<String> leader = findLeader(cluster); // This can throw RuntimeException
            System.out.printf("Leader elected: Node %s on port %d.\n", leader.getSelfId().getNodeId(),
                    leader.getPort());

            testLogic.accept(cluster, leader);

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            System.err.println("Test was interrupted.");
        } finally {
            if (cluster != null) {
                shutdownCluster(cluster);
            }
        }
    }

    private static List<IRaftNode<String>> createCluster(int numNodes, int basePort) {
        System.out.println("Creating " + numNodes + " Raft nodes...");
        List<IRaftNode<String>> cluster = new ArrayList<>();
        for (int i = 0; i < numNodes; i++) {
            String storageDir = "data/raft_node_" + i;
            IStorageService<String> storage = new FileStorageService<>(storageDir);
            List<PeerState> peers = new ArrayList<>();
            for (int j = 0; j < numNodes; j++) {
                if (i != j) {
                    peers.add(new PeerState(new RaftNode.Peer("localhost", basePort + j)));
                }

            }
            RaftConfig<String> config = new RaftConfig<>(new NodeId(String.valueOf(i)), basePort + i, peers,
                    new KeyValueStore());
            cluster.add(new RaftNode<>(config, storage));
        }
        return cluster;
    }

    private static void startCluster(List<IRaftNode<String>> cluster) {
        System.out.println("\nStarting all nodes...");
        List<CompletableFuture<Void>> startupFutures = new ArrayList<>();
        cluster.forEach(node -> startupFutures.add(node.start()));
        awaitAll(startupFutures);
        System.out.println("All servers are running.");
    }

    private static void awaitAll(List<? extends CompletableFuture<?>> futures) {
        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();
    }

    private static void shutdownCluster(List<IRaftNode<String>> cluster) {
        System.out.println("\nShutting down all server nodes...");
        cluster.forEach(IRaftNode::stop);
    }
}
