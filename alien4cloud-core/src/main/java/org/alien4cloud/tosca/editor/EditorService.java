package org.alien4cloud.tosca.editor;

import static alien4cloud.utils.FileUtil.isZipFile;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import javax.inject.Inject;

import alien4cloud.topology.*;
import org.alien4cloud.tosca.editor.exception.EditionConcurrencyException;
import org.alien4cloud.tosca.editor.exception.EditorIOException;
import org.alien4cloud.tosca.editor.exception.RecoverTopologyException;
import org.alien4cloud.tosca.editor.operations.AbstractEditorOperation;
import org.alien4cloud.tosca.editor.operations.RecoverTopologyOperation;
import org.alien4cloud.tosca.editor.operations.ResetTopologyOperation;
import org.alien4cloud.tosca.editor.processors.IEditorCommitableProcessor;
import org.alien4cloud.tosca.editor.processors.IEditorOperationProcessor;
import org.alien4cloud.tosca.editor.services.EditorTopologyRecoveryHelperService;
import org.alien4cloud.tosca.editor.services.EditorTopologyUploadService;
import org.alien4cloud.tosca.editor.services.TopologySubstitutionService;
import org.alien4cloud.tosca.exporter.ArchiveExportService;
import org.alien4cloud.tosca.model.Csar;
import org.alien4cloud.tosca.model.templates.Topology;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationContext;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.stereotype.Service;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

import alien4cloud.exception.NotFoundException;
import alien4cloud.git.SimpleGitHistoryEntry;
import alien4cloud.security.AuthorizationUtil;
import alien4cloud.utils.CollectionUtils;
import alien4cloud.utils.FileUtil;
import alien4cloud.utils.ReflectionUtil;

/**
 * This service manages command execution on the TOSCA topology template editor.
 */
@Service
public class EditorService {
    @Inject
    private ApplicationContext applicationContext;
    @Inject
    private ArchiveExportService exportService;
    @Inject
    private TopologyService topologyService;
    @Inject
    private TopologyServiceCore topologyServiceCore;
    @Inject
    private EditionContextManager editionContextManager;
    @Inject
    private TopologyDTOBuilder dtoBuilder;
    @Inject
    private EditorRepositoryService repositoryService;
    @Inject
    private EditorTopologyUploadService topologyUploadService;
    @Inject
    private EditorTopologyRecoveryHelperService recoveryHelperService;
    @Inject
    private TopologySubstitutionService topologySubstitutionServive;
    @Inject
    private TopologyValidationService topologyValidationService;

    @Value("${directories.alien}/${directories.upload_temp}")
    private String tempUploadDir;

    /** Processors map by type. */
    private Map<Class<?>, IEditorOperationProcessor<? extends AbstractEditorOperation>> processorMap = Maps.newHashMap();

    @PostConstruct
    public void initialize() {
        Map<String, IEditorOperationProcessor> processors = applicationContext.getBeansOfType(IEditorOperationProcessor.class);
        for (IEditorOperationProcessor processor : processors.values()) {
            Class<?> operationClass = ReflectionUtil.getGenericArgumentType(processor.getClass(), IEditorOperationProcessor.class, 0);
            processorMap.put(operationClass, processor);
        }
    }

    /**
     * Check the authorization in the context of a topology edition.
     * 
     * @param topologyId The id of the topology.
     */
    public void checkAuthorization(String topologyId) {
        try {
            editionContextManager.init(topologyId);
            topologyService.checkEditionAuthorizations(EditionContextManager.getTopology());
        } finally {
            editionContextManager.destroy();
        }
    }

