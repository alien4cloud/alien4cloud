package alien4cloud.deployment.matching.services.policies;

import alien4cloud.model.deployment.matching.MatchingConfiguration;
import alien4cloud.model.orchestrators.locations.LocationResources;
import org.alien4cloud.tosca.model.definitions.constraints.IMatchPropertyConstraint;
import org.alien4cloud.tosca.model.templates.PolicyTemplate;
import org.alien4cloud.tosca.model.types.PolicyType;
import org.springframework.stereotype.Component;

import alien4cloud.deployment.matching.services.nodes.AbstractTemplateMatcher;
import alien4cloud.model.orchestrators.locations.PolicyLocationResourceTemplate;

import java.util.List;
import java.util.Map;

/**
 * Matches a policy template against a list of resources.
 */
@Component
public class PolicyMatcher extends AbstractTemplateMatcher<PolicyLocationResourceTemplate, PolicyTemplate, PolicyType> {

    protected boolean validateTemplateMatch(PolicyTemplate abstractTemplate, PolicyLocationResourceTemplate candidate, PolicyType candidateType, LocationResources locationResources,
                                            MatchingConfiguration matchingConfiguration) {

        if (candidate.isOnlyTemplate()) {
            return true;
        } else {
            return super.validateTemplateMatch(abstractTemplate, candidate, candidateType, locationResources, matchingConfiguration);
        }
    }

}
