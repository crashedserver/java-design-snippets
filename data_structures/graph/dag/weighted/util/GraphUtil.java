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

package data_structures.graph.dag.weighted.util;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import data_structures.graph.dag.weighted.model.GraphNode;

public final class GraphUtil {
    private GraphUtil() {
        // Prevent instantiation
    }

    public static void printWeightedGraph(Map<String, List<GraphNode>> graph) {
        if (graph == null || graph.isEmpty()) {
            return;
        }
        for (Map.Entry<String, List<GraphNode>> entry : graph.entrySet()) {
            List<GraphNode> nodes = entry.getValue();
            if (nodes == null) {
                System.out.println(entry.getKey() + "->");
                continue;
            }
            for (GraphNode dest : nodes) {
                System.out.printf("%s --%d--> %s\n", entry.getKey(), dest.getEdgeWeight(), dest.getNodeVal());
            }

        }
    }

    /*
     * Weighted Graph file format:
     * A: B=5 C=2
     * B: D=1 E=8
     * C: A=3
     * D:
     */
    public static Map<String, List<GraphNode>> buildWeightedGraphFromTextFile(String filePath) throws IOException {
        Map<String, List<GraphNode>> adjList = new HashMap<>();

        File graphFile = new File(filePath);
        try (BufferedReader reader = new BufferedReader(new FileReader(graphFile))) {
            String line;
            int lineNumber = 0;
            while ((line = reader.readLine()) != null) {
                lineNumber++;
                String[] tokens = line.split(":");
                if (tokens.length == 0) {
                    System.err.printf("Invalid entry in the file %s line : %s\n", graphFile, line);
                    continue;
                }
                String keyNode = tokens[0].trim();
                if (tokens.length == 1 || tokens[1] == null || tokens[1].trim().isEmpty()) {
                    adjList.put(keyNode, new ArrayList<>()); // Use an empty list instead of null
                    continue;
                }
                String[] valueNodes = tokens[1].trim().split("\\s+");
                List<GraphNode> nodeList = new ArrayList<>();
                for (String n : valueNodes) {
                    if (!n.isEmpty()) {
                        String[] t = n.split("=", 2);
                        if (t.length < 2 || t[0].isEmpty() || t[1].isEmpty()) {
                            throw new IOException(
                                    "Invalid weighted edge format on line " + lineNumber + ": '" + n + "'");
                        }
                        nodeList.add(new GraphNode(t[0], Integer.parseInt(t[1])));
                    }
                }
                adjList.put(keyNode, nodeList);
            }
        }
        return adjList;
    }

}
