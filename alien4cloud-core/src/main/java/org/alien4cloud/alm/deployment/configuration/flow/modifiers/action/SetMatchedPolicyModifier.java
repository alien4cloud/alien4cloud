package org.alien4cloud.alm.deployment.configuration.flow.modifiers.action;

import java.util.Map;
import java.util.Set;

import org.alien4cloud.alm.deployment.configuration.flow.FlowExecutionContext;
import org.alien4cloud.alm.deployment.configuration.model.DeploymentMatchingConfiguration;

/**
 * MatchingModifier for {@link org.alien4cloud.tosca.model.templates.PolicyTemplate}
 */
public class SetMatchedPolicyModifier extends AbstractSetMatchedModifier {

    public SetMatchedPolicyModifier(String policyId, String policyLocationResourceTemplateId) {
        super(policyId, policyLocationResourceTemplateId);
    }

    @Override
    Map<String, String> getLastUserMatches(DeploymentMatchingConfiguration matchingConfiguration) {
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