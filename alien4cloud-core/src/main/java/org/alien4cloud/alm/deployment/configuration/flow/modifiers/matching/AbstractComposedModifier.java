package org.alien4cloud.alm.deployment.configuration.flow.modifiers.matching;

import java.util.List;

import com.google.common.collect.Lists;

import org.alien4cloud.alm.deployment.configuration.flow.FlowExecutionContext;
import org.alien4cloud.alm.deployment.configuration.flow.ITopologyModifier;
import org.alien4cloud.tosca.model.templates.Topology;

import lombok.Getter;

/**
 * Some phases as node and policy matching are composed of multiple sub modifiers that performs the various steps of matching.
 *
 * This modifier is the entry point, and will execute all sub modifiers in the proper order.
 */
@Getter
public abstract class AbstractComposedModifier implements ITopologyModifier {
    private List<ITopologyModifier> subModifiers;

    public AbstractComposedModifier(ITopologyModifier... subModifiers) {
        this.subModifiers = Lists.newArrayList(subModifiers);
    }

    @Override
    public void process(Topology topology, FlowExecutionContext context) {
        subModifiers.forEach(modifier -> modifier.process(topology, context));
    }

    public void addModifierAfter(ITopologyModifier toAddModifier, ITopologyModifier existingModifier) {
        for (int i = 0; i < this.subModifiers.size(); i++) {
            if (this.subModifiers.get(i) == existingModifier) {
                this.subModifiers.add(i + 1, toAddModifier);
                return;
            }
        }

        throw new IllegalArgumentException("Unexpected exception in deployment flow to update node substitution; unable to find "
                + existingModifier.getClass().getSimpleName() + " modifier to inject selection action modifier.");
    }

    public void addModifierBefore(ITopologyModifier toAddModifier, ITopologyModifier existingModifier) {
        for (int i = 0; i < this.subModifiers.size(); i++) {
            if (this.subModifiers.get(i) == existingModifier) {
                this.subModifiers.add(i - 1, toAddModifier);
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
        for (int i = 0; i < this.subModifiers.size(); i++) {
            if (this.subModifiers.get(i) == lastModifierToKeep) {
                this.subModifiers.subList(i + 1, this.subModifiers.size()).clear();
                return;
            }
        }

        throw new IllegalArgumentException("Unexpected exception in deployment flow to update node substitution; unable to find "
                + lastModifierToKeep.getClass().getSimpleName() + " modifier that should be last of the modifiers list.");
    }
}
