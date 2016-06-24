package org.alien4cloud.tosca.editor.processors;

import java.util.ArrayList;

import org.alien4cloud.tosca.editor.TopologyEditionContextManager;
import org.alien4cloud.tosca.editor.exception.InvalidPathException;
import org.alien4cloud.tosca.editor.operations.UpdateFileOperation;
import org.springframework.stereotype.Component;

import alien4cloud.utils.TreeNode;
import lombok.SneakyThrows;

/**
 * FIXME process the upload of the YAML file.
 * FIXME in case of YAML file upload make sure archive name and version cannot be changed.
 *
 * Process an operation that uploaded or updated a file.
 */
@Component
public class UpdateFileProcessor implements IEditorOperationProcessor<UpdateFileOperation> {
    @Override
    @SneakyThrows
    public void process(UpdateFileOperation operation) {
        // archive content tree is actually a node that contains only the folder of the topology
        String initPath = TopologyEditionContextManager.get().getArchiveContentTree().getFullPath();
        TreeNode root = TopologyEditionContextManager.get().getArchiveContentTree().getChildren().get(0);
        // walk the file path to insert an element
        TreeNode target = root;
        String[] pathElements = operation.getPath().split("/");
        for (int i = 0; i < pathElements.length; i++) {
            String pathElement = pathElements[i];
            TreeNode child = getChild(target, pathElement);
            if (child == null) {
                if (target.isLeaf()) {
                    throw new InvalidPathException();
                }
                // add an element
                child = new TreeNode();
                child.setName(pathElement);
                child.setFullPath(target.getFullPath() + "/" + pathElement);
                child.setParent(target);
                target.getChildren().add(child);
                if (i == pathElements.length - 1) {
                    child.setLeaf(true);
                } else {
                    child.setChildren(new ArrayList<TreeNode>());
                }
            }
            target = child;
        }
        if (target.isLeaf()) {
            target.setArtifactId(operation.getTempFileId()); // let's just impact the url to point to the temp file.
        } else {
            // Fail as we cannot override a directory
            throw new InvalidPathException();
        }
    }

    private TreeNode getChild(TreeNode node, String childName) {
        for (TreeNode child : node.getChildren()) {
            if (child.getName().equals(childName)) {
                return child;
            }
        }
        return null;
    }
}
