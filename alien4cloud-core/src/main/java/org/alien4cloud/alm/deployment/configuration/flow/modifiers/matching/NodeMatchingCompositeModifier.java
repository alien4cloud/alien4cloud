package org.alien4cloud.alm.deployment.configuration.flow.modifiers.matching;

import org.alien4cloud.alm.deployment.configuration.flow.ITopologyModifier;

public class NodeMatchingCompositeModifier extends AbstractComposedModifier {

    public NodeMatchingCompositeModifier(ITopologyModifier... subModifiers) {
        super(subModifiers);
    }
}
