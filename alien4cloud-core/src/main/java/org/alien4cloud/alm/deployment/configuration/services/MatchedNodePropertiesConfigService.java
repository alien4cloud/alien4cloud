package org.alien4cloud.alm.deployment.configuration.services;

import alien4cloud.model.application.Application;
import alien4cloud.model.application.ApplicationEnvironment;
import alien4cloud.utils.services.PropertyService;
import org.alien4cloud.alm.deployment.configuration.flow.EnvironmentContext;
import org.alien4cloud.alm.deployment.configuration.flow.FlowExecutionContext;
import org.alien4cloud.alm.deployment.configuration.flow.FlowExecutor;
import org.alien4cloud.alm.deployment.configuration.flow.ITopologyModifier;
import org.alien4cloud.alm.deployment.configuration.flow.modifiers.PostMatchingNodeSetupModifier;
import org.alien4cloud.alm.deployment.configuration.flow.modifiers.action.SetMatchedNodePropertyModifier;
import org.alien4cloud.tosca.model.templates.Topology;
import org.springframework.stereotype.Service;

import javax.inject.Inject;
import java.util.List;
import java.util.Optional;

/**
 * Manage configuration of deployer configured properties for matched nodes.
 */
@Service
public class MatchedNodePropertiesConfigService {
    @Inject
    private DeploymentConfigurationDao deploymentConfigurationDao;
    @Inject
    private FlowExecutor flowExecutor;
    @Inject
    private PostMatchingNodeSetupModifier postMatchingNodeSetupModifier;
    @Inject
    private PropertyService propertyService;

    /**
     * Execute the deployment flow with a modification of changing the substitution for one of the nodes.
     *
     * @param application The application for which to execute the deployment flow.
     * @param environment The environment for which to execute the deployment flow.
     * @param topology The topology linked to the specified environment.
     * @param nodeId The id of the node for which to configure property.
     * @param optionalCapabilityName An optional capability name in case we want to update a property of the capability.
     * @param propertyName The id of the property to set value.
     * @param propertyValue the value of the property.
     * @return The flow execution context.
     */
    public FlowExecutionContext updateProperty(Application application, ApplicationEnvironment environment, Topology topology, String nodeId,
            Optional<String> optionalCapabilityName, String propertyName, Object propertyValue) {
        FlowExecutionContext executionContext = new FlowExecutionContext(deploymentConfigurationDao, topology, new EnvironmentContext(application, environment));
        // Load the actual configuration

        // add a modifier that will actually perform the configuration of a substitution from user request (after cleanup and prior to node matching
        // auto-selection).
        List<ITopologyModifier> modifierList = getModifierListWithSelectionAction(nodeId, optionalCapabilityName, propertyName, propertyValue);

        flowExecutor.execute(topology, modifierList, executionContext);
        return executionContext;
    }

    private List<ITopologyModifier> getModifierListWithSelectionAction(String nodeId, Optional<String> optionalCapabilityName, String propertyName,
            Object propertyValue) {
        List<ITopologyModifier> modifierList = flowExecutor.getDefaultFlowModifiers();

        for (int i = 0; i < modifierList.size(); i++) {
            if (modifierList.get(i) == postMatchingNodeSetupModifier) {
                modifierList.add(i, new SetMatchedNodePropertyModifier(propertyService, nodeId, optionalCapabilityName, propertyName, propertyValue));
                return modifierList;
            }
        }

        throw new IllegalArgumentException(
                "Unexpected exception in deployment flow to update node substitution; unable to find the matching config cleanup modifier to inject selection action modifier.");
    }
}
