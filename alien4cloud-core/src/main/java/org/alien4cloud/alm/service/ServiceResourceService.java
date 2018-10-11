package org.alien4cloud.alm.service;

import static alien4cloud.dao.FilterUtil.fromKeyValueCouples;
import static alien4cloud.dao.FilterUtil.singleKeyFilter;
import static alien4cloud.utils.AlienUtils.safe;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import javax.annotation.Resource;
import javax.inject.Inject;

import alien4cloud.dao.model.FacetedSearchResult;
import alien4cloud.model.application.Application;
import org.alien4cloud.alm.events.ServiceDeletedEvent;
import alien4cloud.orchestrators.locations.events.OnLocationResourceChangeEvent;
import org.alien4cloud.alm.service.events.ServiceChangedEvent;
import org.alien4cloud.alm.service.events.ServiceUsageRequestEvent;
import org.alien4cloud.alm.service.exceptions.IncompatibleHalfRelationshipException;
import org.alien4cloud.alm.service.exceptions.ServiceUsageException;
import org.alien4cloud.tosca.catalog.events.ArchiveUsageRequestEvent;
import org.alien4cloud.tosca.catalog.index.IToscaTypeSearchService;
import org.alien4cloud.tosca.exceptions.ConstraintValueDoNotMatchPropertyTypeException;
import org.alien4cloud.tosca.exceptions.ConstraintViolationException;
import org.alien4cloud.tosca.model.CSARDependency;
import org.alien4cloud.tosca.model.Csar;
import org.alien4cloud.tosca.model.definitions.AbstractPropertyValue;
import org.alien4cloud.tosca.model.definitions.CapabilityDefinition;
import org.alien4cloud.tosca.model.templates.Capability;
import org.alien4cloud.tosca.model.templates.NodeTemplate;
import org.alien4cloud.tosca.model.types.CapabilityType;
import org.alien4cloud.tosca.model.types.NodeType;
import org.alien4cloud.tosca.model.types.RelationshipType;
import org.apache.commons.lang3.ArrayUtils;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.event.EventListener;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;

import alien4cloud.dao.IGenericSearchDAO;
import alien4cloud.dao.model.GetMultipleDataResult;
import alien4cloud.exception.AlreadyExistException;
import alien4cloud.exception.NotFoundException;
import alien4cloud.model.common.Usage;
import alien4cloud.model.orchestrators.locations.Location;
import alien4cloud.model.service.ServiceResource;
import alien4cloud.orchestrators.locations.events.AfterLocationDeleted;
import alien4cloud.paas.plan.ToscaNodeLifecycleConstants;
import alien4cloud.rest.utils.PatchUtil;
import alien4cloud.utils.CollectionUtils;
import alien4cloud.utils.VersionUtil;
import alien4cloud.utils.version.Version;

/**
 * Manages services.
 */
@Service
public class ServiceResourceService {
    @Resource(name = "alien-es-dao")
    private IGenericSearchDAO alienDAO;
    @Inject
    private NodeInstanceService nodeInstanceService;
    @Inject
    private IToscaTypeSearchService toscaTypeSearchService;
    @Inject
    private ApplicationEventPublisher publisher;

    /**
     * Creates a service.
     *
     * @param serviceName The unique name that defines the service from user point of view.
     * @param serviceVersion The id of the plugin used to communicate with the orchestrator.
     * @param serviceNodeType The type of the node type used to create the service.
     * @param serviceNodeVersion The version of the node type used to create the service.
     * @return The generated identifier for the service.
     */
    public String create(String serviceName, String serviceVersion, String serviceNodeType, String serviceNodeVersion) {
        return create(serviceName, serviceVersion, serviceNodeType, serviceNodeVersion, null);
    }



    /**
     * Create a service.
     * 
     * @param serviceName The unique name that defines the service from user point of view.
     * @param serviceVersion The id of the plugin used to communicate with the orchestrator.
     * @param serviceNodeType The type of the node type used to create the service.
     * @param serviceNodeVersion The version of the node type used to create the service.
     * @param environmentId In case the service is created out of an alien environment the id of the environment, null if not.
     * @return The generated identifier for the service.
     */
    public String create(String serviceName, String serviceVersion, String serviceNodeType, String serviceNodeVersion, String environmentId) {
        ServiceResource serviceResource = new ServiceResource();
        // generate an unique id
        serviceResource.setId(UUID.randomUUID().toString());
        serviceResource.setName(serviceName);
        serviceResource.setVersion(serviceVersion);
        serviceResource.setEnvironmentId(environmentId);

        // build a node instance from the given type
        NodeType nodeType = toscaTypeSearchService.findOrFail(NodeType.class, serviceNodeType, serviceNodeVersion);
        serviceResource.setNodeInstance(nodeInstanceService.create(nodeType, serviceNodeVersion));
        serviceResource.setDependency(new CSARDependency(nodeType.getArchiveName(), nodeType.getArchiveVersion()));

        // ensure uniqueness and save
        save(serviceResource, true);

        // TODO: send an event: a service has been created
        return serviceResource.getId();
    }

    /**
     * List the service resources.
     * 
     * @param from start index for pagination.
     * @param count max mumber of elements to return.
     * @return The request result that contains service resources.
     */
    public GetMultipleDataResult<ServiceResource> list(int from, int count) {
        return alienDAO.buildQuery(ServiceResource.class).prepareSearch().setFieldSort("name", false).search(from, count);
    }

    /**
     * Search for service resources.
     *
     * @param searchText The search to use to get data.
     * @param filters Optional filters for the query.
     * @param sortField The field on which to sort results.
     * @param desc Is the sort over sortfield ascending or descending.
     * @param from start index for pagination.
     * @param count max mumber of elements to return.
     * @return The request result that contains service resources matching the search.
     */
    public FacetedSearchResult<ServiceResource> search(String searchText, Map<String, String[]> filters, String sortField, boolean desc, int from,
            int count) {
        if (sortField == null) {
            sortField = "name";
        }
        return alienDAO.facetedSearch(ServiceResource.class, searchText, filters, null, "", from, count, sortField, desc);
    }

    /**
     * Get the service matching the given id or throw a NotFoundException.
     *
     * @param id If of the service that we want to get.
     * @return An instance of the service.
     */
    public ServiceResource getOrFail(String id) {
        ServiceResource serviceResource = alienDAO.findById(ServiceResource.class, id);
        if (serviceResource == null) {
            throw new NotFoundException("Service [" + id + "] doesn't exists.");
        }
        return serviceResource;
    }

    /**
     * Update a service resource.
     *
     * @param resourceId The service resource id.
     * @param name The new name of the service resource.
     * @param version The new version of the service resource.
     * @param description The description of the service resource.
     * @param nodeType The new node type for the instance of the service resource.
     * @param nodeTypeVersion The new node type version of the service resource.
     * @param nodeProperties The new properties of the service resource.
     * @param nodeCapabilities The new capabilies properties of the service resource.
     * @param nodeAttributeValues The new attributes of the service resource.
     * @param locations The new location for the service resource.
     */
    public void update(String resourceId, String name, String version, String description, String nodeType, String nodeTypeVersion,
            Map<String, AbstractPropertyValue> nodeProperties, Map<String, Capability> nodeCapabilities, Map<String, String> nodeAttributeValues,
            String[] locations, Map<String, String> capabilitiesRelationshipTypes, Map<String, String> requirementsRelationshipTypes)
            throws ConstraintValueDoNotMatchPropertyTypeException, ConstraintViolationException {
        update(resourceId, name, version, description, nodeType, nodeTypeVersion, nodeProperties, nodeCapabilities, nodeAttributeValues, locations,
                capabilitiesRelationshipTypes, requirementsRelationshipTypes, false);
    }

    // TODO: manage relationships in duplicate
    public String duplicate(String serviceId) throws ConstraintValueDoNotMatchPropertyTypeException, ConstraintViolationException {
        ServiceResource serviceResource = getOrFail(serviceId);
        if (serviceResource.getEnvironmentId() != null) {
            throw new UnsupportedOperationException("Alien managed services cannot be duplicated.");
        }
        // TODO: better naming deduplicate strategy
        String serviceName = serviceResource.getName() + "_copy";
        String serviceVersion = serviceResource.getVersion();
        String description = serviceResource.getDescription();
        String nodeType = serviceResource.getNodeInstance().getNodeTemplate().getType();
        String nodeVersion = serviceResource.getNodeInstance().getTypeVersion();
        String newServiceId = create(serviceName, serviceVersion, nodeType, nodeVersion);
        Map<String, String> attributeValues = serviceResource.getNodeInstance().getAttributeValues();
        attributeValues.put(ToscaNodeLifecycleConstants.ATT_STATE, ToscaNodeLifecycleConstants.INITIAL);

        NodeTemplate nodeTemplate = serviceResource.getNodeInstance().getNodeTemplate();
        update(newServiceId, serviceName, serviceVersion, description, nodeType, nodeVersion, nodeTemplate.getProperties(), nodeTemplate.getCapabilities(), attributeValues, serviceResource.getLocationIds(), null, null);

        return newServiceId;
    }

    /**
     * Patch a service resource.
     *
     * @param resourceId The service resource id.
     * @param name The new name of the service resource.
     * @param version The new version of the service resource.
     * @param description The description of the service resource.
     * @param nodeType The new node type for the instance of the service resource.
     * @param nodeTypeVersion The new node type version of the service resource.
     * @param nodeProperties The new properties of the service resource.
     * @param nodeCapabilities The new capabilies properties of the service resource.
     * @param nodeAttributeValues The new attributes of the service resource.
     * @param locations The new location for the service resource.
     * @param capabilitiesRelationshipTypes The new half relationships attached to a capacity for the service resource
     * @param requirementsRelationshipTypes The new half relationships attached to a requirement for the service resource
     */
    public void patch(String resourceId, String name, String version, String description, String nodeType, String nodeTypeVersion,
            Map<String, AbstractPropertyValue> nodeProperties, Map<String, Capability> nodeCapabilities, Map<String, String> nodeAttributeValues,
            String[] locations, Map<String, String> capabilitiesRelationshipTypes, Map<String, String> requirementsRelationshipTypes)
            throws ConstraintValueDoNotMatchPropertyTypeException, ConstraintViolationException {
        update(resourceId, name, version, description, nodeType, nodeTypeVersion, nodeProperties, nodeCapabilities, nodeAttributeValues, locations,
                capabilitiesRelationshipTypes, requirementsRelationshipTypes, true);
    }

    private void update(String resourceId, String name, String version, String description, String nodeTypeStr, String nodeTypeVersion,
            Map<String, AbstractPropertyValue> nodeProperties, Map<String, Capability> nodeCapabilities, Map<String, String> nodeAttributeValues,
            String[] locations, Map<String, String> capabilitiesRelationshipTypes, Map<String, String> requirementsRelationshipTypes, boolean patch)
            throws ConstraintValueDoNotMatchPropertyTypeException, ConstraintViolationException {
        ServiceResource serviceResource = getOrFail(resourceId);
        failUpdateIfManaged(serviceResource, patch, name, version, nodeTypeStr, nodeTypeVersion, nodeProperties, nodeCapabilities, nodeAttributeValues);

        boolean ensureUniqueness = false;

        boolean isDeployed = !ToscaNodeLifecycleConstants.INITIAL
                .equals(serviceResource.getNodeInstance().getAttributeValues().get(ToscaNodeLifecycleConstants.ATT_STATE));

        String updatedState = nodeAttributeValues == null ? null : nodeAttributeValues.get(ToscaNodeLifecycleConstants.ATT_STATE);
        if (!patch || updatedState != null) {
            // in case of an update or when patching the state: check that the new state is a valid state
            if (!ToscaNodeLifecycleConstants.TOSCA_STATES.contains(updatedState)) {
                throw new IllegalArgumentException(
                        "State <" + updatedState + "> is not a valid state value must be one of " + ToscaNodeLifecycleConstants.TOSCA_STATES.toString());
            }
        }
        boolean isUpdateDeployed = updatedState == null ? isDeployed
                : !ToscaNodeLifecycleConstants.INITIAL.equals(nodeAttributeValues.get(ToscaNodeLifecycleConstants.ATT_STATE));

        NodeType nodeType = null;
        // Updating a running service is not yet authorized
        if (isDeployed && isUpdateDeployed) {
            // Update operation is not allowed for running services.
            // Patch operation is allowed only on the service description or locations authorized for matching.
            if (!patch || name != null || version != null || nodeTypeStr != null || nodeTypeVersion != null || nodeProperties != null
                    || nodeCapabilities != null || nodeAttributeValues != null || capabilitiesRelationshipTypes != null
                    || requirementsRelationshipTypes != null) {
                throw new UnsupportedOperationException(
                        "Update is not allowed on a running service, please use patch if you wish to change locations or authorizations.");
            }
        } else {
            ensureUniqueness = PatchUtil.set(serviceResource, "name", name, patch);
            ensureUniqueness = PatchUtil.set(serviceResource, "version", version, patch) || ensureUniqueness;

            // Node instance properties update
            nodeType = toscaTypeSearchService.findOrFail(NodeType.class, serviceResource.getNodeInstance().getNodeTemplate().getType(),
                    serviceResource.getNodeInstance().getTypeVersion());

            // node instance type update
            PatchUtil.set(serviceResource.getNodeInstance().getNodeTemplate(), "type", nodeTypeStr, patch);
            PatchUtil.set(serviceResource.getNodeInstance(), "typeVersion", nodeTypeVersion, patch);

            // update half-relationship type
            serviceResource.setCapabilitiesRelationshipTypes(
                    PatchUtil.setMap(serviceResource.getCapabilitiesRelationshipTypes(), capabilitiesRelationshipTypes, patch));
            serviceResource.setRequirementsRelationshipTypes(
                    PatchUtil.setMap(serviceResource.getRequirementsRelationshipTypes(), requirementsRelationshipTypes, patch));
            // validate the half-relationship types exist
            validateRelationshipTypes(serviceResource, nodeType);

            // Node instance properties update
            nodeType = toscaTypeSearchService.findOrFail(NodeType.class, serviceResource.getNodeInstance().getNodeTemplate().getType(),
                    serviceResource.getNodeInstance().getTypeVersion());
            if (patch) {
                nodeInstanceService.patch(nodeType, serviceResource.getNodeInstance(), nodeProperties, nodeCapabilities, nodeAttributeValues);
            } else {
                nodeInstanceService.update(nodeType, serviceResource.getNodeInstance(), nodeProperties, nodeCapabilities, nodeAttributeValues);
            }
        }

        // description, authorized locations and authorizations can be updated even when deployed.
        // Note that changing the locations or authorizations won't impact already deployed applications using the service
        PatchUtil.set(serviceResource, "description", description, patch);
        updateLocations(serviceResource, locations);

        if (isDeployed && !isUpdateDeployed) {
            // un-deploying a service is authorized only if the service is not used.
            failIdUsed(serviceResource.getId());
        }
        if (!isDeployed && isUpdateDeployed) {
            // check that all required properties are defined
            if (nodeType == null) {
                nodeType = toscaTypeSearchService.findOrFail(NodeType.class, serviceResource.getNodeInstance().getNodeTemplate().getType(),
                        serviceResource.getNodeInstance().getTypeVersion());
            }
            nodeInstanceService.checkRequired(nodeType, serviceResource.getNodeInstance());
        }

        save(serviceResource, ensureUniqueness);
    }

    private void validateRelationshipTypes(ServiceResource serviceResource, final NodeType nodeType) {
        safe(serviceResource.getCapabilitiesRelationshipTypes()).forEach((capabilityName, relationshipTypeId) -> {
            RelationshipType relationshipType = toscaTypeSearchService.findByIdOrFail(RelationshipType.class, relationshipTypeId);
            String[] validTargets = relationshipType.getValidTargets();
            if (ArrayUtils.isNotEmpty(validTargets)) {
                CapabilityDefinition capabilityDefinition = nodeType.getCapabilities().stream().filter(c -> c.getId().equals(capabilityName)).findFirst().get();

                Csar csar = toscaTypeSearchService.getArchive(nodeType.getArchiveName(), nodeType.getArchiveVersion());
                Set<CSARDependency> allDependencies = new HashSet<>(safe(csar.getDependencies()));
                allDependencies.add(new CSARDependency(csar.getName(), csar.getVersion(), csar.getHash()));
                CapabilityType capabilityType = toscaTypeSearchService.getElementInDependencies(CapabilityType.class, capabilityDefinition.getType(),
                        allDependencies);
                Set<String> allAcceptedTypes = new HashSet<>();
                allAcceptedTypes.addAll(capabilityType.getDerivedFrom());
                allAcceptedTypes.add(capabilityType.getElementId());

                boolean isValid = false;
                for (String validTarget : validTargets) {
                    if (allAcceptedTypes.contains(validTarget)) {
                        isValid = true;
                        break;
                    }
                }

                if (!isValid) {
                    throw new IncompatibleHalfRelationshipException(
                            "[" + relationshipType.getId() + "] is not compatible with [" + capabilityType.getId() + "]");
                }
            }
        });

        safe(serviceResource.getRequirementsRelationshipTypes()).forEach((k, v) -> {
            toscaTypeSearchService.findByIdOrFail(RelationshipType.class, v);
        });
    }

    private void updateLocations(ServiceResource serviceResource, String[] locations) {
        if (locations == null) {
            return;
        }
        // Check what elements have changed.
        Set<String> removedLocations = CollectionUtils.safeNewHashSet(serviceResource.getLocationIds());
        Set<String> addedLocations = Sets.newHashSet();
        Set<String> newLocations = Sets.newHashSet();
        for (String locationId : locations) {
            if (removedLocations.contains(locationId)) {
                // This location was already affected
                removedLocations.remove(locationId);
                newLocations.add(locationId);
            } else {
                // This is an added element.
                if (!alienDAO.exist(Location.class, locationId)) {
                    throw new NotFoundException("Location with id <" + locationId + "> does not exist.");
                }
                addedLocations.add(locationId);
                newLocations.add(locationId);
            }
        }
        serviceResource.setLocationIds(newLocations.toArray(new String[newLocations.size()]));
        // Dispatch location changed events (not a big deal if the save is actually canceled as it just changed the update date).
        for (String locationId : addedLocations) {
            publisher.publishEvent(new OnLocationResourceChangeEvent(this, locationId));
        }
        for (String locationId : removedLocations) {
            publisher.publishEvent(new OnLocationResourceChangeEvent(this, locationId));
        }
    }

    private void failUpdateIfManaged(ServiceResource serviceResource, boolean patch, String name, String version, String nodeTypeStr, String nodeTypeVersion,
            Map<String, AbstractPropertyValue> nodeProperties, Map<String, Capability> nodeCapabilities, Map<String, String> nodeAttributeValues) {
        // Update operation is not allowed for managed services.
        // Patch operation is allowed only on the service description or locations authorized for matching.
        if (serviceResource.getEnvironmentId() != null) {
            if (!patch || name != null || version != null || nodeTypeStr != null || nodeTypeVersion != null || nodeProperties != null
                    || nodeCapabilities != null || nodeAttributeValues != null) {
                throw new UnsupportedOperationException(
                        "Alien managed services cannot be updated via Service API. Please use patch if you wish to change locations or authorizations.");
            }
        }
    }

    /**
     * Save the service resource and optionally checks that the name/version couple is unique.
     * Check must be done for new resource or when the name or version has changed.
     * 
     * @param serviceResource The service to save.
     * @param ensureUniqueness True if we should process unicity check, false if not.
     */
    public synchronized void save(ServiceResource serviceResource, boolean ensureUniqueness) {
        if (ensureUniqueness) {
            long count = alienDAO.buildQuery(ServiceResource.class)
                    .setFilters(fromKeyValueCouples("name", serviceResource.getName(), "version", serviceResource.getVersion())).count();
            if (count > 0) {
                throw new AlreadyExistException(
                        "A service with name <" + serviceResource.getName() + "> and version <" + serviceResource.getVersion() + "> already exists.");
            }
        }
        // ensure that the nested version just reflects the version.
        Version version = VersionUtil.parseVersion(serviceResource.getVersion());
        serviceResource.setNestedVersion(version);
        alienDAO.save(serviceResource);
        publisher.publishEvent(new ServiceChangedEvent(this, serviceResource.getId()));
    }

    public synchronized void save(ServiceResource serviceResource) {
        save(serviceResource, false);
    }

    /**
     * Delete a service resource.
     * 
     * @param id The id of the service resource.
     */
    public synchronized void delete(String id) {
        failIdUsed(id);
        alienDAO.delete(ServiceResource.class, id);
        // trigger an event: a service has been deleted
        publisher.publishEvent(new ServiceDeletedEvent(this, id));
    }

    private void failIdUsed(String id) {
        ServiceUsageRequestEvent serviceUsageRequestEvent = new ServiceUsageRequestEvent(this, id);
        publisher.publishEvent(serviceUsageRequestEvent);
        Usage[] usages = serviceUsageRequestEvent.getUsages();
        if (usages.length > 0) {
            throw new ServiceUsageException("Used services cannot be updated or deleted.", usages);
        }
    }

    @EventListener
    public synchronized void handleLocationDeleted(AfterLocationDeleted event) {
        // Remove the location in every service that referenced it
        GetMultipleDataResult<ServiceResource> serviceResourceResult = alienDAO.buildQuery(ServiceResource.class)
                .setFilters(singleKeyFilter("locationIds", event.getLocationId())).prepareSearch().search(0, Integer.MAX_VALUE);
        if (serviceResourceResult.getData() == null) {
            return;
        }
        for (ServiceResource serviceResource : serviceResourceResult.getData()) {
            Set<String> locations = CollectionUtils.safeNewHashSet(serviceResource.getLocationIds());
            locations.remove(event.getLocationId());
            serviceResource.setLocationIds(locations.toArray(new String[locations.size()]));
        }
        // bulk update
        alienDAO.save(serviceResourceResult.getData());
    }

    /**
     * Search for services authorized for this location.
     * 
     * @param locationId
     * @return
     */
    public List<ServiceResource> searchByLocation(String locationId) {
        GetMultipleDataResult<ServiceResource> result = this.search("", singleKeyFilter("locationIds", locationId), null, false, 0, Integer.MAX_VALUE);
        return Lists.newArrayList(result.getData());
    }

    /**
     * Get all services from a given type.
     * 
     * @param nodeType The type of services to lookup.
     * @param nodeTypeVersion The version of the services to lookup.
     * @return An array that contains all services for the given node type.
     */
    public ServiceResource[] getByNodeTypes(String nodeType, String nodeTypeVersion) {
        return alienDAO.buildQuery(ServiceResource.class)
                .setFilters(fromKeyValueCouples("nodeInstance.nodeTemplate.type", nodeType, "nodeInstance.typeVersion", nodeTypeVersion)).prepareSearch()
                .search(0, Integer.MAX_VALUE).getData();
    }

    /**
     * Check if a service can be accessed from a given location.
     * 
     * @param serviceId The id of the service.
     * @param locationId The id of the location.
     */
    public void isLocationAuthorized(String serviceId, String locationId) {
        ServiceResource serviceResource = alienDAO.findById(ServiceResource.class, serviceId);
        if (serviceResource.getLocationIds() != null) {
            for (String srvLocationId : serviceResource.getLocationIds()) {
                if (srvLocationId.equals(locationId)) {
                    return;
                }
            }
        }
        throw new AccessDeniedException("Current context does not have access to the service <" + serviceId + "> for location <" + locationId + ">");
    }

    @EventListener
    public void reportArchiveUsage(ArchiveUsageRequestEvent event) {
        ServiceResource[] serviceResources = alienDAO.buildQuery(ServiceResource.class)
                .setFilters(fromKeyValueCouples("dependency.name", event.getArchiveName(), "dependency.version", event.getArchiveVersion())).prepareSearch()
                .search(0, Integer.MAX_VALUE).getData();
        for (ServiceResource serviceResource : serviceResources) {
            Usage usage = new Usage(serviceResource.getName(), ServiceResource.class.getSimpleName().toLowerCase(), serviceResource.getId(), "");
            event.addUsage(usage);
        }
    }
}
