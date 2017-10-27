package org.alien4cloud.alm.deployment.configuration.flow.modifiers;

import java.util.Map;

import org.alien4cloud.alm.deployment.configuration.model.DeploymentMatchingConfiguration;
import org.alien4cloud.alm.deployment.configuration.model.DeploymentMatchingConfiguration.NodePropsOverride;
import org.alien4cloud.tosca.model.templates.PolicyTemplate;
import org.alien4cloud.tosca.model.templates.Topology;
import org.springframework.stereotype.Component;

import lombok.extern.slf4j.Slf4j;

/**
 * This modifier applies user defined properties on substituted policy after matching.
 */
@Slf4j
@Component
public class PostMatchingPolicySetupModifier extends AbstractPostMatchingSetupModifier<PolicyTemplate> {

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

}