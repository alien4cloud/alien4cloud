package org.alien4cloud.alm.deployment.configuration.flow.modifiers;

import static alien4cloud.utils.AlienUtils.safe;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import javax.inject.Inject;

import org.alien4cloud.alm.deployment.configuration.flow.FlowExecutionContext;
import org.alien4cloud.alm.deployment.configuration.flow.ITopologyModifier;
import org.alien4cloud.alm.deployment.configuration.model.DeploymentMatchingConfiguration;
import org.alien4cloud.tosca.model.templates.Topology;
import org.apache.commons.collections4.MapUtils;
import org.springframework.stereotype.Component;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

import alien4cloud.common.MetaPropertiesService;
import alien4cloud.deployment.matching.services.location.LocationMatchingService;
import alien4cloud.model.application.ApplicationEnvironment;
import alien4cloud.model.deployment.matching.ILocationMatch;
import alien4cloud.model.orchestrators.locations.Location;
import alien4cloud.model.orchestrators.locations.LocationModifierReference;
import alien4cloud.orchestrators.locations.services.LocationService;
import alien4cloud.plugin.exception.MissingPluginException;
import alien4cloud.topology.validation.LocationPolicyValidationService;
import alien4cloud.tosca.context.ToscaContext;

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
    @Inject
    private PluginModifierRegistry pluginModifierRegistry;
    @Inject
    private LocationPolicyValidationService locationPolicyValidationService;

    @Override
    public void process(Topology topology, FlowExecutionContext context) {
        // first process
        processLocationMatching(topology, context);

        Optional<DeploymentMatchingConfiguration> configurationOptional = context.getConfiguration(DeploymentMatchingConfiguration.class,
                LocationMatchingModifier.class.getSimpleName());

        // perform validation
        locationPolicyValidationService.validateLocationPolicies(configurationOptional.orElse(new DeploymentMatchingConfiguration()))
                .forEach(locationPolicyTask -> context.log().error(locationPolicyTask));

        // No errors from validation let's inject location topology modifiers if any.
        if (context.log().isValid()) {
            Map<String, Location> selectedLocations = (Map<String, Location>) context.getExecutionCache()
                    .get(FlowExecutionContext.DEPLOYMENT_LOCATIONS_MAP_CACHE_KEY);
            for (LocationModifierReference modifierReference : safe(selectedLocations.values().iterator().next().getModifiers())) {
                injectLocationTopologyModfier(context, selectedLocations.values().iterator().next().getName(), modifierReference);
            }
        }
    }

    private void injectLocationTopologyModfier(FlowExecutionContext context, String locationName, LocationModifierReference modifierReference) {
        try {
            ITopologyModifier modifier = pluginModifierRegistry.getPluginBean(modifierReference.getPluginId(), modifierReference.getBeanName());
            List<ITopologyModifier> phaseModifiers = (List<ITopologyModifier>) context.getExecutionCache().get(modifierReference.getPhase());
            if (phaseModifiers == null) {
                phaseModifiers = Lists.newArrayList();
                context.getExecutionCache().put(modifierReference.getPhase(), phaseModifiers);
            }
            phaseModifiers.add(modifier);
        } catch (MissingPluginException e) {
            context.log().error("Location {} defines modifier that refers to plugin bean {}, {} cannot be found.", locationName,
                    modifierReference.getPluginId(), modifierReference.getBeanName());
        }
    }

    private void processLocationMatching(Topology topology, FlowExecutionContext context) {
        // The configuration has already been loaded by a previous topology modifier.
        Optional<DeploymentMatchingConfiguration> configurationOptional = context.getConfiguration(DeploymentMatchingConfiguration.class,
                LocationMatchingModifier.class.getSimpleName());

        if (!configurationOptional.isPresent()) {
            return;
        }
        DeploymentMatchingConfiguration matchingConfiguration = configurationOptional.get();

        // If some of the locations defined does not exist anymore then just reset the deployment matching configuration
        Map<String, String> locationIds = matchingConfiguration.getLocationIds();
        if (MapUtils.isEmpty(locationIds)) {
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
                // TODO info log the reason why the location is no more a valid match
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
