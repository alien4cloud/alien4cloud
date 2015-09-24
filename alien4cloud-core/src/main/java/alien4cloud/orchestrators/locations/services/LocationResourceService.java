package alien4cloud.orchestrators.locations.services;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import javax.annotation.Resource;
import javax.inject.Inject;

import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.springframework.stereotype.Component;

import alien4cloud.component.ICSARRepositorySearchService;
import alien4cloud.dao.IGenericSearchDAO;
import alien4cloud.dao.model.GetMultipleDataResult;
import alien4cloud.exception.NotFoundException;
import alien4cloud.model.components.CSARDependency;
import alien4cloud.model.components.CapabilityDefinition;
import alien4cloud.model.components.IndexedCapabilityType;
import alien4cloud.model.components.IndexedNodeType;
import alien4cloud.model.components.IndexedToscaElement;
import alien4cloud.model.components.PropertyDefinition;
import alien4cloud.model.orchestrators.Orchestrator;
import alien4cloud.model.orchestrators.locations.Location;
import alien4cloud.model.orchestrators.locations.LocationResourceTemplate;
import alien4cloud.model.orchestrators.locations.LocationResources;
import alien4cloud.model.topology.Capability;
import alien4cloud.model.topology.NodeTemplate;
import alien4cloud.orchestrators.plugin.ILocationConfiguratorPlugin;
import alien4cloud.orchestrators.plugin.ILocationResourceAccessor;
import alien4cloud.orchestrators.plugin.IOrchestratorPlugin;
import alien4cloud.orchestrators.services.OrchestratorService;
import alien4cloud.paas.OrchestratorPluginService;
import alien4cloud.topology.TopologyServiceCore;
import alien4cloud.utils.MapUtil;
import alien4cloud.utils.PropertyUtil;
import alien4cloud.utils.ReflectionUtil;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;

/**
 * Location Resource Service provides utilities to query LocationResourceTemplate.
 */
@Component
public class LocationResourceService {
    @Resource(name = "alien-es-dao")
    private IGenericSearchDAO alienDAO;
    @Inject
    private ICSARRepositorySearchService csarRepoSearchService;
    @Inject
    private TopologyServiceCore topologyService;
    @Inject
    private LocationService locationService;
    @Inject
    private OrchestratorService orchestratorService;
    @Inject
    private OrchestratorPluginService orchestratorPluginService;

    /**
     * Get the list of resources definitions for a given orchestrator.
     *
     * @param location the location.
     * @return A list of resource definitions for the given location.
     */
    public LocationResources getLocationResources(Location location) {
        Orchestrator orchestrator = orchestratorService.getOrFail(location.getOrchestratorId());
        IOrchestratorPlugin orchestratorInstance = (IOrchestratorPlugin) orchestratorPluginService.getOrFail(orchestrator.getId());
        ILocationConfiguratorPlugin configuratorPlugin = orchestratorInstance.getConfigurator(location.getInfrastructureType());
        List<String> allExposedTypes = configuratorPlugin.getResourcesTypes();
        Set<CSARDependency> dependencies = location.getDependencies();
        Map<String, IndexedNodeType> configurationsTypes = Maps.newHashMap();
        Map<String, IndexedNodeType> nodesTypes = Maps.newHashMap();
        Map<String, IndexedCapabilityType> capabilityTypes = Maps.newHashMap();
        for (String exposedType : allExposedTypes) {
            IndexedNodeType exposedIndexedNodeType = csarRepoSearchService.getRequiredElementInDependencies(IndexedNodeType.class, exposedType, dependencies);
            if (exposedIndexedNodeType.isAbstract()) {
                configurationsTypes.put(exposedType, exposedIndexedNodeType);
            } else {
                nodesTypes.put(exposedType, exposedIndexedNodeType);
            }
            if (exposedIndexedNodeType.getCapabilities() != null && !exposedIndexedNodeType.getCapabilities().isEmpty()) {
                for (CapabilityDefinition capabilityDefinition : exposedIndexedNodeType.getCapabilities()) {
                    capabilityTypes.put(capabilityDefinition.getType(),
                            csarRepoSearchService.getRequiredElementInDependencies(IndexedCapabilityType.class, capabilityDefinition.getType(), dependencies));
                }
            }
        }
        List<LocationResourceTemplate> locationResourceTemplates = getResourcesTemplates(location.getId());
        LocationResources locationResources = new LocationResources();
        locationResources.setConfigurationTypes(configurationsTypes);
        locationResources.setNodeTypes(nodesTypes);
        List<LocationResourceTemplate> configurationsTemplates = Lists.newArrayList();
        List<LocationResourceTemplate> nodesTemplates = Lists.newArrayList();
        for (LocationResourceTemplate resourceTemplate : locationResourceTemplates) {
            String templateType = resourceTemplate.getTemplate().getType();
            if (configurationsTypes.containsKey(templateType)) {
                configurationsTemplates.add(resourceTemplate);
            }
            if (nodesTypes.containsKey(templateType)) {
                nodesTemplates.add(resourceTemplate);
            }
        }
        locationResources.setConfigurationTemplates(configurationsTemplates);
        locationResources.setNodeTemplates(nodesTemplates);
        locationResources.setCapabilityTypes(capabilityTypes);
        return locationResources;
    }

