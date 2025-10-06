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

package data_structures.tree;

import data_structures.tree.model.BinaryTreeNode;
import data_structures.tree.util.BinaryTreeUtil;

public final class ValidateBST {
    private ValidateBST() {
        // prevent instantiation
    }

    public static void main(String[] args) {
        Integer[] validBST = new Integer[] { 8, 3, 10, 1, 6, null, 14, null, null, 4, 7, 13, null };
        Integer[] invalidBST = new Integer[] { 5, 3, 8, 1, 6, 7, 9 };
        // Valid Binary Tree
        BinaryTreeNode root = BinaryTreeUtil.buildBinaryTree(validBST, 0);
        System.out.println(isValidBinarySearchTree(root));
        // Invalid Binary Tree
        root = BinaryTreeUtil.buildBinaryTree(invalidBST, 0);
        System.out.println(isValidBinarySearchTree(root));
    }

    private static boolean isValidBinarySearchTree(BinaryTreeNode node) {
        // To validate the entire tree, start with the widest possible range.
        // null : -infinity or +infinity
        return isValidDFS(node, null, null);
    }

    /**
     * Recursively validates that the subtree rooted at `node` adheres to the BST
     * property within the given (min, max) range.
     *
     * @param node The current node to validate.
     * @param min  The minimum allowed value for this node (exclusive).
     * @param max  The maximum allowed value for this node (exclusive).
     * @return True if the subtree is a valid BST, false otherwise.
     */
    private static boolean isValidDFS(BinaryTreeNode node, Integer min, Integer max) {
        if (node == null) {
            return true;
        }

        // The current node's value must be within the valid range defined by its
        // ancestors.
        if ((min != null && node.data < min) || (max != null && node.data > max)) {
            return false;
        }

        // Recursively check the left and right subtrees
        // For the left child, the new maximum is the current node's value.
        // For the right child, the new minimum is the current node's value.
        return isValidDFS(node.left, min, node.data) && isValidDFS(node.right, node.data, max);
    }
}
