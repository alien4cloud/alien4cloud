package org.alien4cloud.tosca.editor.processors;

import javax.inject.Inject;

import org.alien4cloud.tosca.editor.operations.RecoverTopologyOperation;
import org.alien4cloud.tosca.editor.services.EditorTopologyRecoveryHelperService;
import org.alien4cloud.tosca.model.CSARDependency;
import org.alien4cloud.tosca.model.Csar;
import org.alien4cloud.tosca.model.templates.Topology;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.stereotype.Component;

import alien4cloud.paas.wf.WorkflowsBuilderService;
import alien4cloud.topology.TopologyService;
import alien4cloud.tosca.context.ToscaContext;
import alien4cloud.utils.AlienUtils;

/**
 * process {@link RecoverTopologyOperation}
 * It will recover the topology after a dependency have change.
 * This can lead to node / relationship deletion or rebuild.
 */
@Component
public class RecoverTopologyProcessor implements IEditorOperationProcessor<RecoverTopologyOperation> {

    @Inject
    private EditorTopologyRecoveryHelperService recoveryHelperService;
    @Inject
    private TopologyService topologyService;
    @Inject
    private WorkflowsBuilderService builderService;

    @Override
    public void process(Csar csar, Topology topology, RecoverTopologyOperation operation) {
        checkOperation(operation, topology);

        // Need to recover the workflow in the topo
        builderService.refreshTopologyWorkflows(builderService.buildTopologyContext(topology));

        // process every recovery operation
        // we need a new context here, as we want to have fresh types from elasticsearch
        recoveryHelperService.processRecoveryOperations(topology, operation.getRecoveringOperations());

        // make sure we also synch the dependencies and the caches types
        for (CSARDependency updatedDependency : AlienUtils.safe(operation.getUpdatedDependencies())) {
            ToscaContext.get().updateDependency(updatedDependency);
        }
        // TODO passing to this function the processRecoveryOperations ToscaContext should help reducing ES requests
        topologyService.rebuildDependencies(topology);
    }

    /**
     * If the operation is "empty", then try to fill it
     * 
     * @param operation
     * @param topology
     */
    private void checkOperation(RecoverTopologyOperation operation, Topology topology) {
        if (CollectionUtils.isEmpty(operation.getUpdatedDependencies())) {
            recoveryHelperService.buildRecoveryOperation(topology, operation);
        }
    }
}
