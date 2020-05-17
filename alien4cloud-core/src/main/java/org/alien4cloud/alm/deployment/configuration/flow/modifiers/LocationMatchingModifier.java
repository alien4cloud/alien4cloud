package org.alien4cloud.alm.deployment.configuration.flow.modifiers;

import static alien4cloud.utils.AlienUtils.safe;

import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import javax.inject.Inject;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;

import org.alien4cloud.alm.deployment.configuration.flow.FlowExecutionContext;
import org.alien4cloud.alm.deployment.configuration.flow.ITopologyModifier;
import org.alien4cloud.alm.deployment.configuration.model.DeploymentMatchingConfiguration;
import org.alien4cloud.alm.deployment.configuration.model.SecretCredentialInfo;
import org.alien4cloud.secret.services.SecretProviderService;
import org.alien4cloud.tosca.model.templates.Topology;
import org.apache.commons.collections4.MapUtils;
import org.springframework.stereotype.Component;

import alien4cloud.common.MetaPropertiesService;
import alien4cloud.deployment.matching.services.location.LocationMatchingService;
import alien4cloud.model.application.ApplicationEnvironment;
import alien4cloud.model.deployment.matching.ILocationMatch;
import alien4cloud.model.orchestrators.locations.Location;
import alien4cloud.model.orchestrators.locations.LocationModifierReference;
import alien4cloud.orchestrators.locations.services.LocationService;
import alien4cloud.plugin.PluginManager;
import alien4cloud.plugin.exception.MissingPluginException;
import alien4cloud.topology.validation.LocationPolicyValidationService;
import alien4cloud.tosca.context.ToscaContext;
import lombok.extern.slf4j.Slf4j;

/**
 * This processor actually does not change the topology but check that location
 * settings are defined and up-to-date in order to allow other processors to
 * continue.
 */
