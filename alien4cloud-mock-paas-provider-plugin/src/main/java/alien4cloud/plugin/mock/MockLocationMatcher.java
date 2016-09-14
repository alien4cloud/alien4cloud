package alien4cloud.plugin.mock;

import java.util.List;
import java.util.Map;

import javax.annotation.Resource;
import javax.inject.Inject;

import org.apache.commons.collections4.CollectionUtils;

import com.google.common.collect.Lists;

import alien4cloud.deployment.matching.plugins.ILocationMatcher;
import alien4cloud.model.deployment.matching.ILocationMatch;
import alien4cloud.model.deployment.matching.LocationMatch;
import alien4cloud.model.orchestrators.Orchestrator;
import alien4cloud.model.orchestrators.locations.Location;
import org.alien4cloud.tosca.model.templates.Topology;
import alien4cloud.orchestrators.locations.services.LocationService;
import alien4cloud.orchestrators.services.OrchestratorService;
import alien4cloud.paas.exception.LocationMatchingException;
import alien4cloud.plugin.model.ManagedPlugin;
import alien4cloud.utils.AlienUtils;
import lombok.extern.slf4j.Slf4j;

@Slf4j
// @Component("mock-location-matcher")
public class MockLocationMatcher implements ILocationMatcher {

    @Resource
    private LocationService locationService;

    @Inject
    private ManagedPlugin selfContext;

    @Resource
    private OrchestratorService orchestratorService;

    @Override
    public List<ILocationMatch> match(Topology topology) throws LocationMatchingException {
        log.info("Mock location matcher <" + this.getClass().getName() + "> called!");
        List<ILocationMatch> matched = Lists.newArrayList();
        // get all enabled orchestrators
        try {
            List<Orchestrator> enabledOrchestrators = orchestratorService.getAllEnabledOrchestrators();
            if (CollectionUtils.isEmpty(enabledOrchestrators)) {
                return matched;
            }

            Map<String, Orchestrator> orchestratorMap = AlienUtils.fromListToMap(enabledOrchestrators, "id", true);
            List<Location> locations = locationService.getOrchestratorsLocations(orchestratorMap.keySet());
            for (Location location : locations) {
                matched.add(new LocationMatch(location, orchestratorMap.get(location.getOrchestratorId()), null));
            }
            new MockLocationMatchOrchestratorFilter(selfContext).filter(matched, topology);
            return matched;
        } catch (Exception e) {
            throw new LocationMatchingException("Failed to match topology <" + topology.getId() + "> against locations. ", e);
        }
    }
}
