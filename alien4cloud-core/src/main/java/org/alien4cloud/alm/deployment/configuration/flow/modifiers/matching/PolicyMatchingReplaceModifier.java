package org.alien4cloud.alm.deployment.configuration.flow.modifiers.matching;

import org.alien4cloud.alm.deployment.configuration.flow.FlowExecutionContext;
import org.alien4cloud.alm.deployment.configuration.flow.ITopologyModifier;
import org.alien4cloud.tosca.model.templates.Topology;
import org.springframework.stereotype.Component;

/**
 * Last policy matching modifier, it actually inject policy modifiers implementations as policies implementations may impact the actual topology to be deployed.
 */
@Component
public class PolicyMatchingReplaceModifier implements ITopologyModifier {
    @Override
    public void process(Topology topology, FlowExecutionContext context) {
        // This modifier should inject policies implementation modifiers to the right phases.

        // If some policies have not been matched a warning is logged (policy support is considered as warning but does not prevent deployment).

    }
}