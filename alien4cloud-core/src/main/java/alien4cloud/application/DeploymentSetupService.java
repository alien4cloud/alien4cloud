package alien4cloud.application;

import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.annotation.Resource;

import lombok.AllArgsConstructor;

import org.elasticsearch.index.query.QueryBuilders;
import org.springframework.stereotype.Service;

import alien4cloud.cloud.CloudResourceMatcherService;
import alien4cloud.cloud.CloudResourceTopologyMatchResult;
import alien4cloud.cloud.CloudService;
import alien4cloud.dao.IGenericSearchDAO;
import alien4cloud.dao.model.GetMultipleDataResult;
import alien4cloud.exception.AlreadyExistException;
import alien4cloud.exception.NotFoundException;
import alien4cloud.model.application.ApplicationEnvironment;
import alien4cloud.model.application.ApplicationVersion;
import alien4cloud.model.application.DeploymentSetup;
import alien4cloud.model.cloud.Cloud;
import alien4cloud.model.cloud.CloudResourceMatcherConfig;
import alien4cloud.model.cloud.NetworkTemplate;
import alien4cloud.model.components.IndexedNodeType;
import alien4cloud.model.components.PropertyDefinition;
import alien4cloud.model.topology.Topology;
import alien4cloud.topology.TopologyServiceCore;

import com.google.common.collect.Maps;

/**
 * Manages deployment setups.
 */
@Service
public class DeploymentSetupService {

    @Resource(name = "alien-es-dao")
    private IGenericSearchDAO alienDAO;
    @Resource
    private CloudResourceMatcherService cloudResourceMatcherService;
    @Resource
    private TopologyServiceCore topologyServiceCore;
    @Resource
    private CloudService cloudService;
    @Resource
    private ApplicationVersionService applicationVersionService;
    @Resource
    private ApplicationEnvironmentService applicationEnvironmentService;

    public DeploymentSetup get(ApplicationVersion version, ApplicationEnvironment environment) {
        return getById(generateId(version.getId(), environment.getId()));
    }

    private DeploymentSetup getById(String deploymentSetupId) {
        return alienDAO.findById(DeploymentSetup.class, deploymentSetupId);
    }

    public DeploymentSetup getOrFail(ApplicationVersion version, ApplicationEnvironment environment) {
        DeploymentSetup setup = get(version, environment);
        if (setup == null) {
            throw new NotFoundException("No setup found for version [" + version.getId() + "] and environment [" + environment.getId() + "]");
        } else {
            return setup;
        }
    }

    /**
     * Create a deployment setup with a failure when the composed key (version.id, environment.id) already exists
     * 
     * @param version
     * @param environment
     * @return the created deployment setup
     */
    public DeploymentSetup createOrFail(ApplicationVersion version, ApplicationEnvironment environment) {
        // check if deploymentSetup already exists
        DeploymentSetup deploymentSetup = get(version, environment);
        if (deploymentSetup == null) {
            deploymentSetup = new DeploymentSetup();
            deploymentSetup.setId(generateId(version.getId(), environment.getId()));
            deploymentSetup.setEnvironmentId(environment.getId());
            deploymentSetup.setVersionId(version.getId());
            alienDAO.save(deploymentSetup);
        } else {
            throw new AlreadyExistException("A deployment setup for application <" + environment.getApplicationId() + "> for version [" + version.getId()
                    + "] / environment [" + environment.getId() + "] already exists");
        }
        return deploymentSetup;
    }

    /**
     * Get the deployment setup
     * (environment right check done before method call)
     * 
     * @param applicationId
     * @param applicationEnvironmentId
     * @return
     */
    public DeploymentSetup getDeploymentSetup(String applicationId, String applicationEnvironmentId) {

        // get the topology from the version and the cloud from the environment
        ApplicationEnvironment environment = applicationEnvironmentService.getEnvironmentByIdOrDefault(applicationId, applicationEnvironmentId);
        ApplicationVersion version = applicationVersionService.getVersionByIdOrDefault(applicationId, environment.getCurrentVersionId());

        DeploymentSetup deploymentSetup = get(version, environment);
        if (deploymentSetup == null) {
            deploymentSetup = createOrFail(version, environment);
        }
        if (environment.getCloudId() != null) {
            Cloud cloud = cloudService.getMandatoryCloud(environment.getCloudId());
            generateCloudResourcesMapping(deploymentSetup, topologyServiceCore.getMandatoryTopology(version.getTopologyId()), cloud, true);
        }
        return deploymentSetup;
    }

    /**
     * Try to generate resources mapping for deployment setup from the topology and the cloud.
     * If no value has been chosen this method will generate default value.
     * If existing configuration is no longer valid for the topology and the cloud, this method will correct incompatibility
     * 
     * @param deploymentSetup the deployment setup to generate configuration for
     * @param topology the topology
     * @param cloud the cloud
     * @param automaticSave automatically save the deployment setup if it has been changed
     * @return true if the topology's deployment setup is valid (all resources are matchable), false otherwise
     */
    public boolean generateCloudResourcesMapping(DeploymentSetup deploymentSetup, Topology topology, Cloud cloud, boolean automaticSave) {
        CloudResourceMatcherConfig cloudResourceMatcherConfig = cloudService.findCloudResourceMatcherConfig(cloud);
        Map<String, IndexedNodeType> types = topologyServiceCore.getIndexedNodeTypesFromTopology(topology, false, true);
        CloudResourceTopologyMatchResult matchResult = cloudResourceMatcherService.matchTopology(topology, cloud, cloudResourceMatcherConfig, types);

        // Generate default matching for compute template
        MappingGenerationResult<ComputeTemplate> computeMapping = generateDefaultMapping(deploymentSetup.getCloudResourcesMapping(),
                matchResult.getComputeMatchResult(), topology);

        // Generate default matching for network
        MappingGenerationResult<NetworkTemplate> networkMapping = generateDefaultMapping(deploymentSetup.getNetworkMapping(), matchResult.getNetworkMatchResult(),
                topology);

        deploymentSetup.setCloudResourcesMapping(computeMapping.mapping);
        deploymentSetup.setNetworkMapping(networkMapping.mapping);
        if ((computeMapping.changed || networkMapping.changed) && automaticSave) {
            alienDAO.save(deploymentSetup);
        }
        return computeMapping.valid && networkMapping.valid;
    }

    @AllArgsConstructor
    private static class MappingGenerationResult<T> {
        private Map<String, T> mapping;
        private boolean valid;
        private boolean changed;
    }

    private <T> MappingGenerationResult<T> generateDefaultMapping(Map<String, T> mapping, Map<String, List<T>> matchResult, Topology topology) {
        boolean valid = true;
        for (Map.Entry<String, List<T>> matchResultEntry : matchResult.entrySet()) {
            valid = valid && (matchResultEntry.getValue() != null && !matchResultEntry.getValue().isEmpty());
        }
        boolean changed = false;
        if (mapping == null) {
            changed = true;
            mapping = Maps.newHashMap();
        } else {
            // Try to remove unknown mapping from existing config
            Iterator<Map.Entry<String, T>> mappingEntryIterator = mapping.entrySet().iterator();
            while (mappingEntryIterator.hasNext()) {
                Map.Entry<String, T> entry = mappingEntryIterator.next();
                if (topology.getNodeTemplates() == null || !topology.getNodeTemplates().containsKey(entry.getKey()) || !matchResult.containsKey(entry.getKey())
                        || !matchResult.get(entry.getKey()).contains(entry.getValue())) {
                    // Remove the mapping if topology do not contain the node with that name and of type compute
                    // Or the mapping do not exist anymore in the match result
                    changed = true;
                    mappingEntryIterator.remove();
                }
            }
        }
        for (Map.Entry<String, List<T>> entry : matchResult.entrySet()) {
            if (!entry.getValue().isEmpty() && !mapping.containsKey(entry.getKey())) {
                // Only take the first element as selected if no configuration has been set before
                changed = true;
                mapping.put(entry.getKey(), entry.getValue().get(0));
            }
        }
        return new MappingGenerationResult<>(mapping, valid, changed);
    }