    /**
     * Call this method only for checking optimistic locking and initializing edition context for method that don't process an operation (save, undo etc.)
     * 
     * @param topologyId The id of the topology under edition.
     * @param lastOperationId The id of the last operation.
     */
    private void initContext(String topologyId, String lastOperationId) {
        // create a fake operation for optimistic locking
        AbstractEditorOperation optimisticLockOperation = new AbstractEditorOperation() {
            @Override
            public String commitMessage() {
                return "This operation will never be enqueued and never commited.";
            }
        };
        optimisticLockOperation.setPreviousOperationId(lastOperationId);
        // init the edition context with the fake operation.
        initContext(topologyId, optimisticLockOperation);
    }

    /**
     * Initialize the edition context, (checks authorizations etc.)
     * 
     * @param topologyId The id of the topology under edition.
     * @param operation The operation to be processed.
     */
    private void initContext(String topologyId, AbstractEditorOperation operation) {
        editionContextManager.init(topologyId);
        // check authorization to update a topology
        topologyService.checkEditionAuthorizations(EditionContextManager.getTopology());
        // If the version of the topology is not snapshot we don't allow modifications.
        topologyService.throwsErrorIfReleased(EditionContextManager.getTopology());
        // check that operations can be executed (based on a kind of optimistic locking
        checkSynchronization(operation);
    }

    /**
     * Ensure that the request is synchronized with the current state of the edition.
     *
     * @param operation, The operation under evaluation.
     */
    private synchronized void checkSynchronization(AbstractEditorOperation operation) {
        // there is an operation being processed so just fail (nobody could get the notification)
        if (EditionContextManager.get().getCurrentOperation() != null) {
            throw new EditionConcurrencyException();
        }
        List<AbstractEditorOperation> operations = EditionContextManager.get().getOperations();
        // if someone performed some operations we have to ensure that the new operation is performed on top of a synchronized topology
        if (EditionContextManager.get().getLastOperationIndex() == -1) {
            if (operation.getPreviousOperationId() != null) {
                throw new EditionConcurrencyException();
            }
        } else if (!operations.get(EditionContextManager.get().getLastOperationIndex()).getId().equals(operation.getPreviousOperationId())) {
            throw new EditionConcurrencyException();
        }
        operation.setId(UUID.randomUUID().toString());
        EditionContextManager.get().setCurrentOperation(operation);
        return;
    }

    // trigger editor operation
    @MessageMapping("/topology-editor/{topologyId}")
    public <T extends AbstractEditorOperation> TopologyDTO execute(@DestinationVariable String topologyId, T operation) {
        // get the topology context.
        try {
            initContext(topologyId, operation);

            // check for topology potential recovery
            checkTopologyRecovery();

            doExecute(operation);

            // return the topology context
            return dtoBuilder.buildTopologyDTO(EditionContextManager.get());
        } finally {
            EditionContextManager.get().setCurrentOperation(null);
            editionContextManager.destroy();
        }
    }

    private <T extends AbstractEditorOperation> void doExecute(T operation) {
        operation.setAuthor(AuthorizationUtil.getCurrentUser().getUserId());

        // attach the topology tosca context and process the operation
        process(operation);

        List<AbstractEditorOperation> operations = EditionContextManager.get().getOperations();
        if (EditionContextManager.get().getLastOperationIndex() != operations.size() - 1) {
            // Clear the operations to 'redo'.
            CollectionUtils.clearFrom(operations, EditionContextManager.get().getLastOperationIndex() + 1);
        }

        // update the last operation and index
        EditionContextManager.get().getOperations().add(operation);
        EditionContextManager.get().setLastOperationIndex(EditionContextManager.get().getOperations().size() - 1);
    }

    /**
     * FIXME there is a cyclic dependency on beans here.
     * Finds the proper processor and process an operation
     *
     * @param operation The operation to process
     * @param <T> Type of the operation to process
     */
    public <T extends AbstractEditorOperation> void process(T operation) {
        IEditorOperationProcessor<T> processor = (IEditorOperationProcessor<T>) processorMap.get(operation.getClass());
        processor.process(operation);
    }