@Slf4j
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
    @Inject
    private MetaPropertiesService metaPropertiesService;
    @Inject
    private SecretProviderService secretProviderService;
    @Inject
    private PluginManager pluginManager;

    @Override
    public void process(Topology topology, FlowExecutionContext context) {
        // first process
        processLocationMatching(topology, context);

        Optional<DeploymentMatchingConfiguration> configurationOptional = context.getConfiguration(
                DeploymentMatchingConfiguration.class, LocationMatchingModifier.class.getSimpleName());

        // perform validation
        locationPolicyValidationService
                .validateLocationPolicies(configurationOptional.orElse(new DeploymentMatchingConfiguration()))
                .forEach(locationPolicyTask -> context.log().error(locationPolicyTask));

        // No errors from validation let's inject location topology modifiers if any.
        if (context.log().isValid()) {
            Map<String, Location> selectedLocations = (Map<String, Location>) context.getExecutionCache()
                    .get(FlowExecutionContext.DEPLOYMENT_LOCATIONS_MAP_CACHE_KEY);

            List<Location> locationsWithVault = selectedLocations.values().stream()
                    .filter(location -> location.getSecretProviderConfiguration() != null
                            && location.getSecretProviderConfiguration().getConfiguration() != null)
                    .collect(Collectors.toList());
            boolean needVaultCredential = locationsWithVault.size() > 0;
            if (needVaultCredential) {
                List<SecretCredentialInfo> secretCredentialInfos = new LinkedList<>();
                for (Location location : locationsWithVault) {
                    try {
                        SecretCredentialInfo info = new SecretCredentialInfo();
                        String pluginName = location.getSecretProviderConfiguration().getPluginName();
                        Object rawSecretConfiguration = location.getSecretProviderConfiguration().getConfiguration();
                        secretCredentialInfos
                                .add(secretProviderService.getSecretCredentialInfo(pluginName, rawSecretConfiguration));
                    } catch (Exception e) {
                        log.error("Cannot process secret provider configuration", e);
                    }
                }
                context.getExecutionCache().put(FlowExecutionContext.SECRET_CREDENTIAL, secretCredentialInfos);
            } else {
                context.getExecutionCache().remove(FlowExecutionContext.SECRET_CREDENTIAL);
            }
            for (Location location : safe(selectedLocations.values())) {
                for (LocationModifierReference modifierReference : safe(location.getModifiers())) {
                    if (pluginManager.getPluginOrFail(modifierReference.getPluginId()).isEnabled()) {
                        injectLocationTopologyModfier(context, location, modifierReference);
                    } else {
                        log.info("The plugin " + modifierReference.getPluginId() + " is not activated. Ignoring "
                                + modifierReference.getBeanName() + ".");
                    }
                }
            }
        }
    }

    private void injectLocationTopologyModfier(FlowExecutionContext context,
            Location location,
            LocationModifierReference modifierReference) {
        try {
            ITopologyModifier modifier = pluginModifierRegistry.getPluginBean(modifierReference.getPluginId(),
                    modifierReference.getBeanName());
            String phaseName = modifierReference.getPhase() + "-" + location.getId();
            List<ITopologyModifier> phaseModifiers = (List<ITopologyModifier>) context.getExecutionCache()
                    .get(phaseName);
            if (phaseModifiers == null) {
                phaseModifiers = Lists.newArrayList();
                context.getExecutionCache().put(phaseName, phaseModifiers);
            }
            if (!phaseModifiers.contains(modifier)) {
                phaseModifiers.add(modifier);
            }
        } catch (MissingPluginException e) {
            context.log().error("Location {} defines modifier that refers to plugin bean {}, {} cannot be found.",
                    location.getName(), modifierReference.getPluginId(), modifierReference.getBeanName());
        }
    }

    private void processLocationMatching(Topology topology, FlowExecutionContext context) {
        // The configuration has already been loaded by a previous topology modifier.
        Optional<DeploymentMatchingConfiguration> configurationOptional = context.getConfiguration(
                DeploymentMatchingConfiguration.class, LocationMatchingModifier.class.getSimpleName());

        if (!configurationOptional.isPresent()) {
            return;
        }
        DeploymentMatchingConfiguration matchingConfiguration = configurationOptional.get();

        // If some of the locations defined does not exist anymore then just reset the
        // deployment matching configuration
        Map<String, String> groupsToLocationIds = matchingConfiguration.getLocationIds();
        if (MapUtils.isEmpty(groupsToLocationIds)) {
            return;
        }

        Map<String, Location> locations = getLocations(groupsToLocationIds); // returns null if at least one of the expected
                                                                     // location is missing.
        if (locations == null) {
            // TODO Add an info log to explain that previous location does not exist anymore
            // Reset and save the configuration
            resetMatchingConfiguration(context);
            return;
        }
        context.getExecutionCache().put(FlowExecutionContext.DEPLOYMENT_LOCATIONS_MAP_CACHE_KEY, locations);

        // Now we must check that the selected locations are still valid choices for
        // deployment
        // Somehow if the initial topology and none of the previous modifiers had
        // changed then we could assume that the choice is still valid.
        // We had an approximation for that in the past that was not correct enough as
        // we checked only initial topology and location. Actually inputs and
        // potential snapshot substitution merged through composition may also impact
        // this. We know always re-process location matching here.
        // We have to fetch valid location matches anyway to know if the location is a
        // potential valid match.
        List<ILocationMatch> locationMatches = locationMatchingService.match(topology,
                context.getEnvironmentContext().get().getEnvironment());

        context.getExecutionCache().put(FlowExecutionContext.LOCATION_MATCH_CACHE_KEY, locationMatches);

        Map<String, ILocationMatch> locationMatchMap = Maps.newHashMap();
        // Check that current choices exist in actual location matches
        for (ILocationMatch match : safe(locationMatches)) {
            locationMatchMap.put(match.getLocation().getId(), match);
        }

        for (String locationId : groupsToLocationIds.values()) {
            if (!locationMatchMap.containsKey(locationId)) { // A matched location is not a valid choice anymore.
                resetMatchingConfiguration(context);
                // TODO info log the reason why the location is no more a valid match
                return;
            }
        }

        // update the TOSCA context with the new dependencies so that next step runs
        // with an up-to-date context
        ToscaContext.get().resetDependencies(topology.getDependencies());


        // Now we know for each group on which location it should live
        // lets build a map of locations to nodes
        buildLocationsToNodesMap(context, matchingConfiguration, groupsToLocationIds);
    }


    private void buildLocationsToNodesMap(FlowExecutionContext context,  DeploymentMatchingConfiguration matchingConfiguration, Map<String,String> groupsToLocationIds) {
        Map<String, Set<String>> locationsToNodes = Maps.newHashMap();
        for (Entry<String, String> groupToLocEntry : groupsToLocationIds.entrySet()) {
            Set<String> nodes = locationsToNodes.computeIfAbsent(groupToLocEntry.getValue(), k -> Sets.newHashSet());
            nodes.addAll(matchingConfiguration.getLocationGroups().get(groupToLocEntry.getKey()).getMembers());
        }

        context.getExecutionCache().put(FlowExecutionContext.NODES_PER_LOCATIONS_CACHE_KEY, locationsToNodes);

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
     * @return map of location group id to location or null if at least one of the
     *         expected locations is missing.
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
