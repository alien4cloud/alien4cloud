package org.alien4cloud.tosca.editor.processors;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.TreeSet;

import javax.inject.Inject;

import org.alien4cloud.tosca.editor.EditionContextManager;
import org.alien4cloud.tosca.editor.exception.InvalidPathException;
import org.alien4cloud.tosca.editor.operations.AbstractUpdateFileOperation;
import org.alien4cloud.tosca.editor.services.EditorTopologyUploadService;

import alien4cloud.component.repository.IFileRepository;
import alien4cloud.exception.NotFoundException;
import alien4cloud.utils.TreeNode;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;

/**
 * Process an operation that uploaded or updated a file.
 */
@Slf4j
public abstract class AbstractUpdateFileProcessor<T extends AbstractUpdateFileOperation>
        implements IEditorCommitableProcessor<T>, IEditorOperationProcessor<T> {
    @Inject
    private IFileRepository artifactRepository;
    @Inject
    private EditorTopologyUploadService editorTopologyUploadService;

    @Override
    public void process(T operation) {
        // archive content tree is actually a node that contains only the folder of the topology
        TreeNode root = EditionContextManager.get().getArchiveContentTree().getChildren().first();
        // walk the file path to insert an element
        TreeNode target = root;
        if (operation.getPath().endsWith("/")) {
            throw new InvalidPathException("Path <" + operation.getPath() + "> is invalid (must be a file and not a directory).");
        }

        // File upload management
        String[] pathElements = operation.getPath().split("/");
        for (int i = 0; i < pathElements.length; i++) {
            String pathElement = pathElements[i];
            TreeNode child = target.getChild(pathElement);
            if (child == null) {
                if (target.isLeaf()) {
                    throw new InvalidPathException("Path <" + operation.getPath() + "> is invalid (one of the folder of the path is actualy a file).");
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
            // If already applied the input stream is closed and I should just get the artifact id
            String artifactFileId = operation.getTempFileId();
            if (artifactFileId == null) {
                artifactFileId = artifactRepository.storeFile(operation.getArtifactStream());
                operation.setTempFileId(artifactFileId);
                operation.setArtifactStream(null); // Note the input stream should be closed by the caller (controller in our situation).
            }

            try {
                if (EditionContextManager.getCsar().getYamlFilePath().equals(operation.getPath())) {
                    // the operation updates the topology file, we have to parse it and override the topology data out of it.
                    editorTopologyUploadService.processTopology(artifactRepository.resolveFile(artifactFileId),
                            EditionContextManager.getTopology().getWorkspace());
                }
            } catch (RuntimeException e) {
                // remove the file from the temp repository if the topology cannot be parsed
                artifactRepository.deleteFile(artifactFileId);
                throw e;
            }

            target.setArtifactId(artifactFileId); // let's just impact the url to point to the temp file.
        } else {
            // Fail as we cannot override a directory
            throw new InvalidPathException("Path <" + operation.getPath() + "> is invalid (must be a file and not a directory).");
        }
    }

    @Override
    @SneakyThrows
    public void beforeCommit(T operation) {
        try {
            TreeNode fileTreeNode = FileProcessorHelper.getFileTreeNode(operation.getPath());
            Path targetPath = EditionContextManager.get().getLocalGitPath().resolve(operation.getPath());
            Files.createDirectories(targetPath.getParent());
            try (InputStream inputStream = artifactRepository.getFile(operation.getTempFileId())) {
                Files.copy(inputStream, targetPath, StandardCopyOption.REPLACE_EXISTING);
            }
            artifactRepository.deleteFile(operation.getTempFileId());
            fileTreeNode.setArtifactId(null);
        } catch (NotFoundException e) {
            log.debug("The file is not referenced in the tree, must have been deleted in later operation.", e);
        }
    }
}
