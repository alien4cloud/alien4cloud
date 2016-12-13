package org.alien4cloud.tosca.editor.processors;

import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import javax.inject.Inject;

import org.alien4cloud.tosca.editor.EditionContextManager;
import org.alien4cloud.tosca.editor.operations.AbstractEditorOperation;
import org.alien4cloud.tosca.editor.operations.ChangeDependencyVersionOperation;
import org.alien4cloud.tosca.editor.services.EditorTopologyRecoveryHelperService;
import org.alien4cloud.tosca.model.CSARDependency;
import org.alien4cloud.tosca.model.templates.Topology;
import org.springframework.stereotype.Component;

import com.google.common.collect.Sets;

import alien4cloud.exception.NotFoundException;
import alien4cloud.exception.VersionConflictException;
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

    @Override
    public void process(ChangeDependencyVersionOperation operation) {
        Topology topology = EditionContextManager.getTopology();
        CSARDependency newDependency = new CSARDependency(operation.getDependencyName(), operation.getDependencyVersion());

        // FIXME remove transitives also, then add it later
        Set<CSARDependency> topologyDependencies = Sets.newHashSet(topology.getDependencies());
        Iterator<CSARDependency> topologyDependencyIterator = topologyDependencies.iterator();
        Optional<CSARDependency> oldDependency = Optional.empty();
        while (topologyDependencyIterator.hasNext()) {
            CSARDependency dependency = topologyDependencyIterator.next();
            if (dependency.getName().equals(operation.getDependencyName())) {
                oldDependency = Optional.of(dependency);
                topologyDependencyIterator.remove();
            }
        }

        // make sure we sync the dependencies and the cached types
        ToscaContext.get().updateDependency(newDependency);

        try {
            topologyService.checkForMissingTypes(EditionContextManager.get());
        } catch (NotFoundException e) {
            // Revert changes made to ToscaContext then throw.
            if (oldDependency.isPresent()) ToscaContext.get().updateDependency(oldDependency.get());
            else ToscaContext.get().removeDependency(newDependency);
            throw new VersionConflictException("Changing the dependency ["+ newDependency.getName() + "] to version ["
                    + newDependency.getVersion() + "] induces missing types in the topology. Not found : [" + e.getMessage() + "].", e);
        }

        // FIXME add transitives also, if removed before
        topologyDependencies.add(newDependency);
        topology.setDependencies(topologyDependencies);

        Set<CSARDependency> dependencies = Sets.newHashSet(newDependency);
        List<AbstractEditorOperation> recoveringOperations = recoveryHelperService.buildRecoveryOperations(topology, dependencies);

        // process every recovery operation
        recoveryHelperService.processRecoveryOperations(topology, recoveringOperations);

        // TODO passing to this function the processRecoveryOperations ToscaContext should help reducing ES requests
        topologyService.rebuildDependencies(topology);
    }

}
