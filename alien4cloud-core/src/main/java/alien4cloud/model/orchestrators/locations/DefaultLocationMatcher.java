package alien4cloud.model.orchestrators.locations;

import java.util.List;
import java.util.Map;

import javax.annotation.Resource;

import lombok.Getter;
import lombok.Setter;

import org.apache.commons.collections4.CollectionUtils;
import org.springframework.stereotype.Component;

import alien4cloud.dao.IGenericSearchDAO;
import alien4cloud.model.orchestrators.Orchestrator;
import alien4cloud.model.topology.Topology;
import alien4cloud.orchestrators.services.LocationService;
import alien4cloud.orchestrators.services.OrchestratorService;
import alien4cloud.paas.exception.LocationMatchingException;
import alien4cloud.utils.AlienUtils;

import com.google.common.collect.Lists;

/**
 * Default location matcher for topologies
 *
 */

@Component("default-location-matcher")
@Getter
@Setter
public class DefaultLocationMatcher implements ILocationMatcher {

    @Resource(name = "alien-es-dao")
    private IGenericSearchDAO alienDAO;

    @Resource
    private LocationService locationService;

    @Resource
    private OrchestratorService orchestratorService;

    @Override
    public List<LocationMatch> match(Topology topology) throws LocationMatchingException {
        List<LocationMatch> matched = Lists.newArrayList();

        try {
            // get all enabled orchestrators
            List<Orchestrator> enabledOrchestrators = orchestratorService.getAllEnabledOrchestrators();
            if (CollectionUtils.isEmpty(enabledOrchestrators)) {
                return matched;
            }

            Map<String, Orchestrator> orchestratorMap = AlienUtils.fromListToMap(enabledOrchestrators, "id", true);
            List<Location> locations = locationService.getOrchestratorsLocations(orchestratorMap.keySet());
            for (Location location : locations) {
                matched.add(new LocationMatch(location, orchestratorMap.get(location.getOrchestratorId()), null));
            }

            // filter on supported artifacts
            new LocationMatchNodeFilter().filter(matched, topology);

            return matched;
        } catch (Exception e) {
            throw new LocationMatchingException("Failed to match topology <" + topology.getId() + "> against locations. ", e);
        }
    }
}
