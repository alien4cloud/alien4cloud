package org.alien4cloud.alm.deployment.configuration.services;

import alien4cloud.model.application.ApplicationEnvironment;
import alien4cloud.model.orchestrators.locations.Location;
import alien4cloud.orchestrators.locations.services.LocationSecurityService;
import alien4cloud.orchestrators.locations.services.LocationService;
import alien4cloud.utils.AlienConstants;
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

import javax.annotation.Resource;
import javax.inject.Inject;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;

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

    public void setLocationPolicy(ApplicationEnvironment environment, String orchestratorId, Map<String, String> groupsLocationsMapping) {
        if (MapUtils.isEmpty(groupsLocationsMapping)) {
            return;
        }

        DeploymentMatchingConfiguration configuration = deploymentConfigurationDao.findById(DeploymentMatchingConfiguration.class,
                AbstractDeploymentConfig.generateId(environment.getTopologyVersion(), environment.getId()));

        if (configuration == null) {
            configuration = new DeploymentMatchingConfiguration(environment.getTopologyVersion(), environment.getId());
        } else if(configuration.getLocationGroups() == null) {
            configuration.setLocationGroups(Maps.newHashMap());
        }

        Map<String, String> currentConfiguration = configuration.getLocationIds();

        // TODO For now, we only support one location policy for all nodes. So we have a group _A4C_ALL that represents all compute nodes in the topology
        // To improve later on for multiple groups support
        // throw an exception if multiple location policies provided: not yet supported
        // throw an exception if group name is not _A4C_ALL
        checkGroups(groupsLocationsMapping);
        boolean updated = false;

        for (Entry<String, String> matchEntry : groupsLocationsMapping.entrySet()) {
            String current = currentConfiguration.get(matchEntry.getKey());
            if (current != null && current.equals(matchEntry.getValue())) {
                continue;
            }
            updated = true;

            String locationId = matchEntry.getValue();
            Location location = locationService.getOrFail(locationId);
            locationSecurityService.checkAuthorisation(location, environment.getId());
            LocationPlacementPolicy locationPolicy = new LocationPlacementPolicy(locationId);
            locationPolicy.setName("Location policy");
            Map<String, NodeGroup> groups = configuration.getLocationGroups();
            NodeGroup group = new NodeGroup();
            group.setName(matchEntry.getKey());
            group.setPolicies(Lists.<AbstractPolicy> newArrayList());
            group.getPolicies().add(locationPolicy);
            groups.put(matchEntry.getKey(), group);
        }

        if (!updated) {
            return; // nothing has changed.
        }

        configuration.setOrchestratorId(orchestratorId);
        configuration.setMatchedLocationResources(Maps.newHashMap());
        configuration.setMatchedNodesConfiguration(Maps.newHashMap());

        publisher.publishEvent(new OnMatchedLocationChangedEvent(this, environment, orchestratorId, groupsLocationsMapping));

        deploymentConfigurationDao.save(configuration);
    }

    private void checkGroups(Map<String, String> groupsLocationsMapping) {
        if (groupsLocationsMapping.size() > 1) {
            throw new UnsupportedOperationException("Multiple Location policies not yet supported");
        }

        String groupName = groupsLocationsMapping.entrySet().iterator().next().getKey();
        if (!Objects.equals(groupName, AlienConstants.GROUP_ALL)) {
            throw new IllegalArgumentException("Group name should be <" + AlienConstants.GROUP_ALL + ">, as we do not yet support multiple Location policies.");
        }
    }

    @EventListener
    @Order(20) // We must process location copy before copy of elements that depends from the location.
    public void onCopyConfiguration(OnDeploymentConfigCopyEvent onDeploymentConfigCopyEvent) {
        ApplicationEnvironment source = onDeploymentConfigCopyEvent.getSourceEnvironment();
        DeploymentMatchingConfiguration sourceConfiguration = deploymentConfigurationDao.findById(DeploymentMatchingConfiguration.class,
                AbstractDeploymentConfig.generateId(source.getTopologyVersion(), source.getId()));

        if (sourceConfiguration == null || MapUtils.isEmpty(sourceConfiguration.getLocationGroups())) {
            return; // Nothing to copy
        }

        // Set the location policy to the target environment.
        setLocationPolicy(onDeploymentConfigCopyEvent.getTargetEnvironment(), sourceConfiguration.getOrchestratorId(), sourceConfiguration.getLocationIds());
    }
}