    /**
     * Undo or redo operations until the given index (including)
     * 
     * @param topologyId The id of the topology for which to undo or redo operations.
     * @param at The index on which to place the undo/redo cursor (-1 means no operations, then 0 is first operation etc.)
     * @param lastOperationId The last known operation id for client optimistic locking.
     * @return The topology DTO.
     */
    public TopologyDTO undoRedo(String topologyId, int at, String lastOperationId) {
        try {
            initContext(topologyId, lastOperationId);

            if (-1 > at || at > EditionContextManager.get().getOperations().size()) {
                throw new NotFoundException("Unable to find the requested index for undo/redo");
            }

            checkTopologyRecovery();

            if (at == EditionContextManager.get().getLastOperationIndex()) {
                // nothing to change.
                return dtoBuilder.buildTopologyDTO(EditionContextManager.get());
            }

            // TODO Improve this by avoiding dao query for (deep) cloning topology and keeping cache for TOSCA types that are required.
            editionContextManager.reset();

            for (int i = 0; i < at + 1; i++) {
                AbstractEditorOperation operation = EditionContextManager.get().getOperations().get(i);
                IEditorOperationProcessor processor = processorMap.get(operation.getClass());
                processor.process(operation);
            }

            EditionContextManager.get().setLastOperationIndex(at);

            return dtoBuilder.buildTopologyDTO(EditionContextManager.get());
        } catch (IOException e) {
            // FIXME undo should be fail-safe...
            return null;
        } finally {
            EditionContextManager.get().setCurrentOperation(null);
            editionContextManager.destroy();
        }
    }

    /**
     * Save a topology under edition. It updates the local repository files, the topology in elastic-search and perform a local git commit.
     * 
     * @param topologyId The id of the topology under edition.
     * @param lastOperationId The id of the last operation.
     */
    public TopologyDTO save(String topologyId, String lastOperationId) {
        try {
            initContext(topologyId, lastOperationId);

            doSave();

            return dtoBuilder.buildTopologyDTO(EditionContextManager.get());
        } catch (IOException e) {
            // when there is a failure in file copy to the local repo.
            // FIXME git revert to put back the local files state in the initial state.
            throw new EditorIOException("Error while saving files state in local repository", e);
        } finally {
            EditionContextManager.get().setCurrentOperation(null);
            editionContextManager.destroy();
        }
    }

    private void doSave() throws IOException {
        EditionContext context = EditionContextManager.get();
        if (context.getLastOperationIndex() <= context.getLastSavedOperationIndex()) {
            // nothing to save..
            return;
        }

        StringBuilder commitMessage = new StringBuilder();
        // copy and cleanup all temporary files from the executed operations.
        for (int i = context.getLastSavedOperationIndex() + 1; i <= context.getLastOperationIndex(); i++) {
            AbstractEditorOperation operation = context.getOperations().get(i);
            IEditorOperationProcessor<?> processor = (IEditorOperationProcessor) processorMap.get(operation.getClass());
            if (processor instanceof IEditorCommitableProcessor) {
                ((IEditorCommitableProcessor) processor).beforeCommit(operation);
            }
            commitMessage.append(operation.getAuthor()).append(": ").append(operation.commitMessage()).append("\n");
        }

        saveYamlFile();

        Topology topology = EditionContextManager.getTopology();
        // Save the topology in elastic search
        topologyServiceCore.save(topology);
        topologySubstitutionServive.updateSubstitutionType(topology, EditionContextManager.getCsar());

        // Local git commit
        repositoryService.commit(EditionContextManager.get().getCsar(), commitMessage.toString());

        // TODO add support for undo even after save, this require ability to rollback files to git state, we need file rollback support for that..
        context.setOperations(Lists.newArrayList(context.getOperations().subList(context.getLastOperationIndex() + 1, context.getOperations().size())));
        context.setLastOperationIndex(-1);
    }

