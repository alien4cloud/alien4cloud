package org.alien4cloud.tosca.editor.processors;

import alien4cloud.model.components.CSARDependency;
import alien4cloud.model.topology.Topology;
import alien4cloud.topology.TopologyService;
import alien4cloud.tosca.context.ToscaContext;
import org.alien4cloud.tosca.editor.EditionContextManager;
import org.alien4cloud.tosca.editor.EditorTopologyRecoveryHelperService;
import org.alien4cloud.tosca.editor.operations.RecoverTopologyOperation;
import org.springframework.stereotype.Component;

import javax.inject.Inject;
import java.util.Set;

/**
 * process {@link RecoverTopologyOperation}
 * It will recover the topology after a dependency have change.
 * This can lead to node / relationship deletion or rebuild.
 */
@Component
public class RecoverTopologyProcessor implements IEditorOperationProcessor<RecoverTopologyOperation> {

    @Inject
    private EditorTopologyRecoveryHelperService editorTopologyRecoveryHelperService;
    @Inject
    private TopologyService topologyService;

    @Override
    public void process(RecoverTopologyOperation operation) {
        Topology topology = EditionContextManager.getTopology();
        // FIXME cache the updated dependencies / recovery operations and reuse it here
        Set<CSARDependency> updatedDependencies = editorTopologyRecoveryHelperService.getUpdatedDependencies(topology);
        operation.setRecoveringOperations(editorTopologyRecoveryHelperService.buildRecoveryOperations(topology, updatedDependencies));

        // process every recovery operation
        // we need a new context here, as we want to have fresh types from elasticsearch
        editorTopologyRecoveryHelperService.processRecoveryOperations(topology, operation.getRecoveringOperations());

        // make sure we also synch the dependencies and the caches types
        for (CSARDependency updatedDependency : updatedDependencies) {
            ToscaContext.get().updateDependency(updatedDependency);
        }

        // FIXME passing to this function the processRecoveryOperations ToscaContext should help reducing ES requests
        topologyService.rebuildDependencies(topology);
    }
}
