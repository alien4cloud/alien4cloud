package org.alien4cloud.alm.deployment.configuration.flow.modifiers;

import org.alien4cloud.alm.deployment.configuration.flow.FlowExecutionContext;
import org.alien4cloud.alm.deployment.configuration.flow.ITopologyModifier;
import org.alien4cloud.tosca.model.templates.Topology;
import org.springframework.stereotype.Component;

import lombok.extern.slf4j.Slf4j;

/**
 * Policy matching modifier performs.
 */
@Slf4j
@Component
public class PolicyMatchingModifier implements ITopologyModifier {
    @Override
    public void process(Topology topology, FlowExecutionContext context) {
        log.debug("Policy matching is not yet implemented.");
    }
}