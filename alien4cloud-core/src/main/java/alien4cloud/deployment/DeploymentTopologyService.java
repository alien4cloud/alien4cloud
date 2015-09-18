package alien4cloud.deployment;

import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import javax.annotation.Resource;
import javax.inject.Inject;

import org.apache.commons.collections4.MapUtils;
import org.springframework.stereotype.Service;

import alien4cloud.application.ApplicationEnvironmentService;
import alien4cloud.application.ApplicationVersionService;
import alien4cloud.common.AlienConstants;
import alien4cloud.dao.IGenericSearchDAO;
import alien4cloud.exception.NotFoundException;
import alien4cloud.model.application.ApplicationEnvironment;
import alien4cloud.model.application.ApplicationVersion;
import alien4cloud.model.deployment.DeploymentTopology;
import alien4cloud.model.orchestrators.locations.Location;
import alien4cloud.model.topology.AbstractPolicy;
import alien4cloud.model.topology.LocationPlacementPolicy;
import alien4cloud.model.topology.NodeGroup;
import alien4cloud.model.topology.Topology;
import alien4cloud.orchestrators.locations.services.LocationService;
import alien4cloud.security.AuthorizationUtil;
import alien4cloud.security.model.DeployerRole;
import alien4cloud.topology.TopologyServiceCore;
import alien4cloud.topology.TopologyUtils;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

/**
 * Manages the deployment topology handling.
 */
@Service
public class DeploymentTopologyService {

    @Resource(name = "alien-es-dao")
    private IGenericSearchDAO alienDAO;

    @Inject
    private TopologyServiceCore topologyServiceCore;

    @Inject
    private ApplicationVersionService appVersionService;

    @Inject
    private ApplicationEnvironmentService appEnvironmentServices;

    @Inject
    private LocationService locationService;

    @Inject
    private ApplicationVersionService applicationVersionService;

    @Inject
    private ApplicationEnvironmentService applicationEnvironmentService;

    /**
     * Get the deployment topology for a given version and environment.
     *
     * @param versionId The id of the version for which to get the deployment topology.
     * @param environmentId The id of the environment for which to get the deployment topology.
     * @return The deployment topology for the given version and environment.
     */
    public DeploymentTopology getDeploymentTopology(String versionId, String environmentId) {
        DeploymentTopology deploymentTopology = alienDAO.findById(DeploymentTopology.class, generateId(versionId, environmentId));
        if (deploymentTopology == null) {
            throw new NotFoundException("Unable to find the deployment topology for version <" + versionId + "> and environment <" + environmentId + ">");
        }
        return deploymentTopology;
    }

    /**
     * Get the deployment topology for a given an environment id.
     *
     * @param environmentId The id of the environment for which to get the deployment topology.
     * @return The deployment topology for the given version and environment.
     */
    public DeploymentTopology getOrFail(String environmentId) {
        ApplicationEnvironment environment = appEnvironmentServices.getOrFail(environmentId);
        DeploymentTopology deploymentTopology = alienDAO.findById(DeploymentTopology.class, generateId(environment.getCurrentVersionId(), environmentId));
        if (deploymentTopology == null) {
            throw new NotFoundException("Unable to find the deployment topology for environment <" + environmentId + ">");
        }
        return deploymentTopology;
    }

    /**
     * Generate the id of a deployment setup.
     *
     * @param versionId The id of the version of the deployment setup.
     * @param environmentId The id of the environment of the deployment setup.
     * @return The generated id.
     */
    private String generateId(String versionId, String environmentId) {
        return versionId + "::" + environmentId;
    }

    /**
     * Set the location policies of a deloyment
     *
     * @param environmentId
     * @param groupsToLocations
     * @return
     */
    public DeploymentTopology setLocationPolicies(String environmentId, Map<String, String> groupsToLocations) {

        ApplicationEnvironment environment = appEnvironmentServices.getOrFail(environmentId);
        ApplicationVersion appVersion = appVersionService.getOrFail(environment.getCurrentVersionId());

        // TODO What to do if the deploymentTopo do not yet exists?
        DeploymentTopology deploymentTopo = getOrCreateDeploymentTopology(environment);
        deploymentTopo.setInitialTopologyId(appVersion.getTopologyId());
        addLocationPolicies(deploymentTopo, groupsToLocations);

        alienDAO.save(deploymentTopo);
        return deploymentTopo;
    }

