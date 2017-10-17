package org.alien4cloud.alm.deployment.configuration.flow.modifiers.matching;

import org.alien4cloud.alm.deployment.configuration.flow.ITopologyModifier;

public class PolicyMatchingCompositeModifier extends AbstractComposedModifier {

    public PolicyMatchingCompositeModifier(ITopologyModifier... subModifiers) {
        super(subModifiers);
    }
}
