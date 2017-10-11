package alien4cloud.deployment.matching.services.policies;

import org.alien4cloud.tosca.model.templates.PolicyTemplate;
import org.alien4cloud.tosca.model.types.PolicyType;
import org.springframework.stereotype.Component;

import alien4cloud.deployment.matching.services.nodes.AbstractTemplateMatcher;
import alien4cloud.model.orchestrators.locations.PolicyLocationResourceTemplate;

/**
 * Matches a policy template against a list of resources.
 */
@Component
public class PolicyMatcher extends AbstractTemplateMatcher<PolicyLocationResourceTemplate, PolicyTemplate, PolicyType> {
}