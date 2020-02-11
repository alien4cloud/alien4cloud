package org.alien4cloud.alm.deployment.configuration.services;

import java.util.Map;
import java.util.Map.Entry;
import java.util.stream.Collectors;

import javax.annotation.Resource;
import javax.inject.Inject;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

import org.alien4cloud.alm.deployment.configuration.events.OnDeploymentConfigCopyEvent;
import org.alien4cloud.alm.deployment.configuration.events.OnMatchedLocationChangedEvent;
import org.alien4cloud.alm.deployment.configuration.model.AbstractDeploymentConfig;
import org.alien4cloud.alm.deployment.configuration.model.DeploymentMatchingConfiguration;
import org.alien4cloud.tosca.model.templates.AbstractPolicy;
import org.alien4cloud.tosca.model.templates.LocationPlacementPolicy;
import org.alien4cloud.tosca.model.templates.NodeGroup;
import org.apache.commons.collections4.MapUtils;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.event.EventListener;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Service;

import alien4cloud.model.application.ApplicationEnvironment;
import alien4cloud.model.orchestrators.locations.Location;
import alien4cloud.orchestrators.locations.services.LocationSecurityService;
import alien4cloud.orchestrators.locations.services.LocationService;

/**
 * Service to manage location matching and location policies management.
 */
@Service
public class LocationMatchService {
    @Inject
    private DeploymentConfigurationDao deploymentConfigurationDao;
    @Inject
    private LocationService locationService;
    @Resource
    private LocationSecurityService locationSecurityService;
    @Resource
    private ApplicationEventPublisher publisher;

    public void setLocationPolicy(ApplicationEnvironment environment, String orchestratorId,
            Map<String, String> groupsLocationsMapping) {
        if (MapUtils.isEmpty(groupsLocationsMapping)) {
            return;
        }

        DeploymentMatchingConfiguration configuration = deploymentConfigurationDao.findById(
                DeploymentMatchingConfiguration.class,
                AbstractDeploymentConfig.generateId(environment.getTopologyVersion(), environment.getId()));

        if (configuration == null) {
            // TODO probably an error if we get there
            configuration = new DeploymentMatchingConfiguration(environment.getTopologyVersion(), environment.getId());
            configuration.setLocationGroups(Maps.newHashMap());
        }
        // clear all placement policies
        configuration.getLocationGroups().forEach((k, v) -> {
            if (v.getPolicies() != null) {
                v.setPolicies(v.getPolicies().stream()
                        .filter(p -> !p.getType().equals(LocationPlacementPolicy.LOCATION_PLACEMENT_POLICY))
                        .collect(Collectors.toList()));
            }
        });

        for (Entry<String, String> matchEntry : groupsLocationsMapping.entrySet()) {
            String locationId = matchEntry.getValue();
            Location location = locationService.getOrFail(locationId);
            if (!orchestratorId.equals(location.getOrchestratorId())) {
                throw new IllegalArgumentException("Location " + location.getName()
                        + " doesn't belong to the given orchestrator (Id: " + orchestratorId + ")");
            }
            locationSecurityService.checkAuthorisation(location, environment.getId());
            LocationPlacementPolicy locationPolicy = new LocationPlacementPolicy(locationId);
            locationPolicy.setName("Location policy");
            Map<String, NodeGroup> groups = configuration.getLocationGroups();
            NodeGroup group = groups.get(matchEntry.getKey());
            if (group != null) {
                group.setPolicies(Lists.<AbstractPolicy>newArrayList());
                group.getPolicies().add(locationPolicy);
            }
        }

        configuration.setOrchestratorId(orchestratorId);
        configuration.setMatchedLocationResources(Maps.newHashMap());
        configuration.setMatchedNodesConfiguration(Maps.newHashMap());

        publisher.publishEvent(
                new OnMatchedLocationChangedEvent(this, environment, orchestratorId, groupsLocationsMapping));

        deploymentConfigurationDao.save(configuration);
    }

    @EventListener
    @Order(20) // We must process location copy before copy of elements that depends from the
               // location.
    public void onCopyConfiguration(OnDeploymentConfigCopyEvent onDeploymentConfigCopyEvent) {
        ApplicationEnvironment source = onDeploymentConfigCopyEvent.getSourceEnvironment();
        DeploymentMatchingConfiguration sourceConfiguration = deploymentConfigurationDao.findById(
                DeploymentMatchingConfiguration.class,
                AbstractDeploymentConfig.generateId(source.getTopologyVersion(), source.getId()));

        if (sourceConfiguration == null || MapUtils.isEmpty(sourceConfiguration.getLocationGroups())) {
            return; // Nothing to copy
        }

        // Set the location policy to the target environment.
        setLocationPolicy(onDeploymentConfigCopyEvent.getTargetEnvironment(), sourceConfiguration.getOrchestratorId(),
                sourceConfiguration.getLocationIds());
    }
}
