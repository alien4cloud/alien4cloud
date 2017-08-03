package org.alien4cloud.alm.deployment.configuration.flow.modifiers.matching;

import java.util.List;

import org.alien4cloud.alm.deployment.configuration.flow.FlowExecutionContext;
import org.alien4cloud.alm.deployment.configuration.flow.ITopologyModifier;
import org.alien4cloud.tosca.model.templates.Topology;

import com.google.common.collect.Lists;

import lombok.Getter;

/**
 *
 * Node matching is composed of multiple sub modifiers that performs the various steps of matching.
 *
 * This modifier is the entry point, and will execute all sub modifiers in the proper order.
 *
 */
@Getter
public class NodeMatchingModifier implements ITopologyModifier {

    private List<ITopologyModifier> nodeMatchingModifiers;

    public NodeMatchingModifier(ITopologyModifier... nodeMatchingModifiers) {
        this.nodeMatchingModifiers = Lists.newArrayList(nodeMatchingModifiers);
    }

    @Override
    public void process(Topology topology, FlowExecutionContext context) {
        nodeMatchingModifiers.forEach(modifier -> modifier.process(topology, context));
    }

    public void addModifierAfter(ITopologyModifier toAddModifier, ITopologyModifier existingModifier) {

        for (int i = 0; i < this.nodeMatchingModifiers.size(); i++) {
            if (this.nodeMatchingModifiers.get(i) == existingModifier) {
                this.nodeMatchingModifiers.add(i + 1, toAddModifier);
                return;
            }
        }

        throw new IllegalArgumentException("Unexpected exception in deployment flow to update node substitution; unable to find "
                + existingModifier.getClass().getSimpleName() + " modifier to inject selection action modifier.");
    }

    /**
     * Remove all modifiers appearing after the one provided
     * 
     * @param lastModifierToKeep The last modifier to keep in the list. Everything else after it will be removed.
     */
    public void removeModifiersAfter(ITopologyModifier lastModifierToKeep) {
        for (int i = 0; i < this.nodeMatchingModifiers.size(); i++) {
            if (this.nodeMatchingModifiers.get(i) == lastModifierToKeep) {
                this.nodeMatchingModifiers.subList(i + 1, this.nodeMatchingModifiers.size()).clear();
                return;
            }
        }

        throw new IllegalArgumentException("Unexpected exception in deployment flow to update node substitution; unable to find "
                + lastModifierToKeep.getClass().getSimpleName() + " modifier that should be last of the modifiers list.");
    }
}
