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

package data_structures.graph.dag.unweighted;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;

import data_structures.graph.dag.unweighted.util.GraphUtil;

public final class EvaluateMavenDependencyOrder {
    private EvaluateMavenDependencyOrder() {
        // preventing instantiation
    }

    public static void main(String[] args) throws IOException {
        System.out.println("-- TEST1 --");
        System.out.println("-- The following graph should not produce a dependency cycle --");
        Map<String, List<String>> dependencyGraphWithoutCycle = GraphUtil
                .buildGraphFromTextFile("data_structures/graph/data/simple-maven-dependency-graph.txt");
        System.out.println("-- Graph Content --");
        GraphUtil.printGraph(dependencyGraphWithoutCycle);
        List<String> buildOrder = getBuildOrder(dependencyGraphWithoutCycle);
        System.out.println("-- Build order --");
        if (buildOrder != null) {
            System.out.println("Valid Maven Build Order:");
            buildOrder.forEach(System.out::println);
        } else {
            System.err.println("Error: A circular dependency was detected. Cannot determine a valid build order.");
        }
        System.out.println("-- TEST2 --");
        System.out.println("-- The following graph should produce a dependency cycle --");
        Map<String, List<String>> dependencyGraphWithCycle = GraphUtil
                .buildGraphFromTextFile("data_structures/graph/data/simple-maven-dependency-graph-cycle.txt");
        System.out.println("-- Graph Content --");
        GraphUtil.printGraph(dependencyGraphWithCycle);
        List<String> buildOrder2 = getBuildOrder(dependencyGraphWithCycle);
        System.out.println("-- Build order --");
        if (buildOrder2 != null) {
            System.out.println("Valid Maven Build Order:");
            buildOrder2.forEach(System.out::println);
        } else {
            System.err.println("Error: A circular dependency was detected. Cannot determine a valid build order.");
        }

    }

    private static List<String> getBuildOrder(Map<String, List<String>> dependencyGraph) {
        if (dependencyGraph == null || dependencyGraph.isEmpty()) {
            return new ArrayList<>();
        }

        // Step 1: Calculate in-degrees for all projects.
        Map<String, Integer> inDegrees = new HashMap<>();
        // Ensure all unique projects (both keys and dependencies) are in the map. Check
        // keys and values
        for (String project : dependencyGraph.keySet()) {
            inDegrees.put(project, 0);
            dependencyGraph.get(project).forEach(dependency -> inDegrees.put(dependency, 0));
        }

        // The in-degree for a build order is the number of dependencies a project has.
        dependencyGraph.forEach((project, dependencies) -> inDegrees.put(project, dependencies.size()));

        // Step 2: Initialize a queue with all projects that have no dependencies
        // (in-degree of 0).
        Queue<String> queue = new LinkedList<>();
        for (Map.Entry<String, Integer> entry : inDegrees.entrySet()) {
            if (entry.getValue() == 0) {
                queue.offer(entry.getKey());
            }
        }

        // Step 3: Process the queue to determine the build order. Project with 0
        // dependencies can be installed without any issues
        List<String> buildOrder = new ArrayList<>();
        while (!queue.isEmpty()) {
            String project = queue.poll();
            buildOrder.add(project);

            // For every other project in the graph, if it depends on the project we just
            // "built", decrement its in-degree.
            for (Map.Entry<String, List<String>> entry : dependencyGraph.entrySet()) {
                String dependentProject = entry.getKey();
                List<String> dependencies = entry.getValue();

                if (dependencies.contains(project)) {
                    int newDegree = inDegrees.get(dependentProject) - 1;
                    inDegrees.put(dependentProject, newDegree);

                    if (newDegree == 0) {
                        queue.offer(dependentProject);
                    }
                }
            }
        }

        // Step 4: If the build order doesn't include all projects, there's a cycle.
        // The total number of unique nodes is the size of the inDegrees map.
        if (buildOrder.size() != inDegrees.size()) {
            return null; // Indicates a circular dependency.
        }

        return buildOrder;
    }
}