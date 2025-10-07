// Copyright 2025 deepakp
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
// https://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

package data_structures.graph.dag.weighted;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.PriorityQueue;

import data_structures.graph.dag.weighted.model.GraphNode;
import data_structures.graph.dag.weighted.util.GraphUtil;

public final class DijkstraShortestPathTest {
    private DijkstraShortestPathTest() {
        // Prevents Instantiation
    }

    public static void main(String[] args) throws IOException {

        // Test the simple graph building and printing
        Map<String, List<GraphNode>> graph = GraphUtil
                .buildWeightedGraphFromTextFile("data_structures/graph/data/connected-cities-weighted-graph.txt");
        GraphUtil.printWeightedGraph(graph);

        System.out.println("\n--- Finding Shortest Path from Delhi to Kochi ---");
        PathResult result = findShortestPath("Srinagar", "Kochi", graph);

        if (result.distance == Integer.MAX_VALUE) {
            System.out.println("No path found from Srinagar to Kochi.");
        } else {
            System.out.println("Shortest Distance: " + result.distance);
            System.out.println("Path: " + String.join(" -> ", result.path));
        }
    }

    private static PathResult findShortestPath(String src, String dst, Map<String, List<GraphNode>> graph) {
        // Map to store the shortest distance from the source to each node.
        Map<String, Integer> distances = new HashMap<>();
        // Map to store the predecessor of each node in the shortest path.
        Map<String, String> predecessors = new HashMap<>();
        // Priority queue to always process the node with the smallest distance first.
        // Stores a [nodeName, distance] pair.
        Queue<Map.Entry<String, Integer>> pq = new PriorityQueue<>(Map.Entry.comparingByValue());

        // Step 1: Initialize distances to all nodes as infinity, except for the
        // source.
        // Ensure all nodes, including those that are only destinations, are in the map.
        for (String node : graph.keySet()) {
            distances.put(node, Integer.MAX_VALUE);
            for (GraphNode neighbor : graph.get(node)) {
                distances.putIfAbsent(neighbor.getNodeVal(), Integer.MAX_VALUE);
            }
        }
        distances.put(src, 0);

        // Add the source node to the priority queue.
        pq.add(Map.entry(src, 0));

        while (!pq.isEmpty()) {
            Map.Entry<String, Integer> currentEntry = pq.poll();
            String currentNode = currentEntry.getKey();
            int currentDistance = currentEntry.getValue();

            // If we've found a shorter path to this node already, skip it.
            if (currentDistance > distances.get(currentNode)) {
                continue;
            }

            // If we've reached the destination, we can stop.
            if (currentNode.equals(dst)) {
                break;
            }

            // Step 2: For the current node, consider all its neighbors.
            for (GraphNode neighbor : graph.getOrDefault(currentNode, Collections.emptyList())) {
                int newDist = currentDistance + neighbor.getEdgeWeight();

                // Step 3: If a shorter path to the neighbor is found, update its distance and
                // predecessor.
                if (newDist < distances.get(neighbor.getNodeVal())) {
                    distances.put(neighbor.getNodeVal(), newDist);
                    predecessors.put(neighbor.getNodeVal(), currentNode);
                    pq.add(Map.entry(neighbor.getNodeVal(), newDist));
                }
            }
        }

        // Step 4: Reconstruct the path from the predecessors map.
        List<String> path = new ArrayList<>();
        String step = dst;
        // If the destination was never reached, there's no path.
        if (predecessors.get(step) == null && !step.equals(src)) {
            return new PathResult(Integer.MAX_VALUE, Collections.emptyList());
        }
        while (step != null) {
            path.add(step);
            step = predecessors.get(step);
        }
        Collections.reverse(path);

        return new PathResult(distances.get(dst), path);
    }

    // A simple record to hold the result.
    private record PathResult(int distance, List<String> path) {
    }
}