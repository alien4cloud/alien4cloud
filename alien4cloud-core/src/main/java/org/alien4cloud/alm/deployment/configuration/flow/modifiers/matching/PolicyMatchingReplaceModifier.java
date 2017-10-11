package org.alien4cloud.alm.deployment.configuration.flow.modifiers.matching;

import java.util.Map;

import org.alien4cloud.alm.deployment.configuration.flow.FlowExecutionContext;
import org.alien4cloud.alm.deployment.configuration.model.DeploymentMatchingConfiguration;
import org.alien4cloud.tosca.model.templates.PolicyTemplate;
import org.alien4cloud.tosca.model.templates.Topology;
import org.springframework.stereotype.Component;

import alien4cloud.model.orchestrators.locations.PolicyLocationResourceTemplate;

/**
 * Last policy matching modifier, it actually inject policy modifiers implementations as policies implementations may impact the actual topology to be deployed.
 */
@Component
public class PolicyMatchingReplaceModifier extends AbstractMatchingReplaceModifier<PolicyTemplate, PolicyLocationResourceTemplate> {

    @Override
    protected String getOriginalTemplateCacheKey() {
        return FlowExecutionContext.MATCHING_ORIGINAL_POLICIES;
    }

    @Override
    protected Map<String, PolicyLocationResourceTemplate> getMatchesById(FlowExecutionContext context) {
        return (Map<String, PolicyLocationResourceTemplate>) context.getExecutionCache().get(FlowExecutionContext.MATCHED_POLICY_LOCATION_TEMPLATES_BY_ID_MAP);
    }

    @Override
    protected Map<String, String> getUserMatches(DeploymentMatchingConfiguration matchingConfiguration) {
        return matchingConfiguration.getMatchedPolicies();
    }

    @Override
    protected Map<String, PolicyTemplate> getTopologyTemplates(Topology topology) {
        return topology.getPolicies();
    }

    @Override
    protected PolicyLocationResourceTemplate getLocationResourceTemplateCopy(String locationResourceTemplateId) {
        return getLocationResourceService().getOrFail(locationResourceTemplateId);
    }
}