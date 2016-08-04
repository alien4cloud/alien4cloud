package org.alien4cloud.tosca.editor.processors;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.TreeSet;

import javax.inject.Inject;

import org.alien4cloud.tosca.editor.EditionContextManager;
import org.alien4cloud.tosca.editor.exception.EditorToscaYamlUpdateException;
import org.alien4cloud.tosca.editor.exception.InvalidPathException;
import org.alien4cloud.tosca.editor.operations.AbstractUpdateFileOperation;

import alien4cloud.component.repository.IFileRepository;
import alien4cloud.model.components.IndexedToscaElement;
import alien4cloud.model.topology.Topology;
import alien4cloud.paas.wf.WorkflowsBuilderService;
import alien4cloud.tosca.ArchiveParser;
import alien4cloud.tosca.context.ToscaContext;
import alien4cloud.tosca.model.ArchiveRoot;
import alien4cloud.tosca.parser.ParsingErrorLevel;
import alien4cloud.tosca.parser.ParsingException;
import alien4cloud.tosca.parser.ParsingResult;
import alien4cloud.utils.TreeNode;
import lombok.SneakyThrows;

/**
 * Process an operation that uploaded or updated a file.
 */
public abstract class AbstractUpdateFileProcessor<T extends AbstractUpdateFileOperation>
        implements IEditorCommitableProcessor<T>, IEditorOperationProcessor<T> {
    @Inject
    private IFileRepository artifactRepository;
    @Inject
    private ArchiveParser archiveParser;
    @Inject
    private WorkflowsBuilderService workflowBuilderService;

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
                if (EditionContextManager.getTopology().getYamlFilePath().equals(operation.getPath())) {
                    // the operation updates the topology file, we have to parse it and override the topology data out of it.
                    processTopology(operation, artifactFileId);
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
        Path targetPath = EditionContextManager.get().getLocalGitPath().resolve(operation.getPath());
        Files.createDirectories(targetPath.getParent());
        try (InputStream inputStream = artifactRepository.getFile(operation.getTempFileId())) {
            Files.copy(inputStream, targetPath, StandardCopyOption.REPLACE_EXISTING);
        }
        FileProcessorHelper.getFileTreeNode(operation.getPath()).setArtifactId(null);
        artifactRepository.deleteFile(operation.getTempFileId());
    }

    /**
     * Process the topology uploaded from the given file in the local repository.
     *
     * @param operation The operation that triggered the topology refresh.
     * @param artifactFileId The
     */
    private void processTopology(T operation, String artifactFileId) {
        // parse the archive.
        try {
            ParsingResult<ArchiveRoot> parsingResult = archiveParser.parse(artifactRepository.resolveFile(artifactFileId), true);

            // check if any blocker error has been found during parsing process.
            if (parsingResult.hasError(ParsingErrorLevel.ERROR)) {
                // do not save anything if any blocker error has been found during import.

                throw new EditorToscaYamlUpdateException("Uploaded yaml files is not a valid tosca template", parsingResult.getContext().getParsingErrors());
            }
            if (parsingResult.getResult().hasToscaTypes()) {
                throw new EditorToscaYamlUpdateException("Tosca types are currently not supported in the topology editor context.");
            }
            if (!parsingResult.getResult().hasToscaTopologyTemplate()) {
                throw new EditorToscaYamlUpdateException("A topology template is required in the topology edition context.");
            }

            Topology parsedTopology = parsingResult.getResult().getTopology();
            ToscaContext.get().updateDependencies(parsedTopology.getDependencies());

            // init the workflows for the topology based on the yaml
            WorkflowsBuilderService.TopologyContext topologyContext = workflowBuilderService
                    .buildCachedTopologyContext(new WorkflowsBuilderService.TopologyContext() {
                        @Override
                        public Topology getTopology() {
                            return parsedTopology;
                        }

                        @Override
                        public <T extends IndexedToscaElement> T findElement(Class<T> clazz, String id) {
                            return ToscaContext.get(clazz, id);
                        }
                    });
            workflowBuilderService.initWorkflows(topologyContext);

            // update the topology in the edition context with the new one
            EditionContextManager.get().setTopology(parsingResult.getResult().getTopology());
        } catch (ParsingException e) {
            // Manage parsing error and dispatch them in the right editor exception
            throw new EditorToscaYamlUpdateException("The uploaded file to override the topology yaml is not a valid Tosca Yaml.");
        }
    }
}
