package alien4cloud.deployment;

import static alien4cloud.dao.FilterUtil.fromKeyValueCouples;
import static alien4cloud.utils.AlienUtils.safe;

import java.beans.IntrospectionException;
import java.io.IOException;
import java.io.InputStream;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Set;

import javax.annotation.Resource;
import javax.inject.Inject;

import org.alien4cloud.alm.events.BeforeApplicationEnvironmentDeleted;
import org.alien4cloud.alm.events.BeforeApplicationTopologyVersionDeleted;
import org.alien4cloud.tosca.catalog.index.IToscaTypeSearchService;
import org.alien4cloud.tosca.model.CSARDependency;
import org.alien4cloud.tosca.model.Csar;
import org.alien4cloud.tosca.model.definitions.AbstractPropertyValue;
import org.alien4cloud.tosca.model.definitions.DeploymentArtifact;
import org.alien4cloud.tosca.model.definitions.PropertyDefinition;
import org.alien4cloud.tosca.model.definitions.PropertyValue;
import org.alien4cloud.tosca.model.definitions.ScalarPropertyValue;
import org.alien4cloud.tosca.model.definitions.constraints.EqualConstraint;
import org.alien4cloud.tosca.model.templates.AbstractPolicy;
import org.alien4cloud.tosca.model.templates.Capability;
import org.alien4cloud.tosca.model.templates.LocationPlacementPolicy;
import org.alien4cloud.tosca.model.templates.NodeGroup;
import org.alien4cloud.tosca.model.templates.NodeTemplate;
import org.alien4cloud.tosca.model.templates.Topology;
import org.alien4cloud.tosca.model.types.CapabilityType;
import org.alien4cloud.tosca.model.types.NodeType;
import org.apache.commons.collections4.MapUtils;
import org.elasticsearch.index.query.QueryBuilders;
import org.springframework.context.annotation.Lazy;
import org.springframework.context.event.EventListener;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;

