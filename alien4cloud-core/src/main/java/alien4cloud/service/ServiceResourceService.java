package alien4cloud.service;

import static alien4cloud.dao.FilterUtil.fromKeyValueCouples;

import java.util.Date;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import javax.annotation.Resource;
import javax.inject.Inject;

import alien4cloud.model.deployment.Deployment;
import org.alien4cloud.tosca.catalog.index.IToscaTypeSearchService;
import org.alien4cloud.tosca.model.definitions.AbstractPropertyValue;
import org.alien4cloud.tosca.model.templates.Capability;
import org.alien4cloud.tosca.model.types.NodeType;
import org.springframework.security.access.AuthorizationServiceException;
import org.springframework.stereotype.Service;

import alien4cloud.dao.IGenericSearchDAO;
import alien4cloud.dao.model.GetMultipleDataResult;
import alien4cloud.exception.AlreadyExistException;
import alien4cloud.exception.NotFoundException;
import alien4cloud.model.orchestrators.locations.Location;
import alien4cloud.model.service.ServiceResource;
import alien4cloud.paas.plan.ToscaNodeLifecycleConstants;
import alien4cloud.rest.utils.PatchUtil;
import alien4cloud.tosca.properties.constraints.exception.ConstraintValueDoNotMatchPropertyTypeException;
import alien4cloud.tosca.properties.constraints.exception.ConstraintViolationException;
import alien4cloud.utils.CollectionUtils;
import alien4cloud.utils.VersionUtil;
import alien4cloud.utils.version.Version;

/**
 * Manages services.
 *
 * FIXME: should be notified when a location is deleted in order to clean locationIds
 */
@Service
public class ServiceResourceService {
    @Resource(name = "alien-es-dao")
    private IGenericSearchDAO alienDAO;
    @Inject
    private NodeInstanceService nodeInstanceService;
    @Inject
    private IToscaTypeSearchService toscaTypeSearchService;

