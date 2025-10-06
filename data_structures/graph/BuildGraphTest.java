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

package data_structures.graph;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import data_structures.graph.util.GraphUtil;

public final class BuildGraphTest {
    private BuildGraphTest() {
        // Prevents Instantiation
    }

    public static void main(String[] args) throws IOException {

        //Test the simple graph building and printing
        Map<String, List<String>> simpleGraph = GraphUtil
                .buildGraphFromTextFile("data_structures/graph/data/simple_graph.txt");
        GraphUtil.printGraph(simpleGraph);

        // Test the simple graph building and printing
        Map<String, List<String>> mavenGraph = GraphUtil
                .buildGraphFromTextFile("data_structures/graph/data/simple-maven-dependency-graph.txt");
        GraphUtil.printGraph(mavenGraph);

    }

}