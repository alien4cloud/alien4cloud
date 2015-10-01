package alien4cloud.deployment;

import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import javax.annotation.Resource;
import javax.inject.Inject;

import lombok.extern.slf4j.Slf4j;

import org.apache.commons.collections4.MapUtils;
import org.elasticsearch.index.query.QueryBuilders;
import org.springframework.stereotype.Service;

import alien4cloud.application.ApplicationEnvironmentService;
import alien4cloud.application.ApplicationVersionService;
import alien4cloud.application.TopologyCompositionService;
import alien4cloud.common.AlienConstants;
import alien4cloud.dao.IGenericSearchDAO;
import alien4cloud.exception.NotFoundException;
import alien4cloud.model.application.ApplicationEnvironment;
import alien4cloud.model.application.ApplicationVersion;
import alien4cloud.model.components.AbstractPropertyValue;
import alien4cloud.model.deployment.DeploymentTopology;
import alien4cloud.model.orchestrators.locations.Location;
import alien4cloud.model.orchestrators.locations.LocationResourceTemplate;
import alien4cloud.model.topology.AbstractPolicy;
import alien4cloud.model.topology.LocationPlacementPolicy;
import alien4cloud.model.topology.NodeGroup;
import alien4cloud.model.topology.NodeTemplate;
import alien4cloud.model.topology.Topology;
import alien4cloud.orchestrators.locations.services.LocationResourceService;
import alien4cloud.orchestrators.locations.services.LocationService;
import alien4cloud.security.AuthorizationUtil;
import alien4cloud.security.model.DeployerRole;
import alien4cloud.topology.TopologyServiceCore;
import alien4cloud.utils.ReflectionUtil;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

/**
 * Manages the deployment topology handling.
 */
@Service
@Slf4j
public class DeploymentTopologyService {
    @Resource(name = "alien-es-dao")
    private IGenericSearchDAO alienDAO;
    @Inject
    private ApplicationVersionService appVersionService;
    @Inject
    private ApplicationEnvironmentService appEnvironmentServices;
    @Inject
    private LocationService locationService;
    @Inject
    private LocationResourceService locationResourceService;
    @Inject
    private ApplicationVersionService applicationVersionService;
    @Inject
    private ApplicationEnvironmentService applicationEnvironmentService;
    @Inject
    private InputsPreProcessorService inputsPreProcessorService;
    @Inject
    private DeploymentInputService deploymentInputService;
    @Inject
    private TopologyCompositionService topologyCompositionService;
    @Inject
    private TopologyServiceCore topologyServiceCore;
    @Inject
    private DeploymentNodeSubstitutionService deploymentNodeSubstitutionService;

    public void save(DeploymentTopology deploymentTopology) {
        deploymentTopology.setLastUpdateDate(new Date());
        alienDAO.save(deploymentTopology);
    }

    /**
     * Get or create if not yet existing the {@link DeploymentTopology}. This method will check if the initial topology has been updated, if so it will try to
     * re-synchronize the topology and the deployment topology
     *
     * @param environment the environment
     * @return the related or created deployment topology
     */
    private DeploymentTopology getOrCreateDeploymentTopology(ApplicationEnvironment environment, String topologyId) {
        String id = generateId(environment.getCurrentVersionId(), environment.getId());
        DeploymentTopology deploymentTopology = alienDAO.findById(DeploymentTopology.class, id);
        Topology topology = topologyServiceCore.getOrFail(topologyId);
        if (deploymentTopology == null) {
            // Generate the deployment topology if none exist
            deploymentTopology = generateDeploymentTopology(environment, topology);
        } else if (deploymentTopology.getLastInitialTopologyUpdateDate().before(topology.getLastUpdateDate())) {
            // Re-generate the deployment topology if the initial topology has been changed
            generateDeploymentTopology(environment, topology, deploymentTopology);
        }
        return deploymentTopology;
    }

    private DeploymentTopology generateDeploymentTopology(ApplicationEnvironment environment, Topology topology) {
        DeploymentTopology deploymentTopology = new DeploymentTopology();
        return generateDeploymentTopology(environment, topology, deploymentTopology);
    }

    private DeploymentTopology generateDeploymentTopology(ApplicationEnvironment environment, Topology topology, DeploymentTopology deploymentTopology) {
        deploymentTopology.setVersionId(environment.getCurrentVersionId());
        deploymentTopology.setEnvironmentId(environment.getId());
        deploymentTopology.setInitialTopologyId(topology.getId());
        updateAndSaveDeploymentTopology(deploymentTopology, topology, environment);
        return deploymentTopology;
    }

    /**
     * Deployment configuration has been changed, in this case must re-synchronize the deployment topology
     *
     * @param deploymentTopology the deployment topology to update
     */
    public void updateAndSaveDeploymentTopology(DeploymentTopology deploymentTopology) {
        ApplicationEnvironment environment = appEnvironmentServices.getOrFail(deploymentTopology.getEnvironmentId());
        Topology topology = topologyServiceCore.getOrFail(deploymentTopology.getInitialTopologyId());
        updateAndSaveDeploymentTopology(deploymentTopology, topology, environment);
    }

    private void updateAndSaveDeploymentTopology(DeploymentTopology deploymentTopology, Topology topology, ApplicationEnvironment environment) {
        deploymentTopology.setLastInitialTopologyUpdateDate(topology.getLastUpdateDate());
        ReflectionUtil.mergeObject(topology, deploymentTopology);
        processDeploymentTopologyAndSave(deploymentTopology, environment);
    }

    private void processDeploymentTopologyAndSave(DeploymentTopology deploymentTopology, ApplicationEnvironment environment) {
        topologyCompositionService.processTopologyComposition(deploymentTopology);
        deploymentInputService.processInputProperties(deploymentTopology);
        inputsPreProcessorService.processGetInput(deploymentTopology, environment);
        deploymentInputService.processInputArtifacts(deploymentTopology);
        deploymentInputService.processProviderDeploymentProperties(deploymentTopology);
        deploymentNodeSubstitutionService.processNodesSubstitution(deploymentTopology);
        save(deploymentTopology);
    }

    /**
     * Process the deployment topology and save it. This should always be called when the deployment setup has changed
     *
     * @param deploymentTopology
     * @param environment
     */
    public void processDeploymentTopologyAndSave(DeploymentTopology deploymentTopology) {
        ApplicationEnvironment environment = appEnvironmentServices.getOrFail(deploymentTopology.getEnvironmentId());
        processDeploymentTopologyAndSave(deploymentTopology, environment);
    }

    private LocationResourceTemplate getSubstitution(DeploymentTopology deploymentTopology, String nodeId) {
        if (MapUtils.isEmpty(deploymentTopology.getSubstitutedNodes())) {
            throw new NotFoundException("Topology does not have any node substituted");
        }
        LocationResourceTemplate substitution = deploymentTopology.getSubstitutedNodes().get(nodeId);
        if (substitution == null) {
            throw new NotFoundException("Topology does not have the node <" + nodeId + "> substituted");
        }
        return substitution;
    }

    public void updateSubstitutionProperty(DeploymentTopology deploymentTopology, String nodeId, String propertyName, Object propertyValue) {
        LocationResourceTemplate substitution = getSubstitution(deploymentTopology, nodeId);
        NodeTemplate substituted = deploymentTopology.getNodeTemplates().get(nodeId);
        locationResourceService.setTemplateProperty(substitution, propertyName, propertyValue);
        if (substituted.getProperties() == null) {
            substituted.setProperties(Maps.<String, AbstractPropertyValue> newHashMap());
        }
        substituted.getProperties().put(propertyName, substitution.getTemplate().getProperties().get(propertyName));
        processDeploymentTopologyAndSave(deploymentTopology);
    }

    public void updateSubstitutionCapabilityProperty(DeploymentTopology deploymentTopology, String nodeId, String capabilityName, String propertyName,
            Object propertyValue) {
        LocationResourceTemplate substitution = getSubstitution(deploymentTopology, nodeId);
        NodeTemplate substituted = deploymentTopology.getNodeTemplates().get(nodeId);
        locationResourceService.setTemplateCapabilityProperty(substitution, capabilityName, propertyName, propertyValue);
        Map<String, AbstractPropertyValue> substitutedCapabilityProperties = substituted.getCapabilities().get(capabilityName).getProperties();
        if (substitutedCapabilityProperties == null) {
            substitutedCapabilityProperties = Maps.newHashMap();
            substituted.getCapabilities().get(capabilityName).setProperties(substitutedCapabilityProperties);
        }
        substitutedCapabilityProperties.put(propertyName, substitution.getTemplate().getCapabilities().get(capabilityName).getProperties().get(propertyName));
        processDeploymentTopologyAndSave(deploymentTopology);
    }