    /**
     * Creates a service.
     *
     * @param serviceName The unique name that defines the service from user point of view.
     * @param serviceVersion The id of the plugin used to communicate with the orchestrator.
     * @return The generated identifier for the service.
     */
    public String create(String serviceName, String serviceVersion, String serviceNodeType, String serviceNodeVersion) {
        ServiceResource serviceResource = new ServiceResource();
        // generate an unique id
        serviceResource.setId(UUID.randomUUID().toString());
        serviceResource.setName(serviceName);
        serviceResource.setVersion(serviceVersion);
        serviceResource.setCreationDate(new Date());

        // build a node instance from the given type
        NodeType nodeType = toscaTypeSearchService.findOrFail(NodeType.class, serviceNodeType, serviceNodeVersion);
        serviceResource.setNodeInstance(nodeInstanceService.create(nodeType, serviceNodeVersion));

        // ensure uniqueness and save
        save(serviceResource, true);

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
    public GetMultipleDataResult<ServiceResource> search(String searchText, Map<String, String[]> filters, String sortField, boolean desc, int from,
            int count) {
        if (sortField == null) {
            sortField = "name";
        }
        return alienDAO.buildSearchQuery(ServiceResource.class, searchText).prepareSearch().setFilters(filters).setFieldSort(sortField, desc).search(from,
                count);
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
            String[] locations) throws ConstraintValueDoNotMatchPropertyTypeException, ConstraintViolationException {
        update(resourceId, name, version, description, nodeType, nodeTypeVersion, nodeProperties, nodeCapabilities, nodeAttributeValues, locations, false);
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
     */
    public void patch(String resourceId, String name, String version, String description, String nodeType, String nodeTypeVersion,
            Map<String, AbstractPropertyValue> nodeProperties, Map<String, Capability> nodeCapabilities, Map<String, String> nodeAttributeValues,
            String[] locations) throws ConstraintValueDoNotMatchPropertyTypeException, ConstraintViolationException {
        update(resourceId, name, version, description, nodeType, nodeTypeVersion, nodeProperties, nodeCapabilities, nodeAttributeValues, locations, true);
    }

    private void update(String resourceId, String name, String version, String description, String nodeTypeStr, String nodeTypeVersion,
            Map<String, AbstractPropertyValue> nodeProperties, Map<String, Capability> nodeCapabilities, Map<String, String> nodeAttributeValues,
            String[] locations, boolean patch) throws ConstraintValueDoNotMatchPropertyTypeException, ConstraintViolationException {
        ServiceResource serviceResource = getOrFail(resourceId);
        failUpdateIfManaged(serviceResource);

        boolean ensureUniqueness = false;
        // Check if we try to update a running service.
        if (!ToscaNodeLifecycleConstants.INITIAL.equals(serviceResource.getNodeInstance().getAttributeValues().get(ToscaNodeLifecycleConstants.ATT_STATE))) {
            // Update operation is not allowed for running services.
            // Patch operation is allowed only on the service description or locations authorized for matching.
            if (!patch || name != null || version != null || nodeTypeStr != null || nodeTypeVersion != null || nodeProperties != null
                    || nodeCapabilities != null || nodeAttributeValues != null) {
                throw new AuthorizationServiceException(
                        "Update is not allowed on a running service, please use patch if you wish to change locations or authorizations.");
            }
        } else {
            ensureUniqueness = PatchUtil.set(serviceResource, "name", name, patch);
            ensureUniqueness = PatchUtil.set(serviceResource, "version", version, patch) || ensureUniqueness;

            // node instance type update
            PatchUtil.set(serviceResource.getNodeInstance().getNodeTemplate(), "type", nodeTypeStr, patch);
            PatchUtil.set(serviceResource.getNodeInstance(), "typeVersion", nodeTypeVersion, patch);

            // Node instance properties update
            NodeType nodeType = toscaTypeSearchService.findOrFail(NodeType.class, serviceResource.getNodeInstance().getNodeTemplate().getType(),
                    serviceResource.getNodeInstance().getTypeVersion());
            if (patch) {
                nodeInstanceService.patch(nodeType, serviceResource.getNodeInstance(), nodeProperties, nodeCapabilities, nodeAttributeValues);
            } else {
                serviceResource.getNodeInstance().getNodeTemplate().setProperties(nodeProperties);
                serviceResource.getNodeInstance().getNodeTemplate().setCapabilities(nodeCapabilities);
                serviceResource.getNodeInstance().setAttributeValues(nodeAttributeValues);
                nodeInstanceService.validate(nodeType, serviceResource.getNodeInstance());
            }
        }

        // description, authorized locations and authorizations can be updated even when deployed.
        // Note that changing the locations or authorizations won't impact already deployed applications using the service
        PatchUtil.set(serviceResource, "description", description, patch);
        updateLocations(serviceResource, locations);

        save(serviceResource, ensureUniqueness);
    }

    private void updateLocations(ServiceResource serviceResource, String[] locations) {
        if (locations == null) {
            return;
        }
        // Check what elements have changed.
        Set<String> previousLocations = CollectionUtils.safeNewHashSet(serviceResource.getLocationIds());
        for (String locationId : locations) {
            if (!previousLocations.contains(locationId) && !alienDAO.exist(Location.class, locationId)) {
                throw new NotFoundException("Location with id <" + locationId + "> does not exist.");
            }
        }
        serviceResource.setLocationIds(locations);
    }

    private void failUpdateIfManaged(ServiceResource serviceResource) {
        if (serviceResource.getDeploymentId() != null) {
            throw new AuthorizationServiceException("Alien managed services cannot be updated via Service API.");
        }
    }

    /**
     * Save the service resource and optionally checks that the name/version couple is unique.
     * Check must be done for new resource or when the name or version has changed.
     * 
     * @param serviceResource The service to save.
     * @param ensureUniqueness True if we should process unicity check, false if not.
     */
    private synchronized void save(ServiceResource serviceResource, boolean ensureUniqueness) {
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
    }

    /**
     * Delete a service resource.
     * 
     * @param id The id of the service resource.
     */
    public synchronized Deployment[] delete(String id) {
        // FIXME check usage
        GetMultipleDataResult<Deployment> usageResult = alienDAO.buildQuery(Deployment.class)
                .setFilters(fromKeyValueCouples("endDate", null, "serviceResourceIds", id)).prepareSearch().search(0, Integer.MAX_VALUE);
        if (usageResult.getTotalResults() > 0) {
            return usageResult.getData();
        }
        alienDAO.delete(ServiceResource.class, id);
        return null;
    }
}