    private void saveYamlFile() throws IOException {
        Csar csar = EditionContextManager.getCsar();
        Path targetPath = EditionContextManager.get().getLocalGitPath().resolve(csar.getYamlFilePath());
        String yaml = exportService.getYaml(csar, EditionContextManager.getTopology());
        try (BufferedWriter writer = Files.newBufferedWriter(targetPath)) {
            writer.write(yaml);
        }
    }

    /**
     * Performs a git pull.
     */
    public void pull(String topologyId, String username, String password, String remoteBranch) {
        try {
            editionContextManager.init(topologyId);
            repositoryService.pull(EditionContextManager.getCsar(), username, password, remoteBranch);
        } finally {
            editionContextManager.destroy();
        }
    }

    /**
     * Push the content to a remote git repository.
     * Note that conflicts are not managed in a4c. In case of conflicts a new branch is created for manual merge by users.
     */
    public void push(String topologyId, String username, String password, String remoteBranch) {
        try {
            editionContextManager.init(topologyId);
            repositoryService.push(EditionContextManager.getCsar(), username, password, remoteBranch);
        } finally {
            editionContextManager.destroy();
        }
    }

    /**
     * Configure the remote url of the git repository.
     *
     * @param remoteName The name for the repository.
     * @param remoteUrl The url of the repository.
     */
    public void setRemote(String topologyId, String remoteName, String remoteUrl) {
        try {
            editionContextManager.init(topologyId);
            Csar csar = EditionContextManager.getCsar();
            repositoryService.setRemote(csar, remoteName, remoteUrl);
        } finally {
            editionContextManager.destroy();
        }
    }

    /**
     * Retrieve the repository url of the git.
     *
     * @param topologyId the id of the topology.
     * @param remoteName The name of the remote.
     * @return The url corresponding to the remote name of the git repository of the topology.
     */
    public String getRemoteUrl(String topologyId, String remoteName) {
        try {
            editionContextManager.init(topologyId);
            Csar csar = EditionContextManager.getCsar();
            return repositoryService.getRemoteUrl(csar, remoteName);
        } finally {
            editionContextManager.destroy();
        }
    }

    /**
     * Retrieve simplified vision of the git history for the given topology.
     * 
     * @param topologyId The id of the topology.
     * @param from from which index to get history.
     * @param count number of histories entry to retrieve.
     * @return a list of simplified git commit entry.
     */
    public List<SimpleGitHistoryEntry> history(String topologyId, int from, int count) {
        try { // No need to check current operation, we just want to get git history.
            editionContextManager.init(topologyId);
            // check authorization to update a topology
            topologyService.checkEditionAuthorizations(EditionContextManager.getTopology());

            return repositoryService.getHistory(EditionContextManager.getCsar(), from, count);
        } finally {
            editionContextManager.destroy();
        }
    }

    /**
     * Override the content of an archive from a full exising archive.
     * 
     * @param topologyId The if of the topology to process.
     * @param inputStream The input stream of the file that contains the archive.
     */
    public void override(String topologyId, InputStream inputStream) throws IOException {
        Path tempPath = null;
        try {
            // Initialize the editon context, null last operation id means that we just accept a context with no pending operations
            initContext(topologyId, (String) null);

            // first we need to copy the content to a temporary location, unzip and parse the archive
            tempPath = Files.createTempFile(tempUploadDir, null, null);
            Files.copy(inputStream, tempPath, StandardCopyOption.REPLACE_EXISTING);
            // This throws an exception if not successful
            topologyUploadService.processTopology(tempPath, EditionContextManager.get().getTopology().getWorkspace());

            // meaning the topology is well imported in the editor context: override all the content of the git repository
            // erase all content but .git directory
            FileUtil.delete(EditionContextManager.get().getLocalGitPath(), EditionContextManager.get().getLocalGitPath().resolve(".git"));
            // copy the archive content
            if (isZipFile(tempPath)) {
                // unzip the content
                FileUtil.unzip(tempPath, EditionContextManager.get().getLocalGitPath());
            } else {
                // just copy the file
                Path targetPath = EditionContextManager.get().getLocalGitPath().resolve(tempPath.getFileName());
                Files.copy(tempPath, targetPath, StandardCopyOption.REPLACE_EXISTING);
            }

            // and finally save and commit
            Topology topology = EditionContextManager.getTopology();
            String commitMessage = AuthorizationUtil.getCurrentUser().getUserId() + ": Override all content of the topology archive from REST API.";
            topologyServiceCore.save(topology);
            topologySubstitutionServive.updateSubstitutionType(topology, EditionContextManager.getCsar());

            // Local git commit
            repositoryService.commit(EditionContextManager.get().getCsar(), commitMessage);
        } finally {
            EditionContextManager.get().setCurrentOperation(null);
            editionContextManager.destroy();
        }
    }