    public LocationResourceTypes getLocationResourceTypes(Collection<LocationResourceTemplate> resourceTemplates) {
        Map<String, Set<String>> resourceTypesByLocationId = Maps.newHashMap();
        for (LocationResourceTemplate resourceTemplate : resourceTemplates) {
            Set<String> locationResourceTypes = resourceTypesByLocationId.get(resourceTemplate.getLocationId());
            if (locationResourceTypes == null) {
                locationResourceTypes = Sets.newHashSet();
                resourceTypesByLocationId.put(resourceTemplate.getLocationId(), locationResourceTypes);
            }
            locationResourceTypes.add(resourceTemplate.getTemplate().getType());
        }
        LocationResourceTypes locationResourceTypes = new LocationResourceTypes();
        for (Map.Entry<String, Set<String>> resourceTypeByLocationIdEntry : resourceTypesByLocationId.entrySet()) {
            String locationId = resourceTypeByLocationIdEntry.getKey();
            Set<String> exposedTypes = resourceTypeByLocationIdEntry.getValue();
            Location location = locationService.getOrFail(locationId);
            for (String exposedType : exposedTypes) {
                IndexedNodeType exposedIndexedNodeType = csarRepoSearchService.getRequiredElementInDependencies(IndexedNodeType.class, exposedType,
                        location.getDependencies());
                locationResourceTypes.getNodeTypes().put(exposedType, exposedIndexedNodeType);
                if (exposedIndexedNodeType.getCapabilities() != null && !exposedIndexedNodeType.getCapabilities().isEmpty()) {
                    for (CapabilityDefinition capabilityDefinition : exposedIndexedNodeType.getCapabilities()) {
                        locationResourceTypes.getCapabilityTypes().put(capabilityDefinition.getType(),
                                csarRepoSearchService.getRequiredElementInDependencies(IndexedCapabilityType.class, capabilityDefinition.getType(),
                                        location.getDependencies()));
                    }
                }
            }
        }
        return locationResourceTypes;
    }

    /**
     * Get the list of resources definitions for a given orchestrator.
     *
     * @param locationId the location's id
     * @return A list of resource definitions for the given location.
     */
    public LocationResources getLocationResources(String locationId) {
        return getLocationResources(locationService.getOrFail(locationId));
    }

    /**
     * Create an instance of an ILocationResourceAccessor that will perform queries on LocationResourceTemplate for a given location.
     *
     * @param locationId Id of the location for which to get the accessor.
     * @return An instance of the ILocationResourceAccessor.
     */
    public ILocationResourceAccessor accessor(final String locationId) {
        return new ILocationResourceAccessor() {
            private Location location = locationService.getOrFail(locationId);

            @Override
            public List<LocationResourceTemplate> getResources() {
                return getResourcesTemplates(locationId);
            }

            @Override
            public List<LocationResourceTemplate> getResources(String type) {
                // Get all types that derives from the current type.
                String[] types = new String[] { type };
                // Get all the location resources templates for the given type.
                Map<String, String[]> filter = getLocationIdFilter(locationId);
                filter.put("types", types);
                return getResourcesTemplates(filter);
            }

            @Override
            public <T extends IndexedToscaElement> T getIndexedToscaElement(String type) {
                return (T) csarRepoSearchService.getRequiredElementInDependencies(IndexedToscaElement.class, type, location.getDependencies());
            }

            @Override
            public Set<CSARDependency> getDependencies() {
                return location.getDependencies();
            }
        };
    }

    private Map<String, String[]> getLocationIdFilter(String locationId) {
        return MapUtil.newHashMap(new String[] { "locationId" }, new String[][] { new String[] { locationId } });
    }

    private List<LocationResourceTemplate> getResourcesTemplates(Map<String, String[]> filter) {
        // get all defined resources for this resource.
        GetMultipleDataResult<LocationResourceTemplate> result = alienDAO.find(LocationResourceTemplate.class, filter, Integer.MAX_VALUE);
        if (result.getData() == null) {
            return Lists.newArrayList();
        }
        return Lists.newArrayList(result.getData());
    }

    public List<LocationResourceTemplate> getResourcesTemplates(String locationId) {
        return getResourcesTemplates(getLocationIdFilter(locationId));
    }

