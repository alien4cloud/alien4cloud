package org.alien4cloud.alm.deployment.configuration.flow.modifiers.action;

import java.util.Map;
import java.util.Set;

import org.alien4cloud.alm.deployment.configuration.flow.FlowExecutionContext;
import org.alien4cloud.alm.deployment.configuration.model.DeploymentMatchingConfiguration;
import org.alien4cloud.alm.deployment.configuration.model.DeploymentMatchingConfiguration.ResourceMatching;

import alien4cloud.orchestrators.locations.services.ILocationResourceService;

/**
 * MatchingModifier for {@link org.alien4cloud.tosca.model.templates.PolicyTemplate}
 */
public class SetMatchedPolicyModifier extends AbstractSetMatchedModifier {

    public SetMatchedPolicyModifier(String policyId, String policyLocationResourceTemplateId, ILocationResourceService locationResourceService) {
        super(policyId, policyLocationResourceTemplateId, locationResourceService);
    }

    @Override
    Map<String, ResourceMatching> getLastUserMatches(DeploymentMatchingConfiguration matchingConfiguration) {
        return matchingConfiguration.getMatchedPolicies();
    }

    @Override
    Map<String, Set<String>> getAvailableSubstitutions(FlowExecutionContext context) {
        return (Map<String, Set<String>>) context.getExecutionCache().get(FlowExecutionContext.SELECTED_MATCH_POLICY_LOCATION_TEMPLATE_BY_NODE_ID_MAP);
    }

    @Override
    String getSubject() {
        return "policy";
    }

}