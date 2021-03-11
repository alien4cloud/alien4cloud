package org.alien4cloud.alm.deployment.configuration.flow.modifiers;

import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;

import alien4cloud.model.orchestrators.locations.PolicyLocationResourceTemplate;
import com.google.common.collect.Sets;
import org.alien4cloud.alm.deployment.configuration.flow.FlowExecutionContext;
import org.alien4cloud.alm.deployment.configuration.model.DeploymentMatchingConfiguration;
import org.alien4cloud.alm.deployment.configuration.model.DeploymentMatchingConfiguration.NodePropsOverride;
import org.alien4cloud.tosca.model.definitions.AbstractPropertyValue;
import org.alien4cloud.tosca.model.definitions.PropertyDefinition;
import org.alien4cloud.tosca.model.templates.PolicyTemplate;
import org.alien4cloud.tosca.model.templates.Topology;
import org.alien4cloud.tosca.model.types.PolicyType;
import org.springframework.stereotype.Component;

import lombok.extern.slf4j.Slf4j;

import static alien4cloud.utils.AlienUtils.safe;

/**
 * This modifier applies user defined properties on substituted policy after matching.
 */
@Slf4j
@Component
public class PostMatchingPolicySetupModifier extends AbstractPostMatchingSetupModifier<PolicyType, PolicyTemplate> {

    @Override
    Map<String, String> getUserMatches(DeploymentMatchingConfiguration matchingConfiguration) {
        return matchingConfiguration.getMatchedPolicies();
    }

    @Override
    Map<String, NodePropsOverride> getPropertiesOverrides(DeploymentMatchingConfiguration matchingConfiguration) {
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

    @Override
    Class<PolicyType> getToscaTypeClass() {
        return PolicyType.class;
    }

    @Override
    protected boolean isNodeSubstitutedByOnlyTemplateResource(FlowExecutionContext context, DeploymentMatchingConfiguration matchingConfiguration, String nodeId) {
        // Just to know if the location resource template that come into substitution of node is an OnlyTemplate resource template.
        if (matchingConfiguration.getMatchedPolicies() != null) {
            String resourceId = matchingConfiguration.getMatchedPolicies().get(nodeId);
            if (resourceId != null) {
                Object o = context.getExecutionCache().get(FlowExecutionContext.MATCHED_POLICY_LOCATION_TEMPLATES_BY_ID_MAP);
                if (o != null && o instanceof Map) {
                    Map<String, PolicyLocationResourceTemplate> m = (Map<String, PolicyLocationResourceTemplate>)o;
                    PolicyLocationResourceTemplate plrt = m.get(resourceId);
                    if (plrt != null) {
                        return plrt.isOnlyTemplate();
                    }
                }
            }
        }
        return false;
    }

}
