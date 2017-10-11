package alien4cloud.deployment.matching.services.policies;

import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import javax.inject.Inject;

import org.alien4cloud.tosca.model.templates.PolicyTemplate;
import org.alien4cloud.tosca.model.types.PolicyType;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

import com.google.common.collect.Maps;

import alien4cloud.model.orchestrators.locations.Location;
import alien4cloud.model.orchestrators.locations.LocationResources;
import alien4cloud.model.orchestrators.locations.PolicyLocationResourceTemplate;
import alien4cloud.orchestrators.locations.services.ILocationResourceService;
import alien4cloud.orchestrators.locations.services.LocationSecurityService;

/**
 * Service responsible for finding matching.
 */
@Service
public class PolicyMatcherService {
    @Inject
    @Lazy
    private ILocationResourceService locationResourceService;
    @Inject
    private LocationSecurityService locationSecurityService;

    @Inject
    private PolicyMatcher policyMatcher;

    /**
     * Perform matching of policies from a topology.
     *
     * @param policyTemplates The policy templates to match.
     * @param location The location against which to match policies.
     * @return A Map of available matches for every policy template.
     */
    public Map<String, List<PolicyLocationResourceTemplate>> match(Map<String, PolicyTemplate> policyTemplates, Map<String, PolicyType> policyTypes,
            Location location, String environmentId) {
        Map<String, List<PolicyLocationResourceTemplate>> matches = Maps.newHashMap();
        // fetch location resources
        LocationResources locationResources = locationResourceService.getLocationResources(location);
        // Authorization filtering of location resources
        locationResources.getPolicyTemplates().removeIf(securedResource -> !locationSecurityService.isAuthorised(securedResource, environmentId));

        for (Entry<String, PolicyTemplate> policyTemplateEntry : policyTemplates.entrySet()) {
            PolicyType policyType = policyTypes.get(policyTemplateEntry.getValue().getType());
            matches.put(policyTemplateEntry.getKey(), policyMatcher.match(policyTemplateEntry.getValue(), policyType, locationResources.getPolicyTemplates(),
                    locationResources.getPolicyTypes(), locationResources, null));
        }

        return matches;
    }
}