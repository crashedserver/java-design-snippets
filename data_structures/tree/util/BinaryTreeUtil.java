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
package data_structures.tree.util;

import data_structures.tree.model.BinaryTreeNode;

public final class BinaryTreeUtil {

    private BinaryTreeUtil() {
        // prevent instantiation
    }

    public static BinaryTreeNode buildBinaryTree(Integer[] levelOrderArray, int i) {
        if (i >= levelOrderArray.length || levelOrderArray[i] == null) {
            return null;
        }
        BinaryTreeNode node = new BinaryTreeNode(levelOrderArray[i]);
        node.left = buildBinaryTree(levelOrderArray, 2 * i + 1);
        node.right = buildBinaryTree(levelOrderArray, 2 * i + 2);
        return node;
    }

}
