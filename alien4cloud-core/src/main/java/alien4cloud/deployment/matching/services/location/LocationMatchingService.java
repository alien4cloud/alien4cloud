package alien4cloud.deployment.matching.services.location;

import java.util.List;
import java.util.Map;

import javax.annotation.Resource;
import javax.inject.Inject;

import org.alien4cloud.tosca.model.templates.Topology;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.MapUtils;
import org.springframework.stereotype.Service;

import alien4cloud.deployment.matching.plugins.ILocationMatcher;
import alien4cloud.model.application.ApplicationEnvironment;
import alien4cloud.model.deployment.matching.ILocationMatch;
import alien4cloud.topology.TopologyServiceCore;

@Service
public class LocationMatchingService {
    @Resource(name = "default-location-matcher")
    private ILocationMatcher defaultLocationMatcher;
    @Inject
    private TopologyServiceCore topoServiceCore;
    @Inject
    private LocationMatcherFactoriesRegistry locationMatcherFactoriesRegistry;
    @Resource
    private LocationMatchAuthorizationFilter authorizationFilter;

    /**
     * Given a topology, return a list of locations on which the topo can be deployed
     *
     * @param topology The topology to match against the location matcher.
     * @param applicationEnvironment eventually the environment related to the topology to match.
     * @return A list of candidates Location Matches.
     */
    public List<ILocationMatch> match(Topology topology, ApplicationEnvironment applicationEnvironment) {
        List<ILocationMatch> matches;
        // If no registered matcher id found later on, then match with the default matcher
        ILocationMatcher matcher = defaultLocationMatcher;

        // TODO Now we just take the first matcher found. To fix. Later, use the configured matcher
        Map<String, Map<String, ILocationMatcher>> instancesByPlugins = locationMatcherFactoriesRegistry.getInstancesByPlugins();
        if (MapUtils.isNotEmpty(instancesByPlugins)) {
            Map<String, ILocationMatcher> matchers = instancesByPlugins.values().iterator().next();
            if (MapUtils.isNotEmpty(matchers)) {
                matcher = matchers.values().iterator().next();
            }
        }

        matches = matcher.match(topology);
        // keep only the authorized ones
        authorizationFilter.filter(matches, applicationEnvironment);

        return CollectionUtils.isEmpty(matches) ? null : matches;
    }

    /**
     * Given a topologyId, return a list of locations on which the related topo can be deployed
     *
     * @param topologyId mandatory topology id
     * @param applicationEnvironment optional environment, the context from which the match is done, it affects the security aspect
     * @return list of matched locations
     */
    public List<ILocationMatch> match(String topologyId, ApplicationEnvironment applicationEnvironment) {
        Topology topology = topoServiceCore.getOrFail(topologyId);
        return match(topology, applicationEnvironment);
    }
}
