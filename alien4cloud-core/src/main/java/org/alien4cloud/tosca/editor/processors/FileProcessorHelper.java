package org.alien4cloud.tosca.editor.processors;

import org.alien4cloud.tosca.editor.EditionContextManager;

import alien4cloud.exception.NotFoundException;
import alien4cloud.utils.TreeNode;

/**
 * Utility method to process file operations in the editor context.
 */
public final class FileProcessorHelper {
    private FileProcessorHelper() {
    }

    /**
     * Get the tree node that represents a file from the archive under edition.
     * 
     * @param path The path in which to lookup for the
     * @return the tree node from the archive.
     */
    public static TreeNode getFileTreeNode(String path) {
        TreeNode root = EditionContextManager.get().getArchiveContentTree().getChildren().first();
        TreeNode target = root;
        String[] pathElements = path.split("/");
        for (int i = 0; i < pathElements.length; i++) {
            String pathElement = pathElements[i];
            TreeNode child = target.getChild(pathElement);
            if (child == null) {
                throw new NotFoundException("The artifact specified at path <" + path + "> does not exists in the topology archive.");
            }
            target = child;
        }
        return target;
    }
}
