package org.alien4cloud.tosca.editor;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import javax.inject.Inject;

import org.alien4cloud.tosca.editor.model.EditionConcurrencyException;
import org.alien4cloud.tosca.editor.operations.AbstractEditorOperation;
import org.alien4cloud.tosca.editor.processors.IEditorOperationProcessor;
import org.springframework.context.ApplicationContext;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.stereotype.Controller;

import com.google.common.collect.Maps;

import alien4cloud.exception.NotFoundException;
import alien4cloud.security.AuthorizationUtil;
import alien4cloud.topology.TopologyDTO;
import alien4cloud.topology.TopologyService;
import alien4cloud.utils.CollectionUtils;
import alien4cloud.utils.ReflectionUtil;

/**
 * This service manages command execution on the TOSCA topology template editor.
 */
@Controller
public class EditorService {
    @Resource
    private ApplicationContext applicationContext;
    @Resource
    private TopologyService topologyService;
    @Resource
    private EditionContextManager topologyEditionContextManager;
    @Inject
    private TopologyDTOBuilder dtoBuilder;

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
            topologyEditionContextManager.init(topologyId);
            topologyService.checkEditionAuthorizations(EditionContextManager.getTopology());
        } finally {
            topologyEditionContextManager.destroy();
        }
    }

    private void initContext(String topologyId, AbstractEditorOperation operation) {
        topologyEditionContextManager.init(topologyId);
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
        if (((operations.size() == 0 || EditionContextManager.get().getLastOperationIndex() == -1) && operation.getPreviousOperationId() == null)
                || operation.getPreviousOperationId().equals(operations.get(EditionContextManager.get().getLastOperationIndex()).getId())) {
            operation.setId(UUID.randomUUID().toString());
            EditionContextManager.get().setCurrentOperation(operation);
            return;
        }
        // throw an edition concurrency exception
        throw new EditionConcurrencyException();
    }

    // trigger editor operation
    @MessageMapping("/topology-editor/{topologyId}")
    public <T extends AbstractEditorOperation> TopologyDTO execute(@DestinationVariable String topologyId, T operation) {
        // get the topology context.
        try {
            initContext(topologyId, operation);
            operation.setAuthor(AuthorizationUtil.getCurrentUser().getUserId());

            // attach the topology tosca context and process the operation
            IEditorOperationProcessor<T> processor = (IEditorOperationProcessor<T>) processorMap.get(operation.getClass());
            processor.process(operation);

            List<AbstractEditorOperation> operations = EditionContextManager.get().getOperations();
            if (EditionContextManager.get().getLastOperationIndex() == operations.size() - 1) {
                // Clear the operations to 'redo'.
                CollectionUtils.clearFrom(operations, EditionContextManager.get().getLastOperationIndex() + 1);
            }
            // update the last operation and index
            EditionContextManager.get().getOperations().add(operation);
            EditionContextManager.get().setLastOperationIndex(EditionContextManager.get().getOperations().size() - 1);

            // return the topology context
            return dtoBuilder.buildTopologyDTO(EditionContextManager.get());
        } finally {
            EditionContextManager.get().setCurrentOperation(null);
            topologyEditionContextManager.destroy();
        }
    }

    /**
     * Undo or redo operations until the given index (including)
     * 
     * @param topologyId The id of the topology for which to undo or redo operations.
     * @param at The index on which to place the undo/redo cursor (0 means no operations, then 1 is first operation etc.)
     * @param lastOperationId The last known operation id for client optimistic locking.
     * @return The topology DTO.
     */
    public TopologyDTO undoRedo(String topologyId, int at, String lastOperationId) {
        try {
            // create a fake undo operation just to check the last operation id
            AbstractEditorOperation undoOperation = new AbstractEditorOperation() {
            };
            undoOperation.setPreviousOperationId(lastOperationId);
            initContext(topologyId, undoOperation);

            if (0 > at || at > EditionContextManager.get().getOperations().size()) {
                throw new NotFoundException("Unable to find the requested index for undo/redo");
            }

            if ((at + 1) == EditionContextManager.get().getLastOperationIndex()) {
                // nothing to change.
                return dtoBuilder.buildTopologyDTO(EditionContextManager.get());
            }

            // TODO Improve this by avoiding dao query for (deep) cloning topology and keeping cache for TOSCA types that are required.
            topologyEditionContextManager.reset();

            for (int i = 0; i < at; i++) {
                AbstractEditorOperation operation = EditionContextManager.get().getOperations().get(i);
                IEditorOperationProcessor processor = processorMap.get(operation.getClass());
                processor.process(operation);
            }

            EditionContextManager.get().setLastOperationIndex(at - 1);

            return dtoBuilder.buildTopologyDTO(EditionContextManager.get());
        } catch (IOException e) {
            // FIXME undo should be fail-safe...
            return null;
        } finally {
            EditionContextManager.get().setCurrentOperation(null);
            topologyEditionContextManager.destroy();
        }
    }

    // save
    public void save() {
        // save is updating the topology on elasticsearch and also performs a local commit
        // TODO implements save
        // topologyServiceCore.save(topology);
        // topologyServiceCore.updateSubstitutionType(topology);
        // TODO Copy and cleanup all temporary files from the executed operations.
        // TODO LOCAL GIT COMMIT
    }

    /**
     * Performs a git pull.
     */
    public void pull() {
        // pull can be done only if there is no unsaved commands

        // This operation just fails in case of conflicts

        // The topology is updated

    }

    /**
     * Push the content to a remote git repository.
     *
     * Note that conflicts are not managed in a4c. In case of conflicts a new branch is created for manual merge by users.
     */
    public void push() {

    }

    public void history() {
        // repository.log().setMaxCount(10).call().iterator()
    }

}