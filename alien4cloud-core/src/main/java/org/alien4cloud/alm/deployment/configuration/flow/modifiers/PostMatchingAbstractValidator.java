package org.alien4cloud.alm.deployment.configuration.flow.modifiers;

import static alien4cloud.utils.AlienUtils.safe;

import javax.inject.Inject;

import org.alien4cloud.alm.deployment.configuration.flow.FlowExecutionContext;
import org.alien4cloud.alm.deployment.configuration.flow.ITopologyModifier;
import org.alien4cloud.alm.deployment.configuration.model.DeploymentMatchingConfiguration;
import org.alien4cloud.tosca.model.templates.Topology;
import org.springframework.stereotype.Component;

import alien4cloud.exception.NotFoundException;
import alien4cloud.topology.task.AbstractTask;
import alien4cloud.topology.validation.TopologyAbstractNodeValidationService;

/**
 * Performs validation that no abstract nodes are left in the topology.
 */
@Component
public class PostMatchingAbstractValidator implements ITopologyModifier {
    @Inject
    private TopologyAbstractNodeValidationService topologyAbstractNodeValidationService;

    @Override
    public void process(Topology topology, FlowExecutionContext context) {
        DeploymentMatchingConfiguration matchingConfiguration = context
                .getConfiguration(DeploymentMatchingConfiguration.class, PreDeploymentTopologyValidator.class.getSimpleName())
                .orElseThrow(() -> new NotFoundException("Failed to find deployment configuration for pre-deployment validation."));

        for (AbstractTask task : safe(
                topologyAbstractNodeValidationService.findReplacementForAbstracts(topology, matchingConfiguration.getMatchedLocationResources()))) {
            context.log().error(task);
        }
    }
}
