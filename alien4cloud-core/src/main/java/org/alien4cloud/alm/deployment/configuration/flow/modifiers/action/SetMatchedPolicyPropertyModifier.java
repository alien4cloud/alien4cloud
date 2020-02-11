package org.alien4cloud.alm.deployment.configuration.flow.modifiers.action;

import java.util.Map;

import org.alien4cloud.alm.deployment.configuration.flow.FlowExecutionContext;
import org.alien4cloud.alm.deployment.configuration.model.DeploymentMatchingConfiguration;
import org.alien4cloud.tosca.model.templates.PolicyTemplate;
import org.alien4cloud.tosca.model.templates.Topology;
import org.alien4cloud.tosca.model.types.PolicyType;

import alien4cloud.model.orchestrators.locations.PolicyLocationResourceTemplate;
import alien4cloud.utils.services.PropertyService;

/**
 * This modifier is injected when the deployment cycle is run in the context of a deployment user update to the properties of a matched policy.
 *
 * It injects the custom user defined property in the node if valid.
 */

public class SetMatchedPolicyPropertyModifier extends AbstractSetMatchedPropertyModifier<PolicyType, PolicyTemplate, PolicyLocationResourceTemplate> {

    public SetMatchedPolicyPropertyModifier(PropertyService propertyService, String templateId, String propertyName, Object propertyValue) {
        super(propertyService, templateId, propertyName, propertyValue);
    }

    @Override
    Map<String, String> getUserMatches(DeploymentMatchingConfiguration matchingConfiguration) {
        return matchingConfiguration.getMatchedPolicies();
    }

    @Override
    Map<String, PolicyLocationResourceTemplate> getAvailableResourceTemplates(FlowExecutionContext context) {
        return (Map<String, PolicyLocationResourceTemplate>) context.getExecutionCache().get(FlowExecutionContext.MATCHED_POLICY_LOCATION_TEMPLATES_BY_ID_MAP);
    }

    @Override
    Map<String, DeploymentMatchingConfiguration.NodePropsOverride> getPropertiesOverrides(DeploymentMatchingConfiguration matchingConfiguration) {
        return matchingConfiguration.getMatchedPoliciesConfiguration();
    }

    @Override
    String getSubject() {
        return "policy";
    }

    @Override
    Map<String, PolicyTemplate> getTemplates(Topology topology) {
        return topology.getPolicies();
    }
}
