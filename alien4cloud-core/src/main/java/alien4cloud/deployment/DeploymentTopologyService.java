package alien4cloud.deployment;

import java.beans.IntrospectionException;
import java.util.*;
import java.util.Map.Entry;

import javax.annotation.Resource;
import javax.inject.Inject;

import org.alien4cloud.tosca.model.definitions.AbstractPropertyValue;
import org.alien4cloud.tosca.model.definitions.PropertyDefinition;
import org.alien4cloud.tosca.model.definitions.ScalarPropertyValue;
import org.alien4cloud.tosca.model.definitions.constraints.EqualConstraint;
import org.alien4cloud.tosca.model.templates.*;
import org.alien4cloud.tosca.model.types.CapabilityType;
import org.apache.commons.collections4.MapUtils;
import org.elasticsearch.index.query.QueryBuilders;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;

import alien4cloud.application.ApplicationEnvironmentService;
import alien4cloud.application.ApplicationVersionService;
import alien4cloud.application.TopologyCompositionService;
import alien4cloud.common.AlienConstants;
import alien4cloud.dao.IGenericSearchDAO;
import alien4cloud.deployment.matching.services.location.TopologyLocationUtils;
import alien4cloud.deployment.model.DeploymentConfiguration;
import alien4cloud.deployment.model.DeploymentSubstitutionConfiguration;
import alien4cloud.exception.NotFoundException;
import alien4cloud.model.application.ApplicationEnvironment;
import alien4cloud.model.application.ApplicationVersion;
import alien4cloud.model.deployment.DeploymentTopology;
import alien4cloud.model.orchestrators.locations.Location;
import alien4cloud.model.orchestrators.locations.LocationResourceTemplate;
import alien4cloud.orchestrators.locations.services.ILocationResourceService;
import alien4cloud.orchestrators.locations.services.LocationService;
import alien4cloud.security.AuthorizationUtil;
import alien4cloud.security.model.DeployerRole;
import alien4cloud.topology.TopologyServiceCore;
import alien4cloud.tosca.properties.constraints.ConstraintUtil;
import alien4cloud.tosca.properties.constraints.exception.ConstraintTechnicalException;
import alien4cloud.tosca.properties.constraints.exception.ConstraintValueDoNotMatchPropertyTypeException;
import alien4cloud.tosca.properties.constraints.exception.ConstraintViolationException;
import alien4cloud.utils.ReflectionUtil;
import alien4cloud.utils.services.PropertyService;
import lombok.extern.slf4j.Slf4j;

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
    @Lazy(true)
    private ILocationResourceService locationResourceService;
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
    private IDeploymentNodeSubstitutionService deploymentNodeSubstitutionService;
    @Inject
    private PropertyService propertyService;

    public void save(DeploymentTopology deploymentTopology) {
        deploymentTopology.setLastDeploymentTopologyUpdateDate(new Date());
        alienDAO.save(deploymentTopology);
    }

    /**
     * Get a deployment topology from it's id or throw a NotFoundException if none exists for this id.
     *
     * @param id The id of the deployment topology to get.
     * @return The deployment topology matching the given id.
     */
    public DeploymentTopology getOrFail(String id) {
        DeploymentTopology deploymentTopology = alienDAO.findById(DeploymentTopology.class, id);
        if (deploymentTopology == null) {
            throw new NotFoundException("Deployment topology [" + id + "] doesn't exists.");
        }
        return deploymentTopology;
    }

    /**
     * Get or create if not yet existing the {@link DeploymentTopology} for the given environment.
     *
     * @param environmentId The environment for which to get or create a {@link DeploymentTopology}
     * @return the existing {@link DeploymentTopology} or new created one
     */
    public DeploymentTopology getDeploymentTopology(String environmentId) {
        ApplicationEnvironment environment = appEnvironmentServices.getOrFail(environmentId);
        ApplicationVersion version = applicationVersionService.getOrFail(environment.getCurrentVersionId());
        return getOrCreateDeploymentTopology(environment, version.getId());
    }

    /**
     * Get or create if not yet existing the {@link DeploymentTopology}. This method will check if the initial topology has been updated, if so it will try to
     * re-synchronize the topology and the deployment topology
     *
     * @param environment the environment
     * @return the related or created deployment topology
     */
    private DeploymentTopology getOrCreateDeploymentTopology(ApplicationEnvironment environment, String topologyId) {
        String id = DeploymentTopology.generateId(environment.getCurrentVersionId(), environment.getId());
        DeploymentTopology deploymentTopology = alienDAO.findById(DeploymentTopology.class, id);
        Topology topology = topologyServiceCore.getOrFail(topologyId);
        if (deploymentTopology == null) {
            deploymentTopology = generateDeploymentTopology(id, environment, topology, new DeploymentTopology());
        } else {
            Map<String, String> locationIds = TopologyLocationUtils.getLocationIds(deploymentTopology);
            boolean locationsInvalid = false;
            Map<String, Location> locations = Maps.newHashMap();
            if (!MapUtils.isEmpty(locationIds)) {
                try {
                    locations = getLocations(locationIds);
                } catch (NotFoundException ignored) {
                    locationsInvalid = true;
                }
            }
            if (locationsInvalid) {
                // Generate the deployment topology if none exist or if locations are not valid anymore
                deploymentTopology = generateDeploymentTopology(id, environment, topology, new DeploymentTopology());
            } else if (checkIfTopologyOrLocationHasChanged(deploymentTopology, locations.values(), topology)) {
                // Re-generate the deployment topology if the initial topology has been changed
                generateDeploymentTopology(id, environment, topology, deploymentTopology);
            }
        }
        return deploymentTopology;
    }

    private boolean checkIfTopologyOrLocationHasChanged(DeploymentTopology deploymentTopology, Collection<Location> locations, Topology topology) {
        if (deploymentTopology.getLastDeploymentTopologyUpdateDate().before(topology.getLastUpdateDate())) {
            return true;
        }
        for (Location location : locations) {
            if (deploymentTopology.getLastDeploymentTopologyUpdateDate().before(location.getLastUpdateDate())) {
                return true;
            }
        }
        return false;
    }

    public DeploymentConfiguration getDeploymentConfiguration(String environmentId) {
        DeploymentTopology deploymentTopology = getDeploymentTopology(environmentId);
        return getDeploymentConfiguration(deploymentTopology);
    }

    public DeploymentConfiguration getDeploymentConfiguration(DeploymentTopology deploymentTopology) {
        DeploymentSubstitutionConfiguration substitutionConfiguration = getAvailableNodeSubstitutions(deploymentTopology);
        Map<String, Set<String>> availableSubstitutions = substitutionConfiguration.getAvailableSubstitutions();
        Map<String, String> existingSubstitutions = deploymentTopology.getSubstitutedNodes();
        // Handle the case when new resources added
        // TODO In the case when resource is updated / deleted on the location we should update everywhere where they are used
        if (availableSubstitutions.size() != existingSubstitutions.size()) {
            updateDeploymentTopology(deploymentTopology);
        }
        return new DeploymentConfiguration(deploymentTopology, substitutionConfiguration);
    }

    private DeploymentSubstitutionConfiguration getAvailableNodeSubstitutions(DeploymentTopology deploymentTopology) {
        Map<String, List<LocationResourceTemplate>> availableSubstitutions = deploymentNodeSubstitutionService.getAvailableSubstitutions(deploymentTopology);
        DeploymentSubstitutionConfiguration dsc = new DeploymentSubstitutionConfiguration();
        Map<String, Set<String>> availableSubstitutionsIds = Maps.newHashMap();
        Map<String, LocationResourceTemplate> templates = Maps.newHashMap();
        for (Map.Entry<String, List<LocationResourceTemplate>> availableSubstitutionsEntry : availableSubstitutions.entrySet()) {
            Set<String> existingIds = availableSubstitutionsIds.get(availableSubstitutionsEntry.getKey());
            if (existingIds == null) {
                existingIds = Sets.newHashSet();
                availableSubstitutionsIds.put(availableSubstitutionsEntry.getKey(), existingIds);
            }
            for (LocationResourceTemplate template : availableSubstitutionsEntry.getValue()) {
                existingIds.add(template.getId());
                templates.put(template.getId(), template);
            }
        }
        dsc.setAvailableSubstitutions(availableSubstitutionsIds);
        dsc.setSubstitutionsTemplates(templates);
        dsc.setSubstitutionTypes(locationResourceService.getLocationResourceTypes(templates.values()));
        return dsc;
    }

    private DeploymentTopology generateDeploymentTopology(String id, ApplicationEnvironment environment, Topology topology,
            DeploymentTopology deploymentTopology) {
        // TODO first check the initial topology is valid before doing this
        deploymentTopology.setVersionId(environment.getCurrentVersionId());
        deploymentTopology.setEnvironmentId(environment.getId());
        deploymentTopology.setInitialTopologyId(topology.getId());
        deploymentTopology.setId(id);
        doUpdateDeploymentTopology(deploymentTopology, topology, environment);
        return deploymentTopology;
    }

    /**
     * Deployment configuration has been changed, in this case must re-synchronize the deployment topology
     *
     * @param deploymentTopology the deployment topology to update
     */
    public void updateDeploymentTopology(DeploymentTopology deploymentTopology) {
        ApplicationEnvironment environment = appEnvironmentServices.getOrFail(deploymentTopology.getEnvironmentId());
        Topology topology = topologyServiceCore.getOrFail(deploymentTopology.getInitialTopologyId());
        doUpdateDeploymentTopology(deploymentTopology, topology, environment);
    }

    private void doUpdateDeploymentTopology(DeploymentTopology deploymentTopology, Topology topology, ApplicationEnvironment environment) {
        Map<String, NodeTemplate> previousNodeTemplates = deploymentTopology.getNodeTemplates();
        ReflectionUtil.mergeObject(topology, deploymentTopology, "id");
        topologyCompositionService.processTopologyComposition(deploymentTopology);
        deploymentInputService.processInputProperties(deploymentTopology);
        inputsPreProcessorService.processGetInput(deploymentTopology, environment, topology);
        deploymentInputService.processProviderDeploymentProperties(deploymentTopology);
        deploymentNodeSubstitutionService.processNodesSubstitution(deploymentTopology, previousNodeTemplates);
        save(deploymentTopology);
    }

    /**
     * Update the deployment topology's input and save it. This should always be called when the deployment setup has changed
     *
     * @param deploymentTopology the the deployment topology
     */
    public void updateDeploymentTopologyInputsAndSave(DeploymentTopology deploymentTopology) {
        ApplicationEnvironment environment = appEnvironmentServices.getOrFail(deploymentTopology.getEnvironmentId());
        Topology topology = topologyServiceCore.getOrFail(deploymentTopology.getInitialTopologyId());
        deploymentInputService.processInputProperties(deploymentTopology);
        inputsPreProcessorService.processGetInput(deploymentTopology, environment, topology);
        deploymentInputService.processProviderDeploymentProperties(deploymentTopology);
        save(deploymentTopology);
    }

    /**
     * Update the value of a property.
     *
     * @param environmentId The id of the environment for which to update the deployment topology.
     * @param nodeTemplateId The id of the node template to update (this must be a substituted node).
     * @param propertyName The name of the property for which to update the value.
     * @param propertyValue The new value of the property.
     */
    public void updateProperty(String environmentId, String nodeTemplateId, String propertyName, Object propertyValue)
            throws ConstraintViolationException, ConstraintValueDoNotMatchPropertyTypeException {
        DeploymentConfiguration deploymentConfiguration = getDeploymentConfiguration(environmentId);
        DeploymentTopology deploymentTopology = deploymentConfiguration.getDeploymentTopology();
        // It is not allowed to override a value from an original node or from a location resource.
        NodeTemplate substitutedNode = deploymentTopology.getNodeTemplates().get(nodeTemplateId);
        if (substitutedNode == null) {
            throw new NotFoundException(
                    "The deployment topology <" + deploymentTopology.getId() + "> doesn't contains any node with id <" + nodeTemplateId + ">");
        }
        String substitutionId = deploymentTopology.getSubstitutedNodes().get(nodeTemplateId);
        if (substitutionId == null) {
            throw new NotFoundException("The node <" + nodeTemplateId + "> from deployment topology <" + deploymentTopology.getId() + "> is not substituted");
        }
        LocationResourceTemplate locationResourceTemplate = deploymentConfiguration.getAvailableSubstitutions().getSubstitutionsTemplates().get(substitutionId);
        PropertyDefinition propertyDefinition = deploymentConfiguration.getAvailableSubstitutions().getSubstitutionTypes().getNodeTypes()
                .get(locationResourceTemplate.getTemplate().getType()).getProperties().get(propertyName);
        if (propertyDefinition == null) {
            throw new NotFoundException("No property of name <" + propertyName + "> can be found on the node template <" + nodeTemplateId + "> of type <"
                    + locationResourceTemplate.getTemplate().getType() + ">");
        }

        AbstractPropertyValue locationResourcePropertyValue = locationResourceTemplate.getTemplate().getProperties().get(propertyName);
        buildConstaintException(locationResourcePropertyValue, propertyDefinition, "by the admin in the Location Resource Template", propertyName,
                propertyValue);
        NodeTemplate originalNode = deploymentTopology.getOriginalNodes().get(nodeTemplateId);
        buildConstaintException(originalNode.getProperties().get(propertyName), propertyDefinition, "in the portable topology", propertyName, propertyValue);

        // Set the value and check constraints
        propertyService.setPropertyValue(substitutedNode, propertyDefinition, propertyName, propertyValue);
        alienDAO.save(deploymentTopology);
    }

    public void updateCapabilityProperty(String environmentId, String nodeTemplateId, String capabilityName, String propertyName, Object propertyValue)
            throws ConstraintViolationException, ConstraintValueDoNotMatchPropertyTypeException {
        DeploymentConfiguration deploymentConfiguration = getDeploymentConfiguration(environmentId);
        DeploymentTopology deploymentTopology = deploymentConfiguration.getDeploymentTopology();

        // It is not allowed to override a value from an original node or from a location resource.
        NodeTemplate substitutedNode = deploymentTopology.getNodeTemplates().get(nodeTemplateId);
        if (substitutedNode == null) {
            throw new NotFoundException(
                    "The deployment topology <" + deploymentTopology.getId() + "> doesn't contains any node with id <" + nodeTemplateId + ">");
        }
        String substitutionId = deploymentTopology.getSubstitutedNodes().get(nodeTemplateId);
        if (substitutionId == null) {
            throw new NotFoundException("The node <" + nodeTemplateId + "> from deployment topology <" + deploymentTopology.getId() + "> is not substituted");
        }

        LocationResourceTemplate locationResourceTemplate = deploymentConfiguration.getAvailableSubstitutions().getSubstitutionsTemplates().get(substitutionId);
        Capability locationResourceCapability = locationResourceTemplate.getTemplate().getCapabilities().get(capabilityName);
        if (locationResourceCapability == null) {
            throw new NotFoundException("The capability <" + capabilityName + "> cannot be found on node template <" + nodeTemplateId + "> of type <"
                    + locationResourceTemplate.getTemplate().getType() + ">");
        }
        CapabilityType capabilityType = deploymentConfiguration.getAvailableSubstitutions().getSubstitutionTypes().getCapabilityTypes()
                .get(locationResourceCapability.getType());
        PropertyDefinition propertyDefinition = capabilityType.getProperties().get(propertyName);
        if (propertyDefinition == null) {
            throw new NotFoundException("No property with name <" + propertyName + "> can be found on capability <" + capabilityName + "> of type <"
                    + locationResourceCapability.getType() + ">");
        }

        AbstractPropertyValue locationResourcePropertyValue = locationResourceTemplate.getTemplate().getCapabilities().get(capabilityName).getProperties()
                .get(propertyName);
        buildConstaintException(locationResourcePropertyValue, propertyDefinition, "by the admin in the Location Resource Template", propertyName,
                propertyValue);
        AbstractPropertyValue originalNodePropertyValue = deploymentTopology.getOriginalNodes().get(nodeTemplateId).getCapabilities().get(capabilityName)
                .getProperties().get(propertyName);
        buildConstaintException(originalNodePropertyValue, propertyDefinition, "in the portable topology", propertyName, propertyValue);

        // Set the value and check constraints
        propertyService.setCapabilityPropertyValue(substitutedNode.getCapabilities().get(capabilityName), propertyDefinition, propertyName, propertyValue);
        alienDAO.save(deploymentTopology);
    }

    /**
     * Check that the property is not already defined in a source
     *
     * @param sourcePropertyValue null or an already defined Property Value.
     * @param messageSource The named source to add in the exception message in case of failure.
     */
    private void buildConstaintException(AbstractPropertyValue sourcePropertyValue, PropertyDefinition propertyDefinition, String messageSource,
            String propertyName, Object propertyValue) throws ConstraintViolationException {
        if (sourcePropertyValue != null) {
            try {
                EqualConstraint constraint = new EqualConstraint();
                if (sourcePropertyValue instanceof ScalarPropertyValue) {
                    constraint.setEqual(((ScalarPropertyValue) sourcePropertyValue).getValue());
                }
                ConstraintUtil.ConstraintInformation information = ConstraintUtil.getConstraintInformation(constraint);
                // If admin has defined a value users should not be able to override it.
                throw new ConstraintViolationException("Overriding value specified " + messageSource + " is not authorized.", null, information);
            } catch (IntrospectionException e) {
                // ConstraintValueDoNotMatchPropertyTypeException is not supposed to be raised here (only in constraint definition validation)
                log.info("Constraint introspection error for property <" + propertyName + "> value <" + propertyValue + ">", e);
                throw new ConstraintTechnicalException("Constraint introspection error for property <" + propertyName + "> value <" + propertyValue + ">", e);
            }
        }
    }

    public void deleteByEnvironmentId(String environmentId) {
        alienDAO.delete(DeploymentTopology.class, QueryBuilders.termQuery("environmentId", environmentId));
    }

    /**
     * Set the location policies of a deployment
     *
     * @param environmentId the environment's id
     * @param groupsToLocations group to location mapping
     * @return the updated deployment topology
     */
    public DeploymentConfiguration setLocationPolicies(String environmentId, String orchestratorId, Map<String, String> groupsToLocations) {
        // Change of locations will trigger re-generation of deployment topology
        // Set to new locations and process generation of all default properties
        ApplicationEnvironment environment = appEnvironmentServices.getOrFail(environmentId);
        ApplicationVersion appVersion = appVersionService.getOrFail(environment.getCurrentVersionId());

        DeploymentTopology oldDT = alienDAO.findById(DeploymentTopology.class, DeploymentTopology.generateId(appVersion.getId(), environmentId));

        DeploymentTopology deploymentTopology = new DeploymentTopology();
        deploymentTopology.setOrchestratorId(orchestratorId);
        addLocationPolicies(deploymentTopology, groupsToLocations);

        if (oldDT != null) {
            // we should keep input properties
            deploymentTopology.setInputProperties(oldDT.getInputProperties());
            if (deploymentTopology.getOrchestratorId().equals(oldDT.getOrchestratorId())) {
                // and orchestrator properties if not changed.
                deploymentTopology.setProviderDeploymentProperties(oldDT.getProviderDeploymentProperties());
            }
        }

        Topology topology = topologyServiceCore.getOrFail(appVersion.getId());
        generateDeploymentTopology(DeploymentTopology.generateId(appVersion.getId(), environmentId), environment, topology, deploymentTopology);
        return getDeploymentConfiguration(deploymentTopology);
    }

    /**
     * Get location map from the deployment topology
     *
     * @param deploymentTopology the deploymentTopology
     * @return map of location group id to location
     */
    public Map<String, Location> getLocations(DeploymentTopology deploymentTopology) {
        Map<String, String> locationIds = TopologyLocationUtils.getLocationIdsOrFail(deploymentTopology);
        return getLocations(locationIds);
    }

    /**
     * Get location map from the deployment topology
     *
     * @param locationIds map of group id to location id
     * @return map of location group id to location
     */
    public Map<String, Location> getLocations(Map<String, String> locationIds) {
        Map<String, Location> locations = locationService.getMultiple(locationIds.values());
        Map<String, Location> locationMap = Maps.newHashMap();
        for (Map.Entry<String, String> locationIdsEntry : locationIds.entrySet()) {
            locationMap.put(locationIdsEntry.getKey(), locations.get(locationIdsEntry.getValue()));
        }
        if (locations.size() < locationIds.size()) {
            throw new NotFoundException("Some locations could not be found " + locationIds);
        }
        return locationMap;
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

        // TODO For now, we only support one location policy for all nodes. So we have a group _A4C_ALL that represents all compute nodes in the topology
        // To improve later on for multiple groups support
        // throw an exception if multiple location policies provided: not yet supported
        // throw an exception if group name is not _A4C_ALL
        checkGroups(groupsLocationsMapping);

        for (Entry<String, String> matchEntry : groupsLocationsMapping.entrySet()) {
            String locationId = matchEntry.getValue();
            Location location = locationService.getOrFail(locationId);
            AuthorizationUtil.checkAuthorizationForLocation(location, DeployerRole.values());
            deploymentTopology.getLocationDependencies().addAll(location.getDependencies());
            LocationPlacementPolicy locationPolicy = new LocationPlacementPolicy(locationId);
            locationPolicy.setName("Location policy");
            Map<String, NodeGroup> groups = deploymentTopology.getLocationGroups();
            NodeGroup group = new NodeGroup();
            group.setName(matchEntry.getKey());
            group.setPolicies(Lists.<AbstractPolicy> newArrayList());
            group.getPolicies().add(locationPolicy);
            groups.put(matchEntry.getKey(), group);
        }
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
                    deploymentTopologies.add(getOrCreateDeploymentTopology(environment, version.getId()));
                }
            }
        }
        return deploymentTopologies.toArray(new DeploymentTopology[deploymentTopologies.size()]);
    }

    /**
     * Finalize the deployment topology processing and get it ready to deploy
     *
     * @param deploymentTopology
     * @return
     */
    public DeploymentTopology processForDeployment(DeploymentTopology deploymentTopology) {
        // if a property defined as getInput didn't found a value after processing, set it to null
        inputsPreProcessorService.setUnprocessedGetInputToNullValue(deploymentTopology);
        return deploymentTopology;
    }

    /**
     *
     * Update a chosen substitution for a node
     *
     * @param environmentId
     * @param nodeId
     * @param locationResourceTemplateId
     * @return The {@link DeploymentTopologyService} related to the specified environment
     */
    public DeploymentConfiguration updateSubstitution(String environmentId, String nodeId, String locationResourceTemplateId) {
        // TODO maybe check if the substituted is compatible with the provided substitute and return a specific error for REST users?
        DeploymentConfiguration deploymentConfiguration = getDeploymentConfiguration(environmentId);
        DeploymentTopology deploymentTopology = deploymentConfiguration.getDeploymentTopology();
        // check if the resource exists
        locationResourceService.getOrFail(locationResourceTemplateId);
        deploymentTopology.getSubstitutedNodes().put(nodeId, locationResourceTemplateId);
        // revert the old substituted to the original one. It will be updated when processing the substitutions in updateDeploymentTopology
        deploymentTopology.getNodeTemplates().put(nodeId, deploymentTopology.getOriginalNodes().get(nodeId));
        updateDeploymentTopology(deploymentTopology);
        return deploymentConfiguration;
    }
}