import alien4cloud.application.ApplicationEnvironmentService;
import alien4cloud.application.ApplicationVersionService;
import alien4cloud.application.TopologyCompositionService;
import alien4cloud.component.repository.ArtifactRepositoryConstants;
import alien4cloud.component.repository.IFileRepository;
import alien4cloud.dao.IGenericSearchDAO;
import alien4cloud.dao.model.GetMultipleDataResult;
import alien4cloud.deployment.matching.services.location.TopologyLocationUtils;
import alien4cloud.deployment.model.DeploymentConfiguration;
import alien4cloud.deployment.model.DeploymentSubstitutionConfiguration;
import alien4cloud.exception.NotFoundException;
import alien4cloud.model.application.ApplicationEnvironment;
import alien4cloud.model.application.ApplicationTopologyVersion;
import alien4cloud.model.deployment.DeploymentTopology;
import alien4cloud.model.orchestrators.locations.Location;
import alien4cloud.model.orchestrators.locations.LocationResourceTemplate;
import alien4cloud.model.service.ServiceResource;
import alien4cloud.orchestrators.locations.services.ILocationResourceService;
import alien4cloud.orchestrators.locations.services.LocationResourceTypes;
import alien4cloud.orchestrators.locations.services.LocationSecurityService;
import alien4cloud.orchestrators.locations.services.LocationService;
import alien4cloud.service.ServiceResourceService;
import alien4cloud.topology.TopologyServiceCore;
import alien4cloud.tosca.context.ToscaContext;
import alien4cloud.tosca.properties.constraints.ConstraintUtil;
import alien4cloud.tosca.properties.constraints.exception.ConstraintTechnicalException;
import alien4cloud.tosca.properties.constraints.exception.ConstraintValueDoNotMatchPropertyTypeException;
import alien4cloud.tosca.properties.constraints.exception.ConstraintViolationException;
import alien4cloud.utils.AlienConstants;
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
    private ApplicationEnvironmentService appEnvironmentServices;
    @Inject
    private LocationService locationService;
    @Inject
    @Lazy(true)
    private ILocationResourceService locationResourceService;
    @Inject
    private ApplicationVersionService applicationVersionService;
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
    @Inject
    private ServiceResourceService serviceResourceService;
    @Resource
    private LocationSecurityService locationSecurityService;
    @Inject
    private IToscaTypeSearchService toscaTypeSearchService;
    @Resource
    private IFileRepository artifactRepository;

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
        ApplicationTopologyVersion topologyVersion = applicationVersionService
                .getOrFail(Csar.createId(environment.getApplicationId(), environment.getVersion()), environment.getTopologyVersion());
        return getOrCreateDeploymentTopology(environment, topologyVersion.getArchiveId());
    }

    /**
     * Get or create if not yet existing the {@link DeploymentTopology}. This method will check if the initial topology has been updated, if so it will try to
     * re-synchronize the topology and the deployment topology
     *
     * @param environment the environment
     * @return the related or created deployment topology
     */
    private DeploymentTopology getOrCreateDeploymentTopology(final ApplicationEnvironment environment, final String topologyId) {
        String deploymentTopologyId = DeploymentTopology.generateId(Csar.createId(environment.getApplicationId(), environment.getTopologyVersion()),
                environment.getId());
        DeploymentTopology deploymentTopology = alienDAO.findById(DeploymentTopology.class, deploymentTopologyId);
        Topology topology = topologyServiceCore.getOrFail(topologyId);
        if (deploymentTopology == null) {
            deploymentTopology = generateDeploymentTopology(deploymentTopologyId, environment, topology, new DeploymentTopology());
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
                deploymentTopology = generateDeploymentTopology(deploymentTopologyId, environment, topology, new DeploymentTopology());
            } else if (checkIfTopologyOrLocationHasChanged(deploymentTopology, locations.values(), topology)) {
                // Re-generate the deployment topology if the initial topology has been changed
                generateDeploymentTopology(deploymentTopologyId, environment, topology, deploymentTopology);
            } else if (deploymentTopology.getLastDeploymentTopologyUpdateDate().before(deploymentTopology.getLastUpdateDate())) {
                // the deployment topology has been changed without synchronizing its content (by the BlockStorageEventHandler for example). Do it now
                doUpdateDeploymentTopology(deploymentTopology, topology, environment);
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
        // enrich types with those comming from services
        enrichSubstitutionTypesWithServicesDependencies(templates.values(), dsc.getSubstitutionTypes());
        return dsc;
    }

    /**
     * Enrich {@link LocationResourceTypes} adding types coming from on demand service resources.
     */
    private void enrichSubstitutionTypesWithServicesDependencies(Collection<LocationResourceTemplate> resourceTemplates,
            LocationResourceTypes locationResourceTypes) {
        Set<String> serviceTypes = Sets.newHashSet();
        Set<CSARDependency> dependencies = Sets.newHashSet();
        for (LocationResourceTemplate resourceTemplate : resourceTemplates) {
            if (resourceTemplate.isService()) {
                String serviceId = resourceTemplate.getId();
                ServiceResource serviceResource = serviceResourceService.getOrFail(serviceId);
                NodeType serviceType = toscaTypeSearchService.findOrFail(NodeType.class, serviceResource.getNodeInstance().getNodeTemplate().getType(),
                        serviceResource.getNodeInstance().getTypeVersion());
                serviceTypes.add(serviceResource.getNodeInstance().getNodeTemplate().getType());
                Csar csar = toscaTypeSearchService.getArchive(serviceType.getArchiveName(), serviceType.getArchiveVersion());
                if (csar.getDependencies() != null) {
                    dependencies.addAll(csar.getDependencies());
                }
                dependencies.add(new CSARDependency(csar.getName(), csar.getVersion()));
            }
        }
        locationResourceService.fillLocationResourceTypes(serviceTypes, locationResourceTypes, dependencies);
    }

    private DeploymentTopology generateDeploymentTopology(String id, ApplicationEnvironment environment, Topology topology,
            DeploymentTopology deploymentTopology) {
        // TODO first check the initial topology is valid before doing this
        deploymentTopology.setVersionId(Csar.createId(environment.getApplicationId(), environment.getTopologyVersion()));
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
        Topology topology = topologyServiceCore.getOrFail(deploymentTopology.getInitialTopologyId());
        ApplicationEnvironment environment = appEnvironmentServices.getOrFail(deploymentTopology.getEnvironmentId());
        doUpdateDeploymentTopology(deploymentTopology, topology, environment);
    }

    private void doUpdateDeploymentTopology(DeploymentTopology deploymentTopology, Topology topology, ApplicationEnvironment environment) {
        Map<String, NodeTemplate> previousNodeTemplates = deploymentTopology.getNodeTemplates();
        ReflectionUtil.mergeObject(topology, deploymentTopology, "id", "creationDate", "lastUpdateDate");
        deploymentTopology.setSubstitutionMapping(topology.getSubstitutionMapping());
        topologyCompositionService.processTopologyComposition(deploymentTopology);
        deploymentInputService.processInputProperties(deploymentTopology);
        deploymentInputService.processProviderDeploymentProperties(deploymentTopology);
        injectInputAndProcessSubstitutionIfNeeded(deploymentTopology, topology, environment, previousNodeTemplates);
        save(deploymentTopology);
    }

    /**
     * If a location has been selected, inject inputs, and process node substitutions
     * 
     * @param deploymentTopology
     * @param topology
     * @param environment
     * @param previousNodeTemplates
     */
    private void injectInputAndProcessSubstitutionIfNeeded(DeploymentTopology deploymentTopology, Topology topology, ApplicationEnvironment environment,
            Map<String, NodeTemplate> previousNodeTemplates) {
        if (MapUtils.isEmpty(deploymentTopology.getLocationGroups())) {
            // No location group is defined do nothing
            return;
        }
        // injects inputs before processing substitutions
        inputsPreProcessorService.injectInputValues(deploymentTopology, environment, topology);
        deploymentNodeSubstitutionService.processNodesSubstitution(deploymentTopology, previousNodeTemplates);
    }

    /**
     * Update the deployment topology's input and save it. This should always be called when the deployment setup has changed
     *
     * @param deploymentTopology the the deployment topology
     */
    public void updateDeploymentTopologyInputsAndSave(DeploymentTopology deploymentTopology) {
        deploymentInputService.processInputProperties(deploymentTopology);
        deploymentInputService.processProviderDeploymentProperties(deploymentTopology);
        injectInputAndProcessSubstitutionIfNeeded(deploymentTopology, topologyServiceCore.getOrFail(deploymentTopology.getInitialTopologyId()),
                appEnvironmentServices.getOrFail(deploymentTopology.getEnvironmentId()), null);
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
        try {
            ToscaContext.init(deploymentTopology.getDependencies());
            // It is not allowed to override a value from an original node or from a location resource.
            NodeTemplate substitutedNode = deploymentTopology.getNodeTemplates().get(nodeTemplateId);
            if (substitutedNode == null) {
                throw new NotFoundException(
                        "The deployment topology <" + deploymentTopology.getId() + "> doesn't contains any node with id <" + nodeTemplateId + ">");
            }
            String substitutionId = deploymentTopology.getSubstitutedNodes().get(nodeTemplateId);
            if (substitutionId == null) {
                throw new NotFoundException(
                        "The node <" + nodeTemplateId + "> from deployment topology <" + deploymentTopology.getId() + "> is not substituted");
            }
            LocationResourceTemplate locationResourceTemplate = deploymentConfiguration.getAvailableSubstitutions().getSubstitutionsTemplates()
                    .get(substitutionId);
            PropertyDefinition propertyDefinition = deploymentConfiguration.getAvailableSubstitutions().getSubstitutionTypes().getNodeTypes()
                    .get(locationResourceTemplate.getTemplate().getType()).getProperties().get(propertyName);
            if (propertyDefinition == null) {
                throw new NotFoundException("No property of name <" + propertyName + "> can be found on the node template <" + nodeTemplateId + "> of type <"
                        + locationResourceTemplate.getTemplate().getType() + ">");
            }

            AbstractPropertyValue locationResourcePropertyValue = locationResourceTemplate.getTemplate().getProperties().get(propertyName);
            buildConstaintException(locationResourcePropertyValue, "by the admin in the Location Resource Template", propertyName, propertyValue);
            NodeTemplate originalNode = deploymentTopology.getOriginalNodes().get(nodeTemplateId);
            buildConstaintException(originalNode.getProperties().get(propertyName), "in the portable topology", propertyName, propertyValue);

            // Set the value and check constraints
            propertyService.setPropertyValue(substitutedNode, propertyDefinition, propertyName, propertyValue);
            alienDAO.save(deploymentTopology);
        } finally {
            ToscaContext.destroy();
        }
    }

    public void updateCapabilityProperty(String environmentId, String nodeTemplateId, String capabilityName, String propertyName, Object propertyValue)
            throws ConstraintViolationException, ConstraintValueDoNotMatchPropertyTypeException {
        DeploymentConfiguration deploymentConfiguration = getDeploymentConfiguration(environmentId);
        DeploymentTopology deploymentTopology = deploymentConfiguration.getDeploymentTopology();

        try {
            ToscaContext.init(deploymentTopology.getDependencies());
            // It is not allowed to override a value from an original node or from a location resource.
            NodeTemplate substitutedNode = deploymentTopology.getNodeTemplates().get(nodeTemplateId);
            if (substitutedNode == null) {
                throw new NotFoundException(
                        "The deployment topology <" + deploymentTopology.getId() + "> doesn't contains any node with id <" + nodeTemplateId + ">");
            }
            String substitutionId = deploymentTopology.getSubstitutedNodes().get(nodeTemplateId);
            if (substitutionId == null) {
                throw new NotFoundException(
                        "The node <" + nodeTemplateId + "> from deployment topology <" + deploymentTopology.getId() + "> is not substituted");
            }

            LocationResourceTemplate locationResourceTemplate = deploymentConfiguration.getAvailableSubstitutions().getSubstitutionsTemplates()
                    .get(substitutionId);
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
            buildConstaintException(locationResourcePropertyValue, "by the admin in the Location Resource Template", propertyName, propertyValue);
            AbstractPropertyValue originalNodePropertyValue = deploymentTopology.getOriginalNodes().get(nodeTemplateId).getCapabilities().get(capabilityName)
                    .getProperties().get(propertyName);
            buildConstaintException(originalNodePropertyValue, "in the portable topology", propertyName, propertyValue);

            // Set the value and check constraints
            propertyService.setCapabilityPropertyValue(substitutedNode.getCapabilities().get(capabilityName), propertyDefinition, propertyName, propertyValue);
            alienDAO.save(deploymentTopology);
        } finally {
            ToscaContext.destroy();
        }
    }

    /**
     * Check that the property is not already defined in a source
     *
     * @param sourcePropertyValue null or an already defined Property Value.
     * @param messageSource The named source to add in the exception message in case of failure.
     */
    private void buildConstaintException(AbstractPropertyValue sourcePropertyValue, String messageSource, String propertyName, Object propertyValue)
            throws ConstraintViolationException {
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

    @EventListener
    public void handleDeleteTopologyVersion(BeforeApplicationTopologyVersionDeleted event) {
        alienDAO.delete(DeploymentTopology.class, QueryBuilders.termQuery("versionId", Csar.createId(event.getApplicationId(), event.getTopologyVersion())));
    }

    @EventListener
    public void handleDeleteEnvironment(BeforeApplicationEnvironmentDeleted event) {
        alienDAO.delete(DeploymentTopology.class, QueryBuilders.termQuery("environmentId", event.getApplicationEnvironmentId()));
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

        ApplicationTopologyVersion topologyVersion = applicationVersionService
                .getOrFail(Csar.createId(environment.getApplicationId(), environment.getVersion()), environment.getTopologyVersion());

        DeploymentTopology oldDT = alienDAO.findById(DeploymentTopology.class, DeploymentTopology.generateId(topologyVersion.getArchiveId(), environmentId));

        DeploymentTopology deploymentTopology = new DeploymentTopology();
        deploymentTopology.setOrchestratorId(orchestratorId);
        deploymentTopology.setEnvironmentId(environmentId);
        addLocationPolicies(deploymentTopology, groupsToLocations);

        if (oldDT != null) {
            // we should keep input properties
            deploymentTopology.setInputProperties(oldDT.getInputProperties());
            if (deploymentTopology.getOrchestratorId().equals(oldDT.getOrchestratorId())) {
                // and orchestrator properties if not changed.
                deploymentTopology.setProviderDeploymentProperties(oldDT.getProviderDeploymentProperties());
            }
        }

        Topology topology = topologyServiceCore.getOrFail(topologyVersion.getArchiveId());
        generateDeploymentTopology(DeploymentTopology.generateId(topologyVersion.getArchiveId(), environmentId), environment, topology, deploymentTopology);
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
            // AuthorizationUtil.checkAuthorizationForLocation(location, DeployerRole.values());
            locationSecurityService.checkAuthorisation(location, deploymentTopology.getEnvironmentId());
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
        GetMultipleDataResult<DeploymentTopology> result = alienDAO.buildQuery(DeploymentTopology.class)
                .setFilters(fromKeyValueCouples("versionId", topologyId)).prepareSearch().search(0, Integer.MAX_VALUE);
        if (result.getData() != null) {
            return result.getData();
        }
        return new DeploymentTopology[0];
    }

    /**
     * Finalize the deployment topology processing and get it ready to deploy
     *
     * @param deploymentTopology The deployment topology that will actually be deployed.
     * @param environment The environment for which to prepare the deployment topology.
     * @return
     */
    public Map<String, PropertyValue> processForDeployment(DeploymentTopology deploymentTopology, ApplicationEnvironment environment) {
        // if a property defined as getInput didn't found a value after processing, set it to null
        inputsPreProcessorService.setUnprocessedGetInputToNullValue(deploymentTopology);
        return inputsPreProcessorService.computeInputs(deploymentTopology, environment);
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
        DeploymentConfiguration deploymentConfiguration = getDeploymentConfiguration(environmentId);
        DeploymentTopology deploymentTopology = deploymentConfiguration.getDeploymentTopology();

        LocationResourceTemplate template = safe(deploymentConfiguration.getAvailableSubstitutions().getSubstitutionsTemplates())
                .get(locationResourceTemplateId);
        if (template == null) {
            // not matching anymore
            // WHY ACCESS DENIED
            throw new AccessDeniedException("The resource <" + locationResourceTemplateId + "> is not anymore a valid match");
        }

        // check if the resource exists
        if (template.isService()) {
            // check that this location has access to this service
            serviceResourceService.isLocationAuthorized(locationResourceTemplateId, template.getLocationId());
        } else {
            // check if the resource exists, and if the context has authorizations
            LocationResourceTemplate resource = locationResourceService.getOrFail(locationResourceTemplateId);
            locationSecurityService.checkAuthorisation(resource, environmentId);
        }
        deploymentTopology.getSubstitutedNodes().put(nodeId, locationResourceTemplateId);
        // revert the old substituted to the original one. It will be updated when processing the substitutions in updateDeploymentTopology
        deploymentTopology.getNodeTemplates().put(nodeId, deploymentTopology.getOriginalNodes().get(nodeId));
        updateDeploymentTopology(deploymentTopology);
        return deploymentConfiguration;
    }

    /**
     * Update an input artifact value in the deployment topology
     *
     * @param environmentId
     * @param inputArtifactId
     * @param artifactFile
     * @throws IOException
     */
    public void updateInputArtifact(String environmentId, String inputArtifactId, MultipartFile artifactFile) throws IOException {
        DeploymentTopology topology = getDeploymentTopology(environmentId);
        if (topology.getInputArtifacts() == null || !topology.getInputArtifacts().containsKey(inputArtifactId)) {
            throw new NotFoundException("Input Artifact with key [" + inputArtifactId + "] doesn't exist within the topology.");
        }
        Map<String, DeploymentArtifact> artifacts = topology.getUploadedInputArtifacts();
        if (artifacts == null) {
            artifacts = new HashMap<>();
            topology.setUploadedInputArtifacts(artifacts);
        }
        DeploymentArtifact artifact = artifacts.get(inputArtifactId);
        if (artifact == null) {
            artifact = new DeploymentArtifact();
            artifacts.put(inputArtifactId, artifact);
        } else if (ArtifactRepositoryConstants.ALIEN_ARTIFACT_REPOSITORY.equals(artifact.getArtifactRepository())) {
            artifactRepository.deleteFile(artifact.getArtifactRef());
        }
        try (InputStream artifactStream = artifactFile.getInputStream()) {
            String artifactFileId = artifactRepository.storeFile(artifactStream);
            artifact.setArtifactName(artifactFile.getOriginalFilename());
            artifact.setArtifactRef(artifactFileId);
            artifact.setArtifactRepository(ArtifactRepositoryConstants.ALIEN_ARTIFACT_REPOSITORY);
            save(topology);
        }
    }
}
