package org.alien4cloud.tosca.editor.processors;

import javax.inject.Inject;

import org.alien4cloud.tosca.editor.EditionContextManager;
import org.alien4cloud.tosca.editor.operations.ChangeDependencyVersionOperation;
import org.alien4cloud.tosca.editor.services.EditorTopologyRecoveryHelperService;
import org.alien4cloud.tosca.model.CSARDependency;
import org.alien4cloud.tosca.model.Csar;
import org.alien4cloud.tosca.model.templates.Topology;
import org.springframework.stereotype.Component;

import alien4cloud.topology.TopologyService;

/**
 * Process {@link ChangeDependencyVersionOperation}.
 * It will recover the topology after a dependency have change.
 * This can lead to node / relationship deletion or rebuild.
 */
@Component
public class ChangeDependencyVersionProcessor implements IEditorOperationProcessor<ChangeDependencyVersionOperation> {

    @Inject
    private EditorTopologyRecoveryHelperService recoveryHelperService;
    @Inject
    private TopologyService topologyService;

    @Override
    public void process(Csar csar, Topology topology, ChangeDependencyVersionOperation operation) {
        CSARDependency newDependency = new CSARDependency(operation.getDependencyName(), operation.getDependencyVersion());

        // Check for missing type and update the topology's dependencies
        topologyService.updateDependencies(EditionContextManager.get(), newDependency);

        topologyService.recover(topology, newDependency);
    }

}
