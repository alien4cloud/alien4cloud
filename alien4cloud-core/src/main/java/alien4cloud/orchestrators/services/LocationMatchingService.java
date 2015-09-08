package alien4cloud.orchestrators.services;

import java.util.List;
import java.util.Map;

import javax.annotation.Resource;

import org.springframework.stereotype.Service;

import alien4cloud.model.orchestrators.locations.ILocationMatcher;
import alien4cloud.model.orchestrators.locations.LocationMatch;
import alien4cloud.model.orchestrators.locations.LocationMatchAuthorizationFilter;
import alien4cloud.model.topology.Topology;
import alien4cloud.orchestrators.locations.services.LocationMatcherFactoriesRegistry;
import alien4cloud.topology.TopologyServiceCore;

@Service
public class LocationMatchingService {

    @Resource(name = "default-location-matcher")
    private ILocationMatcher defaultLocationMatcher;

    @Resource
    private TopologyServiceCore topoServiceCore;

    @Resource
    private LocationMatcherFactoriesRegistry locationMatcherFactoriesRegistry;

    private LocationMatchAuthorizationFilter authorizationFilter = new LocationMatchAuthorizationFilter();

    /**
     * Given a topology, return a list of locations on which the topo can be deployed
     *
     * @param topology
     * @return
     */
    public List<LocationMatch> match(Topology topology) {
        List<LocationMatch> matches;
        // If no registered matcher found later on, then match with the default matcher
        ILocationMatcher matcher = defaultLocationMatcher;

        // TODO Now we just take the first matcher found. To fix. Later, use the configured matcher
        Map<String, Map<String, ILocationMatcher>> instancesByPlugins = locationMatcherFactoriesRegistry.getInstancesByPlugins();
        if (!instancesByPlugins.isEmpty()) {
            Map<String, ILocationMatcher> matchers = instancesByPlugins.values().iterator().next();
            if (!matchers.isEmpty()) {
                matcher = matchers.values().iterator().next();
            }
        }

        matches = matcher.match(topology);

        // keep only the authorized ones
        authorizationFilter.filter(matches, null);

        return matches;
    }

    /**
     * Given a topologyId, return a list of locations on which the related topo can be deployed
     *
     * @param topologyId
     * @return
     */
    public List<LocationMatch> match(String topologyId) {
        Topology topology = topoServiceCore.getOrFail(topologyId);
        return match(topology);
    }
}
