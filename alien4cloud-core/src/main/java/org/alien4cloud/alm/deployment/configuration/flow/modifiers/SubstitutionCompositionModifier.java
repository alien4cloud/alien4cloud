package org.alien4cloud.alm.deployment.configuration.flow.modifiers;

import javax.annotation.Resource;

import org.alien4cloud.alm.deployment.configuration.flow.FlowExecutionContext;
import org.alien4cloud.alm.deployment.configuration.flow.ITopologyModifier;
import org.alien4cloud.tosca.model.templates.Topology;

import alien4cloud.application.TopologyCompositionService;
import org.springframework.stereotype.Component;

/**
 * This modifier process substitution nodes to actually create the expected topology.
 */
@Component
public class SubstitutionCompositionModifier implements ITopologyModifier {
    @Resource
    private TopologyCompositionService topologyCompositionService;

    @Override
    public void process(Topology topology, FlowExecutionContext context) {
        topologyCompositionService.processTopologyComposition(topology);
    }
}