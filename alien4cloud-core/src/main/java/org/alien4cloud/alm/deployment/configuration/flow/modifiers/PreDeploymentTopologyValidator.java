package org.alien4cloud.alm.deployment.configuration.flow.modifiers;

import static alien4cloud.utils.AlienUtils.safe;

import javax.inject.Inject;

import org.alien4cloud.alm.deployment.configuration.flow.FlowExecutionContext;
import org.alien4cloud.alm.deployment.configuration.flow.ITopologyModifier;
import org.alien4cloud.alm.deployment.configuration.model.DeploymentMatchingConfiguration;
import org.alien4cloud.alm.deployment.configuration.model.OrchestratorDeploymentProperties;
import org.alien4cloud.tosca.model.templates.Topology;
import org.springframework.stereotype.Component;

import alien4cloud.deployment.DeploymentTopologyValidationService;
import alien4cloud.exception.NotFoundException;
import alien4cloud.topology.TopologyValidationResult;
import alien4cloud.topology.task.AbstractTask;

/**
 * Perform validation of the topology before deployment.
 * This is the latest of the topology modifiers.
 */
@Component
public class PreDeploymentTopologyValidator implements ITopologyModifier {
    @Inject
    private DeploymentTopologyValidationService deploymentTopologyValidationService;

    @Override
    public void process(Topology topology, FlowExecutionContext context) {
        DeploymentMatchingConfiguration matchingConfiguration = context
                .getConfiguration(DeploymentMatchingConfiguration.class, PreDeploymentTopologyValidator.class.getSimpleName())
                .orElseThrow(() -> new NotFoundException("Failed to find deployment configuration for pre-deployment validation."));
        OrchestratorDeploymentProperties orchestratorDeploymentProperties = context
                .getConfiguration(OrchestratorDeploymentProperties.class, PreDeploymentTopologyValidator.class.getSimpleName())
                .orElse(new OrchestratorDeploymentProperties(matchingConfiguration.getVersionId(), matchingConfiguration.getEnvironmentId(),
                        matchingConfiguration.getOrchestratorId()));
        TopologyValidationResult validationResult = deploymentTopologyValidationService.validateProcessedDeploymentTopology(topology, matchingConfiguration,
                orchestratorDeploymentProperties);

        for (AbstractTask task : safe(validationResult.getTaskList())) {
            context.log().error(task);
        }

        for (AbstractTask task : safe(validationResult.getInfoList())) {
            context.log().info(task);
        }

        for (AbstractTask task : safe(validationResult.getWarningList())) {
            context.log().warn(task);
        }

    }
}
