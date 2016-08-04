package org.alien4cloud.tosca.editor.processors;

import java.nio.file.Path;

import org.alien4cloud.tosca.editor.EditionContextManager;
import org.alien4cloud.tosca.editor.exception.InvalidPathException;
import org.alien4cloud.tosca.editor.operations.DeleteFileOperation;
import org.springframework.stereotype.Component;

import alien4cloud.utils.FileUtil;
import alien4cloud.utils.TreeNode;
import lombok.SneakyThrows;

/**
 * This processor is responsible for deletion of a file within the archive.
 */
@Component
public class DeleteFileProcessor implements IEditorCommitableProcessor<DeleteFileOperation>, IEditorOperationProcessor<DeleteFileOperation> {

    @Override
    public void process(DeleteFileOperation operation) {
        if (EditionContextManager.get().getTopology().getYamlFilePath().equals(operation.getPath())) {
            throw new InvalidPathException("Topology yaml file cannot be removed.");
        }
        TreeNode target = FileProcessorHelper.getFileTreeNode(operation.getPath());
        target.getParent().getChildren().remove(target);
    }

    @Override
    @SneakyThrows
    public void beforeCommit(DeleteFileOperation operation) {
        // remove the file on the local repository
        Path targetPath = EditionContextManager.get().getLocalGitPath().resolve(operation.getPath());
        FileUtil.delete(targetPath);
    }
}