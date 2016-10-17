package org.alien4cloud.tosca.editor.processors;

import java.util.Iterator;
import java.util.List;
import java.util.Set;

import javax.inject.Inject;

import org.alien4cloud.tosca.catalog.index.ICsarService;
import org.alien4cloud.tosca.editor.EditionContextManager;
import org.alien4cloud.tosca.editor.operations.AbstractEditorOperation;
import org.alien4cloud.tosca.editor.operations.ChangeDependencyVersionOperation;
import org.alien4cloud.tosca.editor.services.EditorTopologyRecoveryHelperService;
import org.alien4cloud.tosca.model.CSARDependency;
import org.alien4cloud.tosca.model.templates.Topology;
import org.springframework.stereotype.Component;

import com.google.common.collect.Sets;

import alien4cloud.topology.TopologyService;
import alien4cloud.tosca.context.ToscaContext;

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
    @Inject
    private ICsarService csarService;

    @Override
    public void process(ChangeDependencyVersionOperation operation) {
        Topology topology = EditionContextManager.getTopology();

        Set<CSARDependency> topologyDependencies = Sets.newHashSet(topology.getDependencies());
        Iterator<CSARDependency> topologyDependencyIterator = topologyDependencies.iterator();
        while (topologyDependencyIterator.hasNext()) {
            CSARDependency dependency = topologyDependencyIterator.next();
            if (dependency.getName().equals(operation.getDependencyName())) {
                topologyDependencyIterator.remove();
            }
        }
        CSARDependency newDependency = new CSARDependency(operation.getDependencyName(), operation.getDependencyVersion());
        topologyDependencies.add(newDependency);
        topology.setDependencies(topologyDependencies);

        // make sure we also synch the dependencies and the caches types
        ToscaContext.get().updateDependency(newDependency);

        Set<CSARDependency> dependencies = Sets.newHashSet(newDependency);
        List<AbstractEditorOperation> recoveringOperations = recoveryHelperService.buildRecoveryOperations(topology, dependencies);

        // process every recovery operation
        recoveryHelperService.processRecoveryOperations(topology, recoveringOperations);

        // TODO passing to this function the processRecoveryOperations ToscaContext should help reducing ES requests
        topologyService.rebuildDependencies(topology);

        csarService.setDependencies(topology.getId(), topology.getDependencies());
    }

}