    public LocationResourceTemplate addResourceTemplate(String locationId, String resourceName, String resourceTypeName) {
        Location location = locationService.getOrFail(locationId);
        IndexedNodeType resourceType = csarRepoSearchService.getRequiredElementInDependencies(IndexedNodeType.class, resourceTypeName,
                location.getDependencies());
        NodeTemplate nodeTemplate = topologyService.buildNodeTemplate(location.getDependencies(), resourceType, null);
        LocationResourceTemplate locationResourceTemplate = new LocationResourceTemplate();
        locationResourceTemplate.setName(resourceName);
        locationResourceTemplate.setEnabled(true);
        locationResourceTemplate.setGenerated(false);
        locationResourceTemplate.setId(UUID.randomUUID().toString());
        locationResourceTemplate.setLocationId(locationId);
        locationResourceTemplate.setService(false);
        locationResourceTemplate.setTypes(Lists.<String> newArrayList(resourceType.getElementId()));
        locationResourceTemplate.getTypes().addAll(resourceType.getDerivedFrom());
        locationResourceTemplate.setTemplate(nodeTemplate);
        alienDAO.save(locationResourceTemplate);
        return locationResourceTemplate;
    }

    public void deleteResourceTemplate(String resourceId) {
        alienDAO.delete(LocationResourceTemplate.class, resourceId);
    }

    public LocationResourceTemplate getOrFail(String resourceId) {
        return alienDAO.findById(LocationResourceTemplate.class, resourceId);
    }

    public void merge(Object mergeRequest, String resourceId) {
        LocationResourceTemplate resourceTemplate = getOrFail(resourceId);
        ReflectionUtil.mergeObject(mergeRequest, resourceTemplate);
        alienDAO.save(resourceTemplate);
    }

    public void setTemplateProperty(String resourceId, String propertyName, Object propertyValue) {
        LocationResourceTemplate resourceTemplate = getOrFail(resourceId);
        Location location = locationService.getOrFail(resourceTemplate.getLocationId());
        IndexedNodeType resourceType = csarRepoSearchService.getRequiredElementInDependencies(IndexedNodeType.class, resourceTemplate.getTemplate().getType(),
                location.getDependencies());
        if (resourceType.getProperties() == null || !resourceType.getProperties().containsKey(propertyName)) {
            throw new NotFoundException("Property <" + propertyName + "> is not found in type <" + resourceType.getElementId() + ">");
        }
        PropertyUtil.setPropertyValue(resourceTemplate.getTemplate(), resourceType.getProperties().get(propertyName), propertyName, propertyValue);
        alienDAO.save(resourceTemplate);
    }

    public void setTemplateCapabilityProperty(String resourceId, String capabilityName, String propertyName, Object propertyValue) {
        LocationResourceTemplate resourceTemplate = getOrFail(resourceId);
        Location location = locationService.getOrFail(resourceTemplate.getLocationId());
        IndexedNodeType resourceType = csarRepoSearchService.getRequiredElementInDependencies(IndexedNodeType.class, resourceTemplate.getTemplate().getType(),
                location.getDependencies());
        if (resourceTemplate.getTemplate().getCapabilities() == null || !resourceTemplate.getTemplate().getCapabilities().containsKey(capabilityName)) {
            throw new NotFoundException("Capability <" + capabilityName + "> is not found in template");

        }
        PropertyDefinition propertyDefinition = null;
        if (resourceType.getCapabilities() != null) {
            for (CapabilityDefinition capabilityDefinition : resourceType.getCapabilities()) {
                if (capabilityName.equals(capabilityDefinition.getId())) {
                    String capabilityTypeName = capabilityDefinition.getType();
                    IndexedCapabilityType capabilityType = csarRepoSearchService.getRequiredElementInDependencies(IndexedCapabilityType.class,
                            capabilityTypeName, location.getDependencies());
                    if (capabilityType.getProperties() != null) {
                        propertyDefinition = capabilityType.getProperties().get(propertyName);
                    }
                }
            }
        }
        if (propertyDefinition == null) {
            throw new NotFoundException("Capability <" + capabilityName + "> is not found in type <" + resourceType.getElementId() + ">");
        }
        Capability capability = resourceTemplate.getTemplate().getCapabilities().get(capabilityName);
        PropertyUtil.setCapabilityPropertyValue(capability, propertyDefinition, propertyName, propertyValue);
        alienDAO.save(resourceTemplate);
    }

    /**
     * Auto configure resources for the given location.
     *
     * @param locationId Id of the location.
     */
    public List<LocationResourceTemplate> autoConfigureResources(String locationId) {
        return locationService.autoConfigure(locationId);
    }

    /**
     * Delete all generated {@link LocationResourceTemplate} for a given location
     *
     * @param locationId
     */
    public void deleteGeneratedResources(String locationId) {
        QueryBuilder locationIdQuery = QueryBuilders.termQuery("locationId", locationId);
        QueryBuilder generatedFieldQuery = QueryBuilders.termQuery("generated", true);
        // QueryBuilder builder = QueryBuilders.filteredQuery(locationIdQuery, filterBuilder);
        QueryBuilder builder = QueryBuilders.boolQuery().must(locationIdQuery).must(generatedFieldQuery);
        alienDAO.delete(LocationResourceTemplate.class, builder);
    }
}