    /**
     * Checks if the topology needs to be recovered and eventually throws an error.
     * The {@link RecoverTopologyOperation} is cache for later use in recovering process
     */
    public void checkTopologyRecovery() {
        Topology topology = EditionContextManager.getTopology();
        EditionContext context = EditionContextManager.get();
        context.setRecoveryOperation(recoveryHelperService.buildRecoveryOperation(topology));
        if (context.getRecoveryOperation() != null) {
            throw new RecoverTopologyException("The topology needs to be recovered.", context.getRecoveryOperation());
        }
    }

    /**
     * Execute an operation and directly trigger the save process
     *
     * @param topologyId The id of the topology.
     * @param operation The operation to execute.
     * @param <T>
     * @return a {@link TopologyDTO}
     */
    private <T extends AbstractEditorOperation> TopologyDTO executeAndSave(String topologyId, T operation) {
        try {
            // init the context.
            initContext(topologyId, operation);
            // execute the operation
            doExecute(operation);
            // save the context
            doSave();
            // return the topology DTO
            return dtoBuilder.buildTopologyDTO(EditionContextManager.get());
        } catch (IOException e) {
            // when there is a failure in file copy to the local repo.
            // FIXME git revert to put back the local files state in the initial state.
            throw new EditorIOException("Error while saving files state in local repository", e);
        } finally {
            EditionContextManager.get().setCurrentOperation(null);
            editionContextManager.destroy();
        }

    }

    /**
     * Recovers a topology
     *
     * @param topologyId The id of the topology.
     * @param lastOperationId
     * @return
     */
    public TopologyDTO recover(String topologyId, String lastOperationId) {
        // The recovering process is done via operation so that we can have it in the history
        RecoverTopologyOperation operation = getRecoverTopologyOperation(topologyId);
        operation.setPreviousOperationId(lastOperationId);
        return executeAndSave(topologyId, operation);
    }

    private RecoverTopologyOperation getRecoverTopologyOperation(String topologyId) {
        try {
            editionContextManager.init(topologyId);
            RecoverTopologyOperation operation = EditionContextManager.get().getRecoveryOperation();
            EditionContextManager.get().setRecoveryOperation(null);
            return operation != null ? operation : new RecoverTopologyOperation();
        } finally {
            editionContextManager.destroy();
        }
    }

    /**
     * Reset and save a topology
     *
     * @param topologyId The id of the topology.
     * @param lastOperationId
     * @return
     */
    public TopologyDTO reset(String topologyId, String lastOperationId) {
        // The resetting process is done via operation so that we can have it in the history
        ResetTopologyOperation operation = new ResetTopologyOperation();
        operation.setPreviousOperationId(lastOperationId);
        return executeAndSave(topologyId, operation);
    }

    /**
     * Validate if a topology is valid.
     * @param topologyId The id of the topology.
     * @return the validation result
     */
    public TopologyValidationResult validateTopology(String topologyId) {
        try {
            editionContextManager.init(topologyId);
            return topologyValidationService.validateTopology(EditionContextManager.getTopology());
        } finally {
            editionContextManager.destroy();
        }
    }
}