package org.alien4cloud.alm.deployment.configuration.flow.modifiers.matching;

import java.util.List;
import java.util.Map;

import org.alien4cloud.alm.deployment.configuration.flow.FlowExecutionContext;
import org.alien4cloud.alm.deployment.configuration.model.DeploymentMatchingConfiguration;
import org.springframework.stereotype.Component;

import alien4cloud.model.orchestrators.locations.PolicyLocationResourceTemplate;

/**
 * This matching modifier is responsible for automatic selection of location resources that are not yet selected by the user.
 *
 * It does not update topology or matching configurations, these operations are done in sub-sequent modifiers.
 */
@Component
public class PolicyMatchingConfigAutoSelectModifier extends AbstractMatchingConfigAutoSelectModifier<PolicyLocationResourceTemplate> {
    @Override
    protected Map<String, String> getLastUserMatches(DeploymentMatchingConfiguration matchingConfiguration) {
        return matchingConfiguration.getMatchedPolicies();
    }

    @Override
    protected Map<String, List<PolicyLocationResourceTemplate>> getAvailableMatches(FlowExecutionContext context) {
        return (Map<String, List<PolicyLocationResourceTemplate>>) context.getExecutionCache().get(FlowExecutionContext.MATCHING_PER_POLICY_LOC_RES_TEMPLATES);
    }
}