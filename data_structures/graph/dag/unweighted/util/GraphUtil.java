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

package data_structures.graph.dag.unweighted.util;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class GraphUtil {
    private GraphUtil() {
        // Prevent instantiation
    }

    /*
     * Graph file format:
     * A: B
     * B: D E
     * C: A
     * D:
     */
    public static Map<String, List<String>> buildGraphFromTextFile(String filePath) throws IOException {
        Map<String, List<String>> adjList = new HashMap<>();

        File graphFile = new File(filePath);
        try (BufferedReader reader = new BufferedReader(new FileReader(graphFile))) {
            String line;
            while ((line = reader.readLine()) != null) {
                String[] tokens = line.split(":");
                if (tokens == null || tokens.length == 0) {
                    System.err.printf("Invalid entry in the file %s line : %s\n", graphFile, line);
                    continue;
                }
                String keyNode = tokens[0];
                if (tokens.length == 1 || tokens[1] == null || tokens[1].length() == 0) {
                    adjList.put(keyNode, new ArrayList<>()); // Use an empty list instead of null
                    continue;
                }
                String[] valueNodes = tokens[1].split(" ");
                List<String> nodeList = new ArrayList<>();
                for (String n : valueNodes) {
                    if (!"".equals(n)) {
                        nodeList.add(n);
                    }
                }
                if (!nodeList.isEmpty()) {
                    adjList.put(keyNode, nodeList);
                } else {
                    adjList.put(keyNode, new ArrayList<>());
                }

            }
        }
        return adjList;
    }

    public static void printGraph(Map<String, List<String>> graph) {
        if (graph == null || graph.isEmpty()) {
            return;
        }
        for (Map.Entry<String, List<String>> entry : graph.entrySet()) {
            List<String> nodes = entry.getValue();
            if (nodes == null) {
                System.out.println(entry.getKey() + "->");
                continue;
            }
            for (String dest : entry.getValue()) {
                System.out.println(entry.getKey() + "->" + dest);
            }

        }
    }    
}

