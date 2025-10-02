package system_design.scalability;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.SortedMap;
import java.util.TreeMap;

/**
 * An implementation of a consistent hashing ring using virtual nodes
 * (replicas).
 * This implementation maps keys to nodes (e.g., servers) in a way that
 * minimizes
 * remapping when nodes are added or removed.
 * Using virtual nodes leads to a more uniform distribution of keys across the
 * physical nodes.
 */
public class ConsistenHashingVNode implements IConsistentHashing {

    // A sorted map to represent the hash ring. The key is the hash value, and the
    // value is the physical node identifier.
    private final SortedMap<Long, String> nodeRing = new TreeMap<>();
    private final int replicas;

    public ConsistenHashingVNode(int replicas) {
        this.replicas = replicas;
    }

    @Override
    public String getMappedNode(String key) {
        if (nodeRing.isEmpty()) {
            return null; // No nodes available in the ring.
        }
        long hashKey = getHash(key);

        // Find the first node on the ring with a hash greater than or equal to the
        // key's hash.
        SortedMap<Long, String> nextNodes = nodeRing.tailMap(hashKey);

        // If tailMap is empty, it means the key's hash is larger than any node's hash.
        // In this "wrap-around" case, we assign the key to the very first node on the
        // ring.
        long nodeHash = nextNodes.isEmpty() ? nodeRing.firstKey() : nextNodes.firstKey();
        return nodeRing.get(nodeHash);
    }

    @Override
    public void addNode(String node) {
        // For each physical node, add multiple virtual nodes to the ring.
        for (int i = 0; i < replicas; i++) {
            String virtualNodeName = node + "#" + i;
            long hashKey = getHash(virtualNodeName);
            nodeRing.put(hashKey, node);
        }
    }

    @Override
    public void removeNode(String node) {
        // Remove all virtual nodes for the given physical node.
        for (int i = 0; i < replicas; i++) {
            long hashKey = getHash(node + "#" + i);
            nodeRing.remove(hashKey);
        }
    }

    private long getHash(String node) {
        try {
            // Use a standard, high-quality hashing algorithm like SHA-256.
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] digest = md.digest(node.getBytes());
            long hashKey = 0;
            // Use the first 8 bytes of the digest to construct a 64-bit long hash value.
            for (int i = 0; i < 8; i++) {
                hashKey = (hashKey << 8) | (digest[i] & 0xff);
            }
            return hashKey;
        } catch (NoSuchAlgorithmException nsa) {
            throw new RuntimeException("SHA-256 algorithm is not supported!");
        }
    }
}

interface IConsistentHashing {
    String getMappedNode(String key);

    void addNode(String node);

    void removeNode(String node);
}