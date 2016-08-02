package org.alien4cloud.tosca.editor.processors;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.TreeSet;

import javax.inject.Inject;

import org.alien4cloud.tosca.editor.EditionContextManager;
import org.alien4cloud.tosca.editor.exception.InvalidPathException;
import org.alien4cloud.tosca.editor.operations.UpdateFileOperation;
import org.springframework.stereotype.Component;

import alien4cloud.component.repository.IFileRepository;
import alien4cloud.utils.TreeNode;
import lombok.SneakyThrows;

/**
 * FIXME process the upload of the YAML file.
 * FIXME in case of YAML file upload make sure archive name and version cannot be changed.
 *
 * Process an operation that uploaded or updated a file.
 */
@Component
public class UpdateFileProcessor implements IEditorCommitableProcessor<UpdateFileOperation>, IEditorOperationProcessor<UpdateFileOperation> {
    @Inject
    private IFileRepository artifactRepository;

    @Override
    @SneakyThrows
    public void process(UpdateFileOperation operation) {
        // archive content tree is actually a node that contains only the folder of the topology
        TreeNode root = EditionContextManager.get().getArchiveContentTree().getChildren().first();
        // walk the file path to insert an element
        TreeNode target = root;
        String[] pathElements = operation.getPath().split("/");
        for (int i = 0; i < pathElements.length; i++) {
            String pathElement = pathElements[i];
            TreeNode child = target.getChild(pathElement);
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
                    child.setChildren(new TreeSet<>());
                }
            }
            target = child;
        }
        if (target.isLeaf()) {
            // store the file in the local temporary file repository
            String artifactFileId = artifactRepository.storeFile(operation.getArtifactStream());
            target.setArtifactId(artifactFileId); // let's just impact the url to point to the temp file.
        } else {
            // Fail as we cannot override a directory
            throw new InvalidPathException();
        }
    }

    @Override
    @SneakyThrows
    public void beforeCommit(UpdateFileOperation operation) {
        Path targetPath = EditionContextManager.get().getLocalGitPath().resolve(operation.getPath());
        Files.copy(artifactRepository.getFile(operation.getTempFileId()), targetPath);
        artifactRepository.deleteFile(operation.getTempFileId());
    }
}
