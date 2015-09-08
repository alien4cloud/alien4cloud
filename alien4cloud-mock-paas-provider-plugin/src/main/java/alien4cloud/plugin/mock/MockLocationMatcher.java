package alien4cloud.plugin.mock;

import java.util.List;
import java.util.Map;

import javax.annotation.Resource;

import lombok.extern.slf4j.Slf4j;

import org.apache.commons.collections4.CollectionUtils;
import org.springframework.stereotype.Component;

import alien4cloud.model.orchestrators.Orchestrator;
import alien4cloud.deployment.matching.plugins.ILocationMatcher;
import alien4cloud.model.orchestrators.locations.Location;
import alien4cloud.model.deployment.matching.LocationMatch;
import alien4cloud.model.topology.Topology;
import alien4cloud.orchestrators.locations.services.LocationService;
import alien4cloud.orchestrators.services.OrchestratorService;
import alien4cloud.paas.exception.LocationMatchingException;
import alien4cloud.utils.AlienUtils;

import com.google.common.collect.Lists;

@Slf4j
@Component("mock-location-matcher")
public class MockLocationMatcher implements ILocationMatcher {

    @Resource
    private LocationService locationService;

    @Resource
    private OrchestratorService orchestratorService;

    @Override
    public List<LocationMatch> match(Topology topology) throws LocationMatchingException {
        log.info("Mock location matcher <" + this.getClass().getName() + "> called!");
        List<LocationMatch> matched = Lists.newArrayList();
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
            new MockLocationMatchOrchestratorFilter().filter(matched, topology);
            return matched;
        } catch (Exception e) {
            throw new LocationMatchingException("Failed to match topology <" + topology.getId() + "> against locations. ", e);
        }
    }
}