    /**
     * Try to generate a default deployment properties
     * 
     * @param deploymentSetup the deployment setup to generate configuration for
     * @param cloud the cloud
     */
    public void generatePropertyDefinition(DeploymentSetup deploymentSetup, Cloud cloud) {
        Map<String, PropertyDefinition> propertyDefinitionMap = cloudService.getDeploymentPropertyDefinitions(cloud.getId());
        if (propertyDefinitionMap != null) {
            // Reset deployment properties as it might have changed between cloud
            Map<String, String> propertyValueMap = Maps.newHashMap();
            for (Map.Entry<String, PropertyDefinition> propertyDefinitionEntry : propertyDefinitionMap.entrySet()) {
                propertyValueMap.put(propertyDefinitionEntry.getKey(), propertyDefinitionEntry.getValue().getDefault());
            }
            deploymentSetup.setProviderDeploymentProperties(propertyValueMap);
        }
    }

    public String generateId(String versionId, String environmentId) {
        return versionId + "::" + environmentId;
    }

    /**
     * Delete a deployment setup based on the id of the related environment.
     *
     * @param environmentId The id of the environment
     */
    public void deleteByEnvironmentId(String environmentId) {
        alienDAO.delete(DeploymentSetup.class, QueryBuilders.termQuery("environmentId", environmentId));
    }

    /**
     * Delete a deployment setup based on the id of the related version.
     *
     * @param versionId The id of the version
     */
    public void deleteByVersionId(String versionId) {
        alienDAO.delete(DeploymentSetup.class, QueryBuilders.termQuery("versionId", versionId));
    }

    /**
     * Get all deployments setup based on the id of the related version.
     *
     * @param versionId The id of the version
     */
    public GetMultipleDataResult<DeploymentSetup> getByVersionId(String versionId) {
        Map<String, String[]> filters = Maps.newHashMap();
        filters.put("versionId", new String[] { versionId });
        return alienDAO.search(DeploymentSetup.class, null, filters, 0, 20);
    }

    /**
     * Get a topology Id for a deploymentSetup
     * 
     * @param deploymentId
     * @return a topology id
     */
    public String getTopologyId(String deploymentSetupId) {
        DeploymentSetup deploymentSetup = getById(deploymentSetupId);
        if (deploymentSetup != null) {
            ApplicationEnvironment applicationEnvironment = applicationEnvironmentService.getOrFail(deploymentSetup.getEnvironmentId());
            ApplicationVersion applicationVersion = applicationVersionService.getOrFail(applicationEnvironment.getCurrentVersionId());
            return applicationVersion.getTopologyId();
        }
        return null;
    }

    /**
     * Get the linked application environment
     * 
     * @param deploymentSetupId
     * @return an application environment
     */
    public ApplicationEnvironment getApplicationEnvironment(String deploymentSetupId) {
        DeploymentSetup deploymentSetup = getById(deploymentSetupId);
        if (deploymentSetup != null) {
            ApplicationEnvironment applicationEnvironment = applicationEnvironmentService.getOrFail(deploymentSetup.getEnvironmentId());
            return applicationEnvironment;
        }
        return null;
    }

    /**
     * Get the linked application version
     * 
     * @param deploymentSetupId
     * @return an application version
     */
    public ApplicationVersion getApplicationVersion(String deploymentSetupId) {
        DeploymentSetup deploymentSetup = getById(deploymentSetupId);
        deploymentSetup.setProviderDeploymentProperties(null);
        if (deploymentSetup != null) {
            ApplicationEnvironment applicationEnvironment = applicationEnvironmentService.getOrFail(deploymentSetup.getEnvironmentId());
            ApplicationVersion applicationVersion = applicationVersionService.getOrFail(applicationEnvironment.getCurrentVersionId());
            return applicationVersion;
        }
        return null;
    }

}