    public void deleteByEnvironmentId(String environmentId) {
        alienDAO.delete(DeploymentTopology.class, QueryBuilders.termQuery("environmentId", environmentId));
    }

    /**
     * Generate the id of a deployment topology.
     *
     * @param versionId The id of the version of the deployment topology.
     * @param environmentId The id of the environment of the deployment topology.
     * @return The generated id.
     */
    private String generateId(String versionId, String environmentId) {
        return versionId + "::" + environmentId;
    }

    /**
     * Set the location policies of a deployment
     *
     * @param environmentId the environment's id
     * @param groupsToLocations group to location mapping
     * @return the updated deployment topology
     */
    public DeploymentTopology setLocationPolicies(String environmentId, String orchestratorId, Map<String, String> groupsToLocations) {
        // Change of locations will trigger re-generation of deployment topology
        // Set to new locations and process generation of all default properties
        ApplicationEnvironment environment = appEnvironmentServices.getOrFail(environmentId);
        ApplicationVersion appVersion = appVersionService.getOrFail(environment.getCurrentVersionId());
        DeploymentTopology deploymentTopology = new DeploymentTopology();
        deploymentTopology.setOrchestratorId(orchestratorId);
        addLocationPolicies(deploymentTopology, groupsToLocations);
        Topology topology = topologyServiceCore.getOrFail(appVersion.getTopologyId());
        generateDeploymentTopology(environment, topology, deploymentTopology);
        return deploymentTopology;
    }

    /**
     * Get or create if not yet existing the {@link DeploymentTopology}
     *
     * @param environmentId environment's id
     * @return the existing deployment topology or new created one
     */
    public DeploymentTopology getOrCreateDeploymentTopology(String environmentId) {
        ApplicationEnvironment environment = appEnvironmentServices.getOrFail(environmentId);
        ApplicationVersion version = applicationVersionService.getOrFail(environment.getCurrentVersionId());
        return getOrCreateDeploymentTopology(environment, version.getTopologyId());
    }

    /**
     * Add location policies in the deploymentTopology
     *
     * @param deploymentTopology the deployment topology
     * @param groupsLocationsMapping the mapping group name to location policy
     */
    private void addLocationPolicies(DeploymentTopology deploymentTopology, Map<String, String> groupsLocationsMapping) {

        if (MapUtils.isEmpty(groupsLocationsMapping)) {
            return;
        }

        // TODO For now, we only support one location policy for all nodes. So we have a group _ALL that represents all compute nodes in the topology
        // To improve later on for multiple groups support
        // throw an exception if multiple location policies provided: not yet supported
        if (groupsLocationsMapping.size() > 1) {
            throw new UnsupportedOperationException("Multiple Location policies not yet supported");
        }

        for (Entry<String, String> matchEntry : groupsLocationsMapping.entrySet()) {
            String locationId = matchEntry.getValue();
            Location location = locationService.getOrFail(locationId);
            AuthorizationUtil.checkAuthorizationForLocation(location, DeployerRole.values());
            deploymentTopology.getLocationDependencies().addAll(location.getDependencies());
            LocationPlacementPolicy locationPolicy = new LocationPlacementPolicy(locationId);
            locationPolicy.setName("Location policy");
            // put matchEntry.getKey() instead for multi location support
            String groupName = AlienConstants.GROUP_ALL;
            Map<String, NodeGroup> groups = deploymentTopology.getLocationGroups();
            NodeGroup group = new NodeGroup();
            group.setName(groupName);
            group.setPolicies(Lists.<AbstractPolicy> newArrayList());
            group.getPolicies().add(locationPolicy);
            groups.put(groupName, group);
        }
    }

    /**
     * Get all deployment topology linked to a topology
     *
     * @param topologyId the topology id
     * @return all deployment topology that is linked to this topology
     */
    public DeploymentTopology[] getByTopologyId(String topologyId) {
        List<DeploymentTopology> deploymentTopologies = Lists.newArrayList();
        ApplicationVersion version = applicationVersionService.getByTopologyId(topologyId);
        if (version != null) {
            ApplicationEnvironment[] environments = applicationEnvironmentService.getByVersionId(version.getId());
            if (environments != null && environments.length > 0) {
                for (ApplicationEnvironment environment : environments) {
                    deploymentTopologies.add(getOrCreateDeploymentTopology(environment, version.getTopologyId()));
                }
            }
        }
        return deploymentTopologies.toArray(new DeploymentTopology[deploymentTopologies.size()]);
    }
}
