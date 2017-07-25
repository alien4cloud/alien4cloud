package org.alien4cloud.alm.deployment.configuration.flow.modifiers;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import javax.inject.Inject;

import alien4cloud.model.application.ApplicationEnvironment;
import org.alien4cloud.alm.deployment.configuration.flow.FlowExecutionContext;
import org.alien4cloud.alm.deployment.configuration.flow.ITopologyModifier;
import org.alien4cloud.alm.deployment.configuration.model.DeploymentMatchingConfiguration;
import org.alien4cloud.tosca.model.templates.Topology;
import org.apache.commons.collections4.MapUtils;
import org.springframework.stereotype.Component;

import com.google.common.collect.Maps;

import alien4cloud.deployment.matching.services.location.LocationMatchingService;
import alien4cloud.model.deployment.matching.ILocationMatch;
import alien4cloud.model.orchestrators.locations.Location;
import alien4cloud.orchestrators.locations.services.LocationService;
import alien4cloud.topology.task.LocationPolicyTask;
import alien4cloud.tosca.context.ToscaContext;

import static alien4cloud.utils.AlienUtils.safe;

/**
 * This processor actually does not change the topology but check that location settings are defined and up-to-date in order to allow other processors to
 * continue.
 */
@Component
public class LocationMatchingModifier implements ITopologyModifier {
    @Inject
    private LocationMatchingService locationMatchingService;
    @Inject
    private LocationService locationService;

    @Override
    public void process(Topology topology, FlowExecutionContext context) {
        // The configuration has already been loaded by a previous topology modifier.
        Optional<DeploymentMatchingConfiguration> configurationOptional = context.getConfiguration(DeploymentMatchingConfiguration.class,
                LocationMatchingModifier.class.getSimpleName());

        if (!configurationOptional.isPresent()) {
            context.log().error(new LocationPolicyTask());
            return;
        }
        DeploymentMatchingConfiguration matchingConfiguration = configurationOptional.get();

        // If some of the locations defined does not exist anymore then just reset the deployment matching configuration
        Map<String, String> locationIds = matchingConfiguration.getLocationIds();
        if (MapUtils.isEmpty(locationIds)) {
            context.log().error(new LocationPolicyTask());
            return;
        }

        Map<String, Location> locations = getLocations(locationIds); // returns null if at least one of the expected location is missing.
        if (locations == null) {
            // TODO Add an info log to explain that previous location does not exist anymore
            // Reset and save the configuration
            resetMatchingConfiguration(context);
            return;
        }
        context.getExecutionCache().put(FlowExecutionContext.DEPLOYMENT_LOCATIONS_MAP_CACHE_KEY, locations);

        // Now we must check that the selected locations are still valid choices for deployment
        // Somehow if the initial topology and none of the previous modifiers had changed then we could assume that the choice is still valid.
        // We had an approximation for that in the past that was not correct enough as we checked only initial topology and location. Actually inputs and
        // potential snapshot substitution merged through composition may also impact this. We know always re-process location matching here.
        // We have to fetch valid location matches anyway to know if the location is a potential valid match.
        List<ILocationMatch> locationMatches = locationMatchingService.match(topology, context.getEnvironmentContext().get().getEnvironment());

        context.getExecutionCache().put(FlowExecutionContext.LOCATION_MATCH_CACHE_KEY, locationMatches);

        Map<String, ILocationMatch> locationMatchMap = Maps.newHashMap();
        // Check that current choices exist in actual location matches
        for (ILocationMatch match : safe(locationMatches)) {
            locationMatchMap.put(match.getLocation().getId(), match);
        }

        for (String locationId : locationIds.values()) {
            if (!locationMatchMap.containsKey(locationId)) { // A matched location is not a valid choice anymore.
                resetMatchingConfiguration(context);
                return;
            }
        }

        // Add the dependencies of the location(s) to the topology
        for (Location location : locations.values()) {
            // FIXME manage conflicting dependencies by fetching types from latest version
            topology.getDependencies().addAll(location.getDependencies());
        }

        // update the TOSCA context with the new dependencies so that next step runs with an up-to-date context
        ToscaContext.get().resetDependencies(topology.getDependencies());
    }

    private void resetMatchingConfiguration(FlowExecutionContext context) {
        ApplicationEnvironment environment = context.getEnvironmentContext()
                .orElseThrow(() -> new IllegalArgumentException("Input modifier requires an environment context.")).getEnvironment();
        context.saveConfiguration(new DeploymentMatchingConfiguration(environment.getTopologyVersion(), environment.getId()));
        context.log().error(new LocationPolicyTask());
    }

    /**
     * Get location map from the deployment topology
     *
     * @param locationIds map of group id to location id
     * @return map of location group id to location or null if at least one of the expected locations is missing.
     */
    private Map<String, Location> getLocations(Map<String, String> locationIds) {
        Map<String, Location> locations = locationService.getMultiple(locationIds.values());
        Map<String, Location> locationMap = Maps.newHashMap();
        for (Map.Entry<String, String> locationIdsEntry : locationIds.entrySet()) {
            Location location = locations.get(locationIdsEntry.getValue());
            if (location == null) {
                return null;
            }
            locationMap.put(locationIdsEntry.getKey(), location);
        }
        return locationMap;
    }
}
