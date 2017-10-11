package org.alien4cloud.alm.deployment.configuration.flow.modifiers.matching;

import java.util.List;
import java.util.Map;

import org.alien4cloud.alm.deployment.configuration.flow.FlowExecutionContext;
import org.alien4cloud.alm.deployment.configuration.flow.ITopologyModifier;
import org.alien4cloud.alm.deployment.configuration.model.DeploymentMatchingConfiguration;
import org.springframework.stereotype.Component;

import alien4cloud.model.orchestrators.locations.PolicyLocationResourceTemplate;

/**
 * This modifier cleanup the user matching configuration in case it is not valid anymore based on the choices available (that must be fetched from prior
 * PolicyMatchingCandidateModifier execution).
 *
 * It does not update topology or matching configurations, these operations are done in sub-sequent modifiers.
 */
@Component
public class PolicyMatchingConfigCleanupModifier extends AbstractMatchingConfigCleanupModifier<PolicyLocationResourceTemplate> implements ITopologyModifier {
    @Override
    protected Map<String, String> getLastUserMatches(DeploymentMatchingConfiguration matchingConfiguration) {
        return matchingConfiguration.getMatchedPolicies();
    }

    @Override
    protected Map<String, List<PolicyLocationResourceTemplate>> getAvailableMatches(FlowExecutionContext context) {
        return (Map<String, List<PolicyLocationResourceTemplate>>) context.getExecutionCache().get(FlowExecutionContext.MATCHING_PER_POLICY_LOC_RES_TEMPLATES);
    }
}