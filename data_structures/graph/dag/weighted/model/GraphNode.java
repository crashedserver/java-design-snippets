/*
 * Copyright 2025 deepakp
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package data_structures.graph.dag.weighted.model;

public class GraphNode {
    private final String nodeVal;
    private final int edgeWeight;

    public GraphNode(String nodeVal, int edgeWeight) {
        this.nodeVal = nodeVal;
        this.edgeWeight = edgeWeight;
    }

    public String getNodeVal() {
        return nodeVal;
    }

    public int getEdgeWeight() {
        return edgeWeight;
    }
}