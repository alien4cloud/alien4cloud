package alien4cloud.orchestrators.locations.services;

import static alien4cloud.utils.AlienUtils.safe;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import javax.annotation.Resource;
import javax.inject.Inject;

import org.alien4cloud.tosca.catalog.index.CsarService;
import org.alien4cloud.tosca.catalog.index.ICsarDependencyLoader;
import org.alien4cloud.tosca.exceptions.ConstraintValueDoNotMatchPropertyTypeException;
import org.alien4cloud.tosca.exceptions.ConstraintViolationException;
import org.alien4cloud.tosca.model.CSARDependency;
import org.alien4cloud.tosca.model.Csar;
import org.alien4cloud.tosca.model.definitions.CapabilityDefinition;
import org.alien4cloud.tosca.model.definitions.PropertyDefinition;
import org.alien4cloud.tosca.model.templates.AbstractTemplate;
import org.alien4cloud.tosca.model.templates.Capability;
import org.alien4cloud.tosca.model.templates.NodeTemplate;
import org.alien4cloud.tosca.model.templates.PolicyTemplate;
import org.alien4cloud.tosca.model.types.AbstractInheritableToscaType;
import org.alien4cloud.tosca.model.types.AbstractToscaType;
import org.alien4cloud.tosca.model.types.CapabilityType;
import org.alien4cloud.tosca.model.types.DataType;
import org.alien4cloud.tosca.model.types.NodeType;
import org.alien4cloud.tosca.model.types.PolicyType;
import org.alien4cloud.tosca.utils.DataTypesFetcher;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.MapUtils;
import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;

import alien4cloud.component.ICSARRepositorySearchService;
import alien4cloud.dao.FilterUtil;
import alien4cloud.dao.IGenericSearchDAO;
import alien4cloud.dao.model.GetMultipleDataResult;
import alien4cloud.events.LocationTemplateCreated;
import alien4cloud.exception.NotFoundException;
import alien4cloud.model.components.IndexedModelUtils;
import alien4cloud.model.orchestrators.Orchestrator;
import alien4cloud.model.orchestrators.locations.AbstractLocationResourceTemplate;
import alien4cloud.model.orchestrators.locations.Location;
import alien4cloud.model.orchestrators.locations.LocationResourceTemplate;
import alien4cloud.model.orchestrators.locations.LocationResourceTemplateWithDependencies;
import alien4cloud.model.orchestrators.locations.LocationResources;
import alien4cloud.model.orchestrators.locations.PolicyLocationResourceTemplate;
import alien4cloud.orchestrators.plugin.ILocationConfiguratorPlugin;
import alien4cloud.orchestrators.plugin.ILocationResourceAccessor;
import alien4cloud.orchestrators.plugin.IOrchestratorPlugin;
import alien4cloud.orchestrators.services.OrchestratorService;
import alien4cloud.paas.OrchestratorPluginService;
import alien4cloud.tosca.container.ToscaTypeLoader;
import alien4cloud.tosca.topology.TemplateBuilder;
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
    private TemplateBuilder templateBuilder;
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
    @Resource
    private ICsarDependencyLoader csarDependencyLoader;
    @Inject
    private PluginArchiveIndexer pluginArchiveIndexer;
    @Inject
    private CsarService csarService;

    /*
     * (non-Javadoc)
     *
     * @see alien4cloud.orchestrators.locations.services.ILocationResourceService#getLocationResources(alien4cloud.model.orchestrators.locations.Location)
     */
    @Override
    public LocationResources getLocationResources(Location location) {
        Orchestrator orchestrator = orchestratorService.get(location.getOrchestratorId());
        Optional<LocationResources> locationResourcesFromOrchestrator = Optional.empty();
        if (orchestrator != null && orchestratorPluginService.get(orchestrator.getId()) != null) {
            locationResourcesFromOrchestrator = Optional.ofNullable(getLocationResourcesFromOrchestrator(location));
        }

        // Also get resource templates from outside of the orchestrator definition - eg custom resources
        List<LocationResourceTemplate> locationResourceTemplates = getResourcesTemplates(location.getId());
        LocationResources locationResources = new LocationResources(getLocationResourceTypes(locationResourceTemplates));

        // process policies types also
        List<PolicyLocationResourceTemplate> policyLocationResourceTemplates = getPoliciesResourcesTemplates(location.getId());
        locationResources.addFrom(getPoliciesLocationResourceTypes(policyLocationResourceTemplates));
        /*
         * If the orchestrator is present, take node types computed from the resources template
         * as "Custom resources types". If not, consider this is an orchestrator-free location.
         */
        locationResourcesFromOrchestrator.ifPresent(orchestratorResources -> {
            locationResources.getCapabilityTypes().putAll(orchestratorResources.getCapabilityTypes());
            locationResources.getConfigurationTypes().putAll(orchestratorResources.getConfigurationTypes());
            locationResources.getNodeTypes().putAll(orchestratorResources.getNodeTypes());
            locationResources.getProvidedTypes().addAll(orchestratorResources.getNodeTypes().keySet());
            locationResources.getAllNodeTypes().putAll(orchestratorResources.getAllNodeTypes());
            locationResources.getOnDemandTypes().putAll(orchestratorResources.getOnDemandTypes());
            locationResources.getPolicyTypes().putAll(orchestratorResources.getPolicyTypes());
        });
        setLocationRessource(locationResourceTemplates, locationResources);
        locationResources.getPolicyTemplates().addAll(policyLocationResourceTemplates);
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
        IOrchestratorPlugin orchestratorInstance = orchestratorPluginService.getOrFail(orchestrator.getId());
        ILocationConfiguratorPlugin configuratorPlugin = orchestratorInstance.getConfigurator(location.getInfrastructureType());

        fillLocationResourceTypes(configuratorPlugin.getResourcesTypes(), locationResources, location.getDependencies());
        fillPoliciesLocationResourceTypes(configuratorPlugin.getPoliciesTypes(), locationResources, location.getDependencies());

        // add LocationResourceTemplate
        List<LocationResourceTemplate> locationResourceTemplates = getResourcesTemplates(location.getId());
        setLocationRessource(locationResourceTemplates, locationResources);

        // add PolicyLocationResourceTemplate
        locationResources.getPolicyTemplates().addAll(getPoliciesResourcesTemplates(location.getId()));
        return locationResources;
    }

    /*
     * (non-Javadoc)
     *
     * @see alien4cloud.orchestrators.locations.services.ILocationResourceService#getLocationResourceTypes(java.util.Collection)
     */
    @Override
    public LocationResourceTypes getLocationResourceTypes(Collection<LocationResourceTemplate> resourceTemplates) {
        LocationResourceTypes locationResourceTypes = new LocationResourceTypes();
        fillLocationResourceTypes(resourceTemplates,
                (exposedTypes, dependencies) -> fillLocationResourceTypes(exposedTypes, locationResourceTypes, dependencies));
        return locationResourceTypes;
    }

    @Override
    public LocationResourceTypes getPoliciesLocationResourceTypes(Collection<PolicyLocationResourceTemplate> resourceTemplates) {
        LocationResourceTypes locationResourceTypes = new LocationResourceTypes();
        fillLocationResourceTypes(resourceTemplates,
                (exposedTypes, dependencies) -> fillPoliciesLocationResourceTypes(exposedTypes, locationResourceTypes, dependencies));
        return locationResourceTypes;
    }

    private void fillLocationResourceTypes(Collection<? extends AbstractLocationResourceTemplate> resourceTemplates, IResourceTypeFiller resourceTypeFiller) {
        Map<String, Set<String>> resourceTypesByLocationId = Maps.newHashMap();
        for (AbstractLocationResourceTemplate resourceTemplate : resourceTemplates) {
            if (!resourceTemplate.isService()) {
                Set<String> locationResourceTypesSet = resourceTypesByLocationId.computeIfAbsent(resourceTemplate.getLocationId(), k -> Sets.newHashSet());
                locationResourceTypesSet.add(resourceTemplate.getTemplate().getType());
            }
        }
        for (Map.Entry<String, Set<String>> resourceTypeByLocationIdEntry : resourceTypesByLocationId.entrySet()) {
            String locationId = resourceTypeByLocationIdEntry.getKey();
            Set<String> exposedTypes = resourceTypeByLocationIdEntry.getValue();
            Location location = locationService.getOrFail(locationId);
            resourceTypeFiller.process(exposedTypes, location.getDependencies());
        }
    }

    /**
     * Put the exposed types to the appropriate List of locationResourceTypes passed as param
     */
    public void fillLocationResourceTypes(Collection<String> exposedTypes, LocationResourceTypes locationResourceTypes,
            final Set<CSARDependency> dependencies) {
        fillLocationResourceTypes(exposedTypes, locationResourceTypes, dependencies, (exposedType) -> {
            NodeType nodeType = csarRepoSearchService.getRequiredElementInDependencies(NodeType.class, exposedType, dependencies);
            addExposedNodeType(locationResourceTypes, dependencies, nodeType);
        });
    }

    private void fillLocationResourceTypes(Collection<String> exposedTypes, LocationResourceTypes locationResourceTypes, Set<CSARDependency> dependencies,
            IResourceTypeAdder resourceTypeAdder) {
        if (CollectionUtils.isEmpty(exposedTypes) || CollectionUtils.isEmpty(dependencies)) {
            // Protect as in some case this method is called even if there's nothing to fill in
            return;
        }

        exposedTypes.forEach(exposedType -> resourceTypeAdder.process(exposedType));

        Map<String, DataType> allDataTypes = new HashMap<>(locationResourceTypes.getDataTypes());
        DataTypesFetcher.DataTypeFinder dataTypeFinder = (type, id) -> csarRepoSearchService.getElementInDependencies(type, id, dependencies);
        allDataTypes.putAll(DataTypesFetcher.getDataTypesDependencies(locationResourceTypes.getNodeTypes().values(), dataTypeFinder));
        allDataTypes.putAll(DataTypesFetcher.getDataTypesDependencies(locationResourceTypes.getCapabilityTypes().values(), dataTypeFinder));
        allDataTypes.putAll(DataTypesFetcher.getDataTypesDependencies(locationResourceTypes.getOnDemandTypes().values(), dataTypeFinder));
        allDataTypes.putAll(DataTypesFetcher.getDataTypesDependencies(locationResourceTypes.getConfigurationTypes().values(), dataTypeFinder));
        allDataTypes.putAll(DataTypesFetcher.getDataTypesDependencies(locationResourceTypes.getPolicyTypes().values(), dataTypeFinder));
        locationResourceTypes.setDataTypes(allDataTypes);
    }

    private void addExposedNodeType(LocationResourceTypes locationResourceTypes, Set<CSARDependency> dependencies, NodeType exposedNodeType) {
        if (exposedNodeType.isAbstract()) {
            locationResourceTypes.getConfigurationTypes().put(exposedNodeType.getElementId(), exposedNodeType);
        } else {
            locationResourceTypes.getNodeTypes().put(exposedNodeType.getElementId(), exposedNodeType);
        }

        safe(exposedNodeType.getCapabilities()).forEach(capabilityDefinition -> locationResourceTypes.getCapabilityTypes().put(capabilityDefinition.getType(),
                csarRepoSearchService.getRequiredElementInDependencies(CapabilityType.class, capabilityDefinition.getType(), dependencies)));
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
                return getResourcesTemplates(LocationResourceTemplate.class, filter);
            }

            @Override
            public <T extends AbstractToscaType> T getIndexedToscaElement(String type) {
                return (T) csarRepoSearchService.getRequiredElementInDependencies(AbstractToscaType.class, type, location.getDependencies());
            }

            @Override
            public Set<CSARDependency> getDependencies() {
                return location.getDependencies();
            }
        };
    }

    private Map<String, String[]> getLocationIdFilter(String locationId) {
        return FilterUtil.singleKeyFilter("locationId", locationId);
    }

    private <T extends AbstractLocationResourceTemplate> List<T> getResourcesTemplates(Class<T> clazz, Map<String, String[]> filter) {
        GetMultipleDataResult<T> result = alienDAO.find(clazz, filter, Integer.MAX_VALUE);
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
        return getResourcesTemplates(LocationResourceTemplate.class, getLocationIdFilter(locationId));
    }

    /*
     * (non-Javadoc)
     * 
     * @see alien4cloud.orchestrators.locations.services.ILocationResourceService#addResourceTemplate(java.lang.String, java.lang.String, java.lang.String)
     */
    @Override
    public LocationResourceTemplateWithDependencies addResourceTemplate(String locationId, String resourceName, String resourceTypeName) {
        Location location = locationService.getOrFail(locationId);
        return new LocationResourceTemplateWithDependencies(this.addResourceTemplate(location, resourceName, resourceTypeName), Collections.EMPTY_SET);
    }

    private LocationResourceTemplate addResourceTemplate(Location location, String resourceName, String resourceTypeName) {
        NodeType resourceType = csarRepoSearchService.getRequiredElementInDependencies(NodeType.class, resourceTypeName, location.getDependencies());
        // refuse the creation of a resource if the related type's CSAR has unresolved dependencies
        csarService.validateMissgingDependencies(resourceType);

        NodeTemplate nodeTemplate = templateBuilder.buildNodeTemplate(location.getDependencies(), resourceType);
        LocationResourceTemplate locationResourceTemplate = new LocationResourceTemplate();
        locationResourceTemplate.setGenerated(false);
        fillAndSaveLocationResourceTemplate(location, resourceName, locationResourceTemplate, resourceType, nodeTemplate);
        return locationResourceTemplate;
    }

    private <T extends AbstractTemplate> void fillAndSaveLocationResourceTemplate(Location location, String resourceName,
            AbstractLocationResourceTemplate<T> locationResourceTemplate, AbstractInheritableToscaType resourceType, T template) {
        locationResourceTemplate.setName(resourceName);
        locationResourceTemplate.setEnabled(true);
        locationResourceTemplate.setId(UUID.randomUUID().toString());
        locationResourceTemplate.setLocationId(location.getId());
        locationResourceTemplate.setService(false);
        locationResourceTemplate.setTypes(Lists.<String> newArrayList(resourceType.getElementId()));
        locationResourceTemplate.getTypes().addAll(resourceType.getDerivedFrom());
        locationResourceTemplate.setTemplate(template);

        publishCreatedEventAndSaveResource(location, locationResourceTemplate, resourceType);
    }

    private <T extends AbstractTemplate> void publishCreatedEventAndSaveResource(Location location,
            AbstractLocationResourceTemplate<T> locationResourceTemplate, AbstractInheritableToscaType resourceType) {
        LocationTemplateCreated event = new LocationTemplateCreated(this);
        event.setTemplate(locationResourceTemplate);
        event.setLocation(location);
        event.setToscaType(resourceType);
        applicationContext.publishEvent(event);

        saveResource(location, locationResourceTemplate);
    }

    /*
     * (non-Javadoc)
     *
     * @see alien4cloud.orchestrators.locations.services.ILocationResourceService#addResourceTemplate(java.lang.String, java.lang.String, java.lang.String)
     */
    @Override
    public LocationResourceTemplateWithDependencies addResourceTemplateFromArchive(String locationId, String resourceName, String resourceTypeName,
            String archiveName, String archiveVersion) {
        Location location = updateLocationDependencies(locationId, archiveName, archiveVersion);

        // Return a wrapper object with the template and location dependencies
        return new LocationResourceTemplateWithDependencies(this.addResourceTemplate(location, resourceName, resourceTypeName),
                Sets.newHashSet(location.getDependencies()));

    }

    @Override
    public LocationResourceTemplateWithDependencies duplicateResourceTemplate(String resourceId) {
        LocationResourceTemplate locationResourceTemplate = getOrFail(resourceId);
        locationResourceTemplate.setId(UUID.randomUUID().toString());
        locationResourceTemplate.setName(locationResourceTemplate.getName() + "_" + "copy");
        locationResourceTemplate.setGenerated(false);

        Location location = locationService.getOrFail(locationResourceTemplate.getLocationId());
        NodeType resourceType = csarRepoSearchService.getRequiredElementInDependencies(NodeType.class, locationResourceTemplate.getTemplate().getType(),
                location.getDependencies());

        publishCreatedEventAndSaveResource(location, locationResourceTemplate, resourceType);

        return new LocationResourceTemplateWithDependencies(locationResourceTemplate, Sets.newHashSet(location.getDependencies()));
    }

    @Override
    public LocationResourceTemplateWithDependencies duplicatePolicyTemplate(String resourceId) {
        PolicyLocationResourceTemplate policyLocationResourceTemplate = getOrFail(resourceId);
        policyLocationResourceTemplate.setId(UUID.randomUUID().toString());
        policyLocationResourceTemplate.setName(policyLocationResourceTemplate.getName() + "_" + "copy");

        Location location = locationService.getOrFail(policyLocationResourceTemplate.getLocationId());
        PolicyType resourceType = csarRepoSearchService.getRequiredElementInDependencies(PolicyType.class,
                policyLocationResourceTemplate.getTemplate().getType(), location.getDependencies());

        publishCreatedEventAndSaveResource(location, policyLocationResourceTemplate, resourceType);

        return new LocationResourceTemplateWithDependencies(policyLocationResourceTemplate, Sets.newHashSet(location.getDependencies()));
    }

    /*
     * (non-Javadoc)
     * 
     * @see alien4cloud.orchestrators.locations.services.ILocationResourceService#deleteResourceTemplate(Class, String)
     */
    @Override
    public void deleteResourceTemplate(String resourceId) {
        AbstractLocationResourceTemplate resourceTemplate = getOrFail(resourceId);
        Location location = locationService.getOrFail(resourceTemplate.getLocationId());
        alienDAO.delete(resourceTemplate.getClass(), resourceId);
        refreshDependencies(location);
        alienDAO.save(location);
    }

    /*
     * (non-Javadoc)
     * 
     * @see alien4cloud.orchestrators.locations.services.ILocationResourceService#getOrFail(java.lang.String)
     */
    @Override
    public <T extends AbstractLocationResourceTemplate> T getOrFail(String resourceId) {
        AbstractLocationResourceTemplate locationResourceTemplate = alienDAO.findById(AbstractLocationResourceTemplate.class, resourceId);
        if (locationResourceTemplate == null) {
            throw new NotFoundException("Location Resource Template [" + resourceId + "] doesn't exists.");
        }
        return (T) locationResourceTemplate;
    }

    /*
     * (non-Javadoc)
     * 
     * @see alien4cloud.orchestrators.locations.services.ILocationResourceService#merge(java.lang.Object, java.lang.String)
     */
    @Override
    public void merge(Object mergeRequest, String resourceId) {
        AbstractLocationResourceTemplate resourceTemplate = getOrFail(resourceId);
        ReflectionUtil.mergeObject(mergeRequest, resourceTemplate);
        saveResource(resourceTemplate);
    }

    /*
     * (non-Javadoc)
     * 
     * @see alien4cloud.orchestrators.locations.services.ILocationResourceService#setTemplateProperty(java.lang.String, java.lang.String,
     * java.lang.Object)
     */
    @Override
    public void setTemplateProperty(String resourceId, String propertyName, Object propertyValue)
            throws ConstraintValueDoNotMatchPropertyTypeException, ConstraintViolationException {
        AbstractLocationResourceTemplate resourceTemplate = getOrFail(resourceId);
        Location location = locationService.getOrFail(resourceTemplate.getLocationId());
        AbstractInheritableToscaType resourceType = (AbstractInheritableToscaType) csarRepoSearchService
                .getRequiredElementInDependencies(AbstractToscaType.class, resourceTemplate.getTemplate().getType(), location.getDependencies());
        if (!safe(resourceType.getProperties()).containsKey(propertyName)) {
            throw new NotFoundException("Property [" + propertyName + "] is not found in type [" + resourceType.getElementId() + "]");
        }

        propertyService.setPropertyValue(location.getDependencies(), resourceTemplate.getTemplate(), resourceType.getProperties().get(propertyName),
                propertyName, propertyValue);
        saveResource(resourceTemplate);
    }

    private void setTemplateCapabilityProperty(LocationResourceTemplate resourceTemplate, String capabilityName, String propertyName, Object propertyValue)
            throws ConstraintViolationException, ConstraintValueDoNotMatchPropertyTypeException {
        Location location = locationService.getOrFail(resourceTemplate.getLocationId());
        NodeType resourceType = csarRepoSearchService.getRequiredElementInDependencies(NodeType.class, resourceTemplate.getTemplate().getType(),
                location.getDependencies());
        Capability capability = getOrFailCapability(resourceTemplate.getTemplate(), capabilityName);
        CapabilityDefinition capabilityDefinition = getOrFailCapabilityDefinition(resourceType, capabilityName);
        CapabilityType capabilityType = csarRepoSearchService.getRequiredElementInDependencies(CapabilityType.class, capabilityDefinition.getType(),
                location.getDependencies());
        PropertyDefinition propertyDefinition = getOrFailCapabilityPropertyDefinition(capabilityType, propertyName);
        propertyService.setCapabilityPropertyValue(location.getDependencies(), capability, propertyDefinition, propertyName, propertyValue);
    }

    private Capability getOrFailCapability(NodeTemplate nodeTemplate, String capabilityName) {
        Capability capability = MapUtils.getObject(nodeTemplate.getCapabilities(), capabilityName);
        if (capability != null) {
            return capability;
        }
        throw new NotFoundException("Capability [" + capabilityName + "] not found in template.");
    }

    private PropertyDefinition getOrFailCapabilityPropertyDefinition(CapabilityType capabilityType, String propertyName) {
        PropertyDefinition propertyDefinition = MapUtils.getObject(capabilityType.getProperties(), propertyName);
        if (propertyDefinition != null) {
            return propertyDefinition;
        }
        throw new NotFoundException("Property [" + propertyName + "] not found in capability type [" + capabilityType.getElementId() + "]");
    }

    private CapabilityDefinition getOrFailCapabilityDefinition(NodeType resourceType, String capabilityName) {
        CapabilityDefinition capabilityDefinition = IndexedModelUtils.getCapabilityDefinitionById(resourceType.getCapabilities(), capabilityName);
        if (capabilityDefinition != null) {
            return capabilityDefinition;
        }
        throw new NotFoundException("Capability [" + capabilityName + "] not found in type [" + resourceType.getElementId() + "]");
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
    public List<LocationResourceTemplate> autoConfigureResources(String locationId) throws UnsupportedOperationException {
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
    public void saveResource(Location location, AbstractLocationResourceTemplate resourceTemplate) {
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
    public void saveResource(AbstractLocationResourceTemplate resourceTemplate) {
        Location location = locationService.getOrFail(resourceTemplate.getLocationId());
        saveResource(location, resourceTemplate);
    }

    private void refreshDependencies(Location location) {
        ToscaTypeLoader toscaTypeLoader = new ToscaTypeLoader(csarDependencyLoader);

        loadResourcesTypes(getResourcesTemplates(location.getId()), location, toscaTypeLoader);
        loadResourcesTypes(getPoliciesResourcesTemplates((location.getId())), location, toscaTypeLoader);

        location.setDependencies(toscaTypeLoader.getLoadedDependencies());
        // ALWAYS add native dependencies
        location.getDependencies().addAll(pluginArchiveIndexer.getNativeDependencies(orchestratorService.getOrFail(location.getOrchestratorId()), location));
    }

    private <T extends AbstractLocationResourceTemplate> void loadResourcesTypes(List<T> resources, Location location, ToscaTypeLoader toscaTypeLoader) {
        for (T resource : resources) {
            String type = resource.getTemplate().getType();
            AbstractToscaType toscaType = csarRepoSearchService.getRequiredElementInDependencies(AbstractToscaType.class, type, location.getDependencies());
            toscaTypeLoader.loadType(resource.getTemplate().getType(),
                    csarDependencyLoader.buildDependencyBean(toscaType.getArchiveName(), toscaType.getArchiveVersion()));
        }
    }

    /*
     * (non-Javadoc)
     *
     * @see alien4cloud.orchestrators.locations.services.ILocationResourceService#addPolicyTemplateFromArchive(java.lang.String, java.lang.String,
     * java.lang.String)
     */
    @Override
    public LocationResourceTemplateWithDependencies addPolicyTemplateFromArchive(String locationId, String resourceName, String policyType, String archiveName,
            String archiveVersion) {
        Location location = updateLocationDependencies(locationId, archiveName, archiveVersion);

        // Return a wrapper object with the template and location dependencies
        return new LocationResourceTemplateWithDependencies(this.addPolicyLocationResourceTemplate(location, resourceName, policyType),
                Sets.newHashSet(location.getDependencies()));

    }

    private PolicyLocationResourceTemplate addPolicyLocationResourceTemplate(Location location, String resourceName, String resourceTypeName) {
        PolicyLocationResourceTemplate policyLocationResourceTemplate = new PolicyLocationResourceTemplate();
        PolicyType resourceType = csarRepoSearchService.getRequiredElementInDependencies(PolicyType.class, resourceTypeName, location.getDependencies());
        PolicyTemplate policyTemplate = TemplateBuilder.buildPolicyTemplate(resourceType);
        fillAndSaveLocationResourceTemplate(location, resourceName, policyLocationResourceTemplate, resourceType, policyTemplate);
        return policyLocationResourceTemplate;
    }

    private Location updateLocationDependencies(String locationId, String archiveName, String archiveVersion) {
        Location location = locationService.getOrFail(locationId);

        // If an archive is specified, update the location dependencies accordingly. Dependencies are in a Set so there is no duplication issue.
        if (!(StringUtils.isEmpty(archiveName) && StringUtils.isEmpty(archiveVersion))) {
            Optional.ofNullable(csarRepoSearchService.getArchive(archiveName, archiveVersion)).map(Csar::getDependencies)
                    .ifPresent(csarDependencies -> location.getDependencies().addAll(csarDependencies));
            // Add the archive as dependency too
            final CSARDependency archive = new CSARDependency(archiveName, archiveVersion);
            location.getDependencies().add(archive);
        }
        return location;
    }

    private List<PolicyLocationResourceTemplate> getPoliciesResourcesTemplates(String locationId) {
        return getResourcesTemplates(PolicyLocationResourceTemplate.class, getLocationIdFilter(locationId));
    }

    private void fillPoliciesLocationResourceTypes(Collection<String> exposedTypes, LocationResourceTypes locationResourceTypes,
            Set<CSARDependency> dependencies) {
        fillLocationResourceTypes(exposedTypes, locationResourceTypes, dependencies, (exposedType) -> {
            PolicyType policyType = csarRepoSearchService.getRequiredElementInDependencies(PolicyType.class, exposedType, dependencies);
            locationResourceTypes.getPolicyTypes().put(policyType.getElementId(), policyType);
        });
    }

    private interface IResourceTypeAdder {
        void process(String exposedType);
    }

    private interface IResourceTypeFiller {
        void process(Collection<String> exposedTypes, Set<CSARDependency> dependencies);
    }
}