    /**
     * Get or create if not yet existing the {@link DeploymentTopology}
     *
     * @param environment
     * @return
     */
    private DeploymentTopology getOrCreateDeploymentTopology(ApplicationEnvironment environment) {
        String id = generateId(environment.getCurrentVersionId(), environment.getId());
        DeploymentTopology deploymentTopo = alienDAO.findById(DeploymentTopology.class, id);
        if (deploymentTopo == null) {
            deploymentTopo = new DeploymentTopology();
            deploymentTopo.setVersionId(environment.getCurrentVersionId());
            deploymentTopo.setEnvironmentId(environment.getId());
            deploymentTopo.setId(id);
            alienDAO.save(deploymentTopo);
        }
        return deploymentTopo;
    }

    /**
     * Get or create if not yet existing the {@link DeploymentTopology}
     *
     * @param environmentId
     * @return
     */
    public DeploymentTopology getOrCreateDeploymentTopology(String environmentId) {
        ApplicationEnvironment environment = appEnvironmentServices.getOrFail(environmentId);
        return getOrCreateDeploymentTopology(environment);
    }

    /**
     * Add location policies in the deploymentTopology
     *
     * @param deploymentTopo
     * @param groupsLocationsMapping
     */
    private void addLocationPolicies(DeploymentTopology deploymentTopo, Map<String, String> groupsLocationsMapping) {

        if (MapUtils.isEmpty(groupsLocationsMapping)) {
            return;
        }

        // TODO For now, we only support one location policy for all nodes. So we have a group _ALL that represents all compute nodes in the topology
        // To improve later on for multiple groups support
        // throw an exception if multiple location policies provided: not yet supported
        if (groupsLocationsMapping.size() > 1) {
            throw new UnsupportedOperationException("Multiple Location policies not yet supported");
        }

        Topology topology = topologyServiceCore.getOrFail(deploymentTopo.getInitialTopologyId());

        for (Entry<String, String> matchEntry : groupsLocationsMapping.entrySet()) {

            String locationId = matchEntry.getValue();
            checkAuthorizationOnLocation(locationId);
            LocationPlacementPolicy locationPolicy = new LocationPlacementPolicy(locationId);
            locationPolicy.setName("Location policy");

            // put matchEntry.getKey() instead for multi location support
            String groupName = AlienConstants.GROUP_ALL;

            Map<String, NodeGroup> groups = deploymentTopo.getGroups();
            if (groups == null) {
                groups = Maps.newHashMap();
                deploymentTopo.setGroups(groups);
            }

            NodeGroup group = new NodeGroup();
            group.setName(groupName);
            group.setIndex(TopologyUtils.getAvailableGroupIndex(topology));
            group.setPolicies(Lists.<AbstractPolicy> newArrayList());
            group.getPolicies().add(locationPolicy);

            // Should we add all members here? if so, how to sync the deploymentTopology and the initial one?
            groups.put(groupName, group);
        }

    }

    private void checkAuthorizationOnLocation(String locationId) {
        Location location = locationService.getOrFail(locationId);
        AuthorizationUtil.checkAuthorizationForLocation(location, DeployerRole.values());
    }

    /**
     * Get all deployment setup linked to a topology
     *
     * @param topologyId the topology id
     * @return all deployment setup that is linked to this topology
     */
    public DeploymentTopology[] getByTopologyId(String topologyId) {
        List<DeploymentTopology> deploymentTopologies = org.elasticsearch.common.collect.Lists.newArrayList();
        ApplicationVersion version = applicationVersionService.getByTopologyId(topologyId);
        if (version != null) {
            ApplicationEnvironment[] environments = applicationEnvironmentService.getByVersionId(version.getId());
            if (environments != null && environments.length > 0) {
                for (ApplicationEnvironment environment : environments) {
                    deploymentTopologies.add(getDeploymentTopology(version.getId(), environment.getId()));
                }
            }
        }
        return deploymentTopologies.toArray(new DeploymentTopology[deploymentTopologies.size()]);
    }
}
