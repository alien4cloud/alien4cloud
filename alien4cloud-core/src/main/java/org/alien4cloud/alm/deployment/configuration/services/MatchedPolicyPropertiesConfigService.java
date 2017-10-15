package org.alien4cloud.alm.deployment.configuration.services;

import java.util.List;

import javax.annotation.Resource;
import javax.inject.Inject;

import org.alien4cloud.alm.deployment.configuration.flow.EnvironmentContext;
import org.alien4cloud.alm.deployment.configuration.flow.FlowExecutionContext;
import org.alien4cloud.alm.deployment.configuration.flow.FlowExecutor;
import org.alien4cloud.alm.deployment.configuration.flow.ITopologyModifier;
import org.alien4cloud.alm.deployment.configuration.flow.modifiers.PostMatchingPolicySetupModifier;
import org.alien4cloud.alm.deployment.configuration.flow.modifiers.action.SetMatchedPolicyPropertyModifier;
import org.alien4cloud.tosca.model.templates.Topology;
import org.springframework.stereotype.Service;

import alien4cloud.dao.IGenericSearchDAO;
import alien4cloud.model.application.Application;
import alien4cloud.model.application.ApplicationEnvironment;
import alien4cloud.utils.services.PropertyService;

/**
 * Manage configuration of deployer configured properties for matched nodes.
 */
@Service
public class MatchedPolicyPropertiesConfigService {
    @Resource(name = "alien-es-dao")
    private IGenericSearchDAO alienDAO;
    @Inject
    private FlowExecutor flowExecutor;
    @Inject
    private PostMatchingPolicySetupModifier postMatchingPolicySetupModifier;
    @Inject
    private PropertyService propertyService;

    /**
     * Execute the deployment flow with a modification of changing the substitution for one of the nodes.
     *
     * @param application The application for which to execute the deployment flow.
     * @param environment The environment for which to execute the deployment flow.
     * @param topology The topology linked to the specified environment.
     * @param nodeId The id of the node for which to configure property.
     * @param propertyName The id of the property to set value.
     * @param propertyValue the value of the property.
     * @return The flow execution context.
     */
    public FlowExecutionContext updateProperty(Application application, ApplicationEnvironment environment, Topology topology, String nodeId,
            String propertyName, Object propertyValue) {
        FlowExecutionContext executionContext = new FlowExecutionContext(alienDAO, topology, new EnvironmentContext(application, environment));
        // Load the actual configuration

        // add a modifier that will actually perform the configuration of a substitution from user request (after cleanup and prior to node matching
        // auto-selection).
        List<ITopologyModifier> modifierList = getModifierListWithSelectionAction(nodeId, propertyName, propertyValue);

        flowExecutor.execute(topology, modifierList, executionContext);
        return executionContext;
    }

    private List<ITopologyModifier> getModifierListWithSelectionAction(String nodeId, String propertyName, Object propertyValue) {
        List<ITopologyModifier> modifierList = flowExecutor.getDefaultFlowModifiers();

        for (int i = 0; i < modifierList.size(); i++) {
            if (modifierList.get(i) == postMatchingPolicySetupModifier) {
                modifierList.add(i, new SetMatchedPolicyPropertyModifier(propertyService, nodeId, propertyName, propertyValue));
                return modifierList;
            }
        }

        throw new IllegalArgumentException(
                "Unexpected exception in deployment flow to update node substitution; unable to find the matching config cleanup modifier to inject selection action modifier.");
    }
}
