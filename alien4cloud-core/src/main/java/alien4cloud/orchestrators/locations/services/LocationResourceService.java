package alien4cloud.orchestrators.locations.services;

import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import javax.annotation.Resource;
import javax.inject.Inject;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.MapUtils;
import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;

import alien4cloud.component.ICSARRepositorySearchService;
import alien4cloud.dao.IGenericSearchDAO;
import alien4cloud.dao.model.GetMultipleDataResult;
import alien4cloud.events.LocationTemplateCreated;
import alien4cloud.exception.NotFoundException;
import alien4cloud.model.components.CSARDependency;
import alien4cloud.model.components.CapabilityDefinition;
import alien4cloud.model.components.IndexedCapabilityType;
import alien4cloud.model.components.IndexedModelUtils;
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
import alien4cloud.topology.TopologyUtils;
import alien4cloud.tosca.properties.constraints.exception.ConstraintValueDoNotMatchPropertyTypeException;
import alien4cloud.tosca.properties.constraints.exception.ConstraintViolationException;
import alien4cloud.utils.MapUtil;
import alien4cloud.utils.ReflectionUtil;
import alien4cloud.utils.services.PropertyService;

/**
 * Location Resource Service provides utilities to query LocationResourceTemplate.
 */
@Component("location-resource-service")
public class LocationResourceService implements ILocationResourceService {
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
    @Resource
    private PropertyService propertyService;
    @Inject
    private ApplicationContext applicationContext;

    /*
     * (non-Javadoc)
     * 
     * @see alien4cloud.orchestrators.locations.services.ILocationResourceService#getLocationResources(alien4cloud.model.orchestrators.locations.Location)
     */
    @Override
    public LocationResources getLocationResources(Location location) {
        Orchestrator orchestrator = orchestratorService.get(location.getOrchestratorId());
        if (orchestrator != null && orchestratorPluginService.get(orchestrator.getId()) != null) {
            return getLocationResourcesFromOrchestrator(location);
        }

        List<LocationResourceTemplate> locationResourceTemplates = getResourcesTemplates(location.getId());
        LocationResources locationResources = new LocationResources(getLocationResourceTypes(locationResourceTemplates));
        setLocationRessource(locationResourceTemplates, locationResources);
        return locationResources;
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * alien4cloud.orchestrators.locations.services.ILocationResourceService#getLocationResourcesFromOrchestrator(alien4cloud.model.orchestrators.locations.
     * Location)
     */
    @Override
    public LocationResources getLocationResourcesFromOrchestrator(Location location) {
        LocationResources locationResources = new LocationResources();
        Orchestrator orchestrator = orchestratorService.getOrFail(location.getOrchestratorId());
        IOrchestratorPlugin orchestratorInstance = (IOrchestratorPlugin) orchestratorPluginService.getOrFail(orchestrator.getId());
        ILocationConfiguratorPlugin configuratorPlugin = orchestratorInstance.getConfigurator(location.getInfrastructureType());
        List<String> allExposedTypes = configuratorPlugin.getResourcesTypes();
        setLocationRessourceTypes(allExposedTypes, location, locationResources);

        List<LocationResourceTemplate> locationResourceTemplates = getResourcesTemplates(location.getId());
        setLocationRessource(locationResourceTemplates, locationResources);
        return locationResources;
    }

    /*
     * (non-Javadoc)
     * 
     * @see alien4cloud.orchestrators.locations.services.ILocationResourceService#getLocationResourceTypes(java.util.Collection)
     */
    @Override
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
            setLocationRessourceTypes(exposedTypes, location, locationResourceTypes);
        }
        return locationResourceTypes;
    }

    /**
     * Put the exposed types to the appropriate List of locationResourceTypes passed as param
     */
    private void setLocationRessourceTypes(Collection<String> exposedTypes, Location location, LocationResourceTypes locationResourceTypes) {
        for (String exposedType : exposedTypes) {
            IndexedNodeType exposedIndexedNodeType = csarRepoSearchService.getRequiredElementInDependencies(IndexedNodeType.class, exposedType,
                    location.getDependencies());

            if (exposedIndexedNodeType.isAbstract()) {
                locationResourceTypes.getConfigurationTypes().put(exposedType, exposedIndexedNodeType);
            } else {
                locationResourceTypes.getNodeTypes().put(exposedType, exposedIndexedNodeType);
            }

            if (exposedIndexedNodeType.getCapabilities() != null && !exposedIndexedNodeType.getCapabilities().isEmpty()) {
                for (CapabilityDefinition capabilityDefinition : exposedIndexedNodeType.getCapabilities()) {
                    locationResourceTypes.getCapabilityTypes().put(capabilityDefinition.getType(), csarRepoSearchService
                            .getRequiredElementInDependencies(IndexedCapabilityType.class, capabilityDefinition.getType(), location.getDependencies()));
                }
            }
        }
    }

    /**
     * Put the locationResourceTemplates to the appropriate List of the locationResources passed as param
     */
    private void setLocationRessource(List<LocationResourceTemplate> locationResourceTemplates, LocationResources locationResources) {
        for (LocationResourceTemplate resourceTemplate : locationResourceTemplates) {
            String templateType = resourceTemplate.getTemplate().getType();
            if (locationResources.getConfigurationTypes().containsKey(templateType)) {
                locationResources.getConfigurationTemplates().add(resourceTemplate);
            }
            if (locationResources.getNodeTypes().containsKey(templateType)) {
                locationResources.getNodeTemplates().add(resourceTemplate);
            }
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see alien4cloud.orchestrators.locations.services.ILocationResourceService#accessor(java.lang.String)
     */
    @Override
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

    /*
     * (non-Javadoc)
     * 
     * @see alien4cloud.orchestrators.locations.services.ILocationResourceService#getResourcesTemplates(java.lang.String)
     */
    @Override
    public List<LocationResourceTemplate> getResourcesTemplates(String locationId) {
        return getResourcesTemplates(getLocationIdFilter(locationId));
    }

    /*
     * (non-Javadoc)
     * 
     * @see alien4cloud.orchestrators.locations.services.ILocationResourceService#getMultiple(java.util.Collection)
     */
    @Override
    public Map<String, LocationResourceTemplate> getMultiple(Collection<String> ids) {
        Map<String, LocationResourceTemplate> result = Maps.newHashMap();
        if (CollectionUtils.isNotEmpty(ids)) {
            List<LocationResourceTemplate> templates = alienDAO.findByIds(LocationResourceTemplate.class, ids.toArray(new String[ids.size()]));
            for (LocationResourceTemplate template : templates) {
                result.put(template.getId(), template);
            }
        }
        return result;
    }

    /*
     * (non-Javadoc)
     * 
     * @see alien4cloud.orchestrators.locations.services.ILocationResourceService#addResourceTemplate(java.lang.String, java.lang.String, java.lang.String)
     */
    @Override
    public LocationResourceTemplate addResourceTemplate(String locationId, String resourceName, String resourceTypeName) {
        Location location = locationService.getOrFail(locationId);
        IndexedNodeType resourceType = csarRepoSearchService.getRequiredElementInDependencies(IndexedNodeType.class, resourceTypeName,
                location.getDependencies());
        NodeTemplate nodeTemplate = topologyService.buildNodeTemplate(location.getDependencies(), resourceType, null);
        // FIXME Workaround to remove default scalable properties from compute
        TopologyUtils.setNullScalingPolicy(nodeTemplate, resourceType);
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

        LocationTemplateCreated event = new LocationTemplateCreated(this);
        event.setTemplate(locationResourceTemplate);
        event.setLocation(location);
        event.setNodeType(resourceType);
        applicationContext.publishEvent(event);

        saveResource(location, locationResourceTemplate);
        return locationResourceTemplate;
    }

    /*
     * (non-Javadoc)
     * 
     * @see alien4cloud.orchestrators.locations.services.ILocationResourceService#deleteResourceTemplate(java.lang.String)
     */
    @Override
    public void deleteResourceTemplate(String resourceId) {
        LocationResourceTemplate resourceTemplate = getOrFail(resourceId);
        Location location = locationService.getOrFail(resourceTemplate.getLocationId());
        location.setLastUpdateDate(new Date());
        alienDAO.delete(LocationResourceTemplate.class, resourceId);
        alienDAO.save(location);
    }

    /*
     * (non-Javadoc)
     * 
     * @see alien4cloud.orchestrators.locations.services.ILocationResourceService#getOrFail(java.lang.String)
     */
    @Override
    public LocationResourceTemplate getOrFail(String resourceId) {
        LocationResourceTemplate locationResourceTemplate = alienDAO.findById(LocationResourceTemplate.class, resourceId);
        if (locationResourceTemplate == null) {
            throw new NotFoundException("Location Resource Template [" + resourceId + "] doesn't exists.");
        }
        return locationResourceTemplate;
    }

    /*
     * (non-Javadoc)
     * 
     * @see alien4cloud.orchestrators.locations.services.ILocationResourceService#merge(java.lang.Object, java.lang.String)
     */
    @Override
    public void merge(Object mergeRequest, String resourceId) {
        LocationResourceTemplate resourceTemplate = getOrFail(resourceId);
        ReflectionUtil.mergeObject(mergeRequest, resourceTemplate);
        saveResource(resourceTemplate);
    }

    /*
     * (non-Javadoc)
     * 
     * @see alien4cloud.orchestrators.locations.services.ILocationResourceService#setTemplateProperty(java.lang.String, java.lang.String, java.lang.Object)
     */
    @Override
    public void setTemplateProperty(String resourceId, String propertyName, Object propertyValue)
            throws ConstraintValueDoNotMatchPropertyTypeException, ConstraintViolationException {
        LocationResourceTemplate resourceTemplate = getOrFail(resourceId);
        Location location = locationService.getOrFail(resourceTemplate.getLocationId());
        IndexedNodeType resourceType = csarRepoSearchService.getRequiredElementInDependencies(IndexedNodeType.class, resourceTemplate.getTemplate().getType(),
                location.getDependencies());
        if (resourceType.getProperties() == null || !resourceType.getProperties().containsKey(propertyName)) {
            throw new NotFoundException("Property <" + propertyName + "> is not found in type <" + resourceType.getElementId() + ">");
        }
        propertyService.setPropertyValue(location.getDependencies(), resourceTemplate.getTemplate(), resourceType.getProperties().get(propertyName),
                propertyName, propertyValue);
        saveResource(resourceTemplate);
    }

    /*
     * (non-Javadoc)
     * 
     * @see alien4cloud.orchestrators.locations.services.ILocationResourceService#setTemplateCapabilityProperty(alien4cloud.model.orchestrators.locations.
     * LocationResourceTemplate, java.lang.String, java.lang.String, java.lang.Object)
     */
    @Override
    public void setTemplateCapabilityProperty(LocationResourceTemplate resourceTemplate, String capabilityName, String propertyName, Object propertyValue)
            throws ConstraintViolationException, ConstraintValueDoNotMatchPropertyTypeException {
        Location location = locationService.getOrFail(resourceTemplate.getLocationId());
        IndexedNodeType resourceType = csarRepoSearchService.getRequiredElementInDependencies(IndexedNodeType.class, resourceTemplate.getTemplate().getType(),
                location.getDependencies());
        Capability capability = getOrFailCapability(resourceTemplate.getTemplate(), capabilityName);
        CapabilityDefinition capabilityDefinition = getOrFailCapabilityDefinition(resourceType, capabilityName);
        IndexedCapabilityType capabilityType = csarRepoSearchService.getRequiredElementInDependencies(IndexedCapabilityType.class,
                capabilityDefinition.getType(), location.getDependencies());
        PropertyDefinition propertyDefinition = getOrFailCapabilityPropertyDefinition(capabilityType, propertyName);
        propertyService.setCapabilityPropertyValue(capability, propertyDefinition, propertyName, propertyValue);
    }

    private Capability getOrFailCapability(NodeTemplate nodeTemplate, String capabilityName) {
        Capability capability = MapUtils.getObject(nodeTemplate.getCapabilities(), capabilityName);
        if (capability != null) {
            return capability;
        }
        throw new NotFoundException("Capability <" + capabilityName + "> not found in template.");
    }

    private PropertyDefinition getOrFailCapabilityPropertyDefinition(IndexedCapabilityType capabilityType, String propertyName) {
        PropertyDefinition propertyDefinition = MapUtils.getObject(capabilityType.getProperties(), propertyName);
        if (propertyDefinition != null) {
            return propertyDefinition;
        }
        throw new NotFoundException("Property <" + propertyName + "> not found in capability type <" + capabilityType.getElementId() + ">");
    }

    private CapabilityDefinition getOrFailCapabilityDefinition(IndexedNodeType resourceType, String capabilityName) {
        CapabilityDefinition capabilityDefinition = IndexedModelUtils.getCapabilityDefinitionById(resourceType.getCapabilities(), capabilityName);
        if (capabilityDefinition != null) {
            return capabilityDefinition;
        }
        throw new NotFoundException("Capability <" + capabilityName + "> not found in type <" + resourceType.getElementId() + ">");
    }

    /*
     * (non-Javadoc)
     * 
     * @see alien4cloud.orchestrators.locations.services.ILocationResourceService#setTemplateCapabilityProperty(java.lang.String, java.lang.String,
     * java.lang.String, java.lang.Object)
     */
    @Override
    public void setTemplateCapabilityProperty(String resourceId, String capabilityName, String propertyName, Object propertyValue)
            throws ConstraintViolationException, ConstraintValueDoNotMatchPropertyTypeException {
        LocationResourceTemplate resourceTemplate = getOrFail(resourceId);
        setTemplateCapabilityProperty(resourceTemplate, capabilityName, propertyName, propertyValue);
        saveResource(resourceTemplate);
    }

    /*
     * (non-Javadoc)
     * 
     * @see alien4cloud.orchestrators.locations.services.ILocationResourceService#autoConfigureResources(java.lang.String)
     */
    @Override
    public List<LocationResourceTemplate> autoConfigureResources(String locationId) {
        return locationService.autoConfigure(locationId);
    }

    /*
     * (non-Javadoc)
     * 
     * @see alien4cloud.orchestrators.locations.services.ILocationResourceService#deleteGeneratedResources(java.lang.String)
     */
    @Override
    public void deleteGeneratedResources(String locationId) {
        QueryBuilder locationIdQuery = QueryBuilders.termQuery("locationId", locationId);
        QueryBuilder generatedFieldQuery = QueryBuilders.termQuery("generated", true);
        // QueryBuilder builder = QueryBuilders.filteredQuery(locationIdQuery, filterBuilder);
        QueryBuilder builder = QueryBuilders.boolQuery().must(locationIdQuery).must(generatedFieldQuery);
        Location location = locationService.getOrFail(locationId);
        location.setLastUpdateDate(new Date());
        alienDAO.delete(LocationResourceTemplate.class, builder);
        alienDAO.save(location);
    }

    /*
     * (non-Javadoc)
     * 
     * @see alien4cloud.orchestrators.locations.services.ILocationResourceService#saveResource(alien4cloud.model.orchestrators.locations.Location,
     * alien4cloud.model.orchestrators.locations.LocationResourceTemplate)
     */
    @Override
    public void saveResource(Location location, LocationResourceTemplate resourceTemplate) {
        location.setLastUpdateDate(new Date());
        alienDAO.save(location);
        alienDAO.save(resourceTemplate);
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * alien4cloud.orchestrators.locations.services.ILocationResourceService#saveResource(alien4cloud.model.orchestrators.locations.LocationResourceTemplate)
     */
    @Override
    public void saveResource(LocationResourceTemplate resourceTemplate) {
        Location location = locationService.getOrFail(resourceTemplate.getLocationId());
        saveResource(location, resourceTemplate);
    }
}
