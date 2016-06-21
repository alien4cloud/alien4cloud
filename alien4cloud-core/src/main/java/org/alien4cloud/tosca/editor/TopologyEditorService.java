package org.alien4cloud.tosca.editor;

import java.io.InputStream;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;

import org.alien4cloud.tosca.editor.model.EditionConcurrencyException;
import org.alien4cloud.tosca.editor.operations.AbstractEditorOperation;
import org.alien4cloud.tosca.editor.processors.IEditorOperationProcessor;
import org.springframework.context.ApplicationContext;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.stereotype.Controller;

import com.google.common.collect.Maps;

import alien4cloud.security.AuthorizationUtil;
import alien4cloud.topology.TopologyService;
import alien4cloud.utils.ReflectionUtil;

/**
 * This service manages command execution on the TOSCA topology template editor.
 */
@Controller
public class TopologyEditorService {
    @Resource
    private ApplicationContext applicationContext;
    @Resource
    private TopologyService topologyService;
    @Resource
    private TopologyEditionContextManager topologyEditionContextManager;

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

    // trigger editor operation
    @MessageMapping("/topology-editor/{topologyId}")
    public <T extends AbstractEditorOperation> TopologyEditionContext execute(@DestinationVariable String topologyId, T operation)
            throws EditionConcurrencyException {
        // get the topology context.
        try {
            topologyEditionContextManager.init(topologyId);

            operation.setAuthor(AuthorizationUtil.getCurrentUser().getUserId());
            // check authorization to update a topology
            topologyService.checkEditionAuthorizations(TopologyEditionContextManager.getTopology());

            // If the version of the topology is not snapshot we don't allow modifications.
            topologyService.throwsErrorIfReleased(TopologyEditionContextManager.getTopology());
            // check that operations can be executed (based on a kind of optimistic locking
            checkSynchronization(operation);

            // attach the topology tosca context and process the operation
            IEditorOperationProcessor<T> processor = (IEditorOperationProcessor<T>) processorMap.get(operation.getClass());
            processor.process(operation);

            // return the topology context
            return TopologyEditionContextManager.get();
        } finally {
            topologyEditionContextManager.destroy();
        }
    }

    /**
     * Ensure that the request is synchronized with the current state of the edition.
     *
     * @param operation, The operation under evaluation.
     */
    private synchronized void checkSynchronization(AbstractEditorOperation operation) throws EditionConcurrencyException {
        List<AbstractEditorOperation> operations = TopologyEditionContextManager.get().getOperations();
        // if someone performed some operations we have to ensure that the new operation is performed on top of a synchronized topology
        if (operations.size() == 0 || operation.getPreviousOperationId() == operations.get(operations.size() - 1).getId()) {
            operation.setId(UUID.randomUUID().toString());
            operations.add(operation);
            return;
        }
        // throw an edition concurrency exception
        throw new EditionConcurrencyException();
    }

    public void undoRedo(String topologyId, int at, String lastOperationId) {
        // TODO check that the requested index (at) is in the operation range.
        // TODO Re-initialize the Type Loader from the topology context
        // TODO Replay all operations until the given index
    }

    // save
    public void save() {
        // save is updating the topology on elasticsearch and also performs a local commit
        // TODO implements save
        // topologyServiceCore.save(topology);
        // topologyServiceCore.updateSubstitutionType(topology);
        // TODO Copy and cleanup all temporary files from the executed operations.
        // TODO GIT COMMIT
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
     * Performs a git push.
     */
    public void push() {

    }

    /**
     * Upload a file in the archive.
     *
     * Note, if the updated file is the YAML we have to process it fully.
     * 
     * @param
     */
    public void uploadFile(String topologyId, String path, InputStream fileStream) {

    }

}