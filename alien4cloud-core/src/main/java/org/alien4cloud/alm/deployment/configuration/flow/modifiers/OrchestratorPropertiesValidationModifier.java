package org.alien4cloud.alm.deployment.configuration.flow.modifiers;

import java.util.Optional;

import javax.inject.Inject;

import org.alien4cloud.alm.deployment.configuration.flow.FlowExecutionContext;
import org.alien4cloud.alm.deployment.configuration.flow.ITopologyModifier;
import org.alien4cloud.alm.deployment.configuration.model.DeploymentMatchingConfiguration;
import org.alien4cloud.alm.deployment.configuration.model.OrchestratorDeploymentProperties;
import org.alien4cloud.tosca.model.templates.Topology;
import org.springframework.stereotype.Component;

import alien4cloud.deployment.OrchestratorPropertiesValidationService;
import alien4cloud.model.application.ApplicationEnvironment;
import alien4cloud.topology.task.LocationPolicyTask;

/**
 * Performs validations of orchestrator properties.
 */
@Component
public class OrchestratorPropertiesValidationModifier implements ITopologyModifier {
    @Inject
    private OrchestratorPropertiesValidationService orchestratorPropertiesValidationService;

    @Override
    public void process(Topology topology, FlowExecutionContext context) {
        Optional<DeploymentMatchingConfiguration> configurationOptional = context.getConfiguration(DeploymentMatchingConfiguration.class,
                OrchestratorPropertiesValidationModifier.class.getSimpleName());

        if (!configurationOptional.isPresent()) { // we should not end-up here as location matching should be processed first
            context.log().error(new LocationPolicyTask());
            return;
        }

        ApplicationEnvironment environment = context.getEnvironmentContext()
                .orElseThrow(() -> new IllegalArgumentException("Input modifier requires an environment context.")).getEnvironment();

        OrchestratorDeploymentProperties orchestratorDeploymentProperties = context
                .getConfiguration(OrchestratorDeploymentProperties.class, OrchestratorPropertiesValidationModifier.class.getSimpleName())
                .orElse(new OrchestratorDeploymentProperties(environment.getTopologyVersion(), environment.getId(),
                        configurationOptional.get().getOrchestratorId()));
        orchestratorPropertiesValidationService.validate(orchestratorDeploymentProperties);
    }
}