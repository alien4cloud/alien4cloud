package alien4cloud.service;

import static alien4cloud.dao.FilterUtil.fromKeyValueCouples;

import java.util.Arrays;
import java.util.Date;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import javax.annotation.Resource;
import javax.inject.Inject;

import alien4cloud.dao.IESQueryBuilderHelper;
import org.alien4cloud.tosca.model.instances.NodeInstance;
import org.springframework.security.access.AuthorizationServiceException;
import org.springframework.stereotype.Service;

import com.google.common.collect.Sets;

import alien4cloud.dao.IGenericSearchDAO;
import alien4cloud.dao.model.GetMultipleDataResult;
import alien4cloud.exception.AlreadyExistException;
import alien4cloud.exception.NotFoundException;
import alien4cloud.model.orchestrators.locations.Location;
import alien4cloud.model.service.ServiceResource;
import alien4cloud.tosca.context.ToscaContextual;
import alien4cloud.utils.CollectionUtils;
import alien4cloud.utils.VersionUtil;
import alien4cloud.utils.version.Version;

/**
 * Manages services.
 *
 * TODO: should be notified when a location is deleted in order to clean locationIds
 */
@Service
public class ServiceResourceService {
    @Resource(name = "alien-es-dao")
    private IGenericSearchDAO alienDAO;
    @Inject
    private NodeInstanceService nodeInstanceService;

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
        serviceResource.setNodeInstance(nodeInstanceService.create(serviceNodeType, serviceNodeVersion));

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
     * @param resourceId The id of the service resource to update.
     * @param name The optional new name of the service.
     * @param version The optional new version of the service.
     * @param description The optional new description of the service.
     * @param nodeInstance The optional new nodeInstance of the service.
     * @param locations The optional new locations of the service.
     */
    public void update(String resourceId, String name, String version, String description, NodeInstance nodeInstance, String[] locations) {
        ServiceResource serviceResource = getOrFail(resourceId);

        // if we don't update neither the name and version then there is no need for uniqueness check.
        boolean ensureUniqueness = false;
        if (name != null && !serviceResource.getName().equals(name)) {
            failUpdateIfManaged(serviceResource);
            serviceResource.setName(name);
            ensureUniqueness = true;
        }
        if (version != null && !serviceResource.getVersion().equals(version)) {
            failUpdateIfManaged(serviceResource);
            serviceResource.setVersion(version);
            ensureUniqueness = true;
        } else {
            // FIXME actually do we authorize to update version and type ? Only when not used ? Or should we let the user delete and create a new service ? Or
            // only when the state is initial ?
            // if the version has not changed it is not authorized to change the node instance type.
            if (nodeInstance != null && nodeInstance.getNodeTemplate() != null
                    && !serviceResource.getNodeInstance().getNodeTemplate().getType().equals(nodeInstance.getNodeTemplate().getType())) {
                throw new IllegalArgumentException("Update request cannot change the node's type if version is not changed.");
            }
        }
        if (description != null) {
            failUpdateIfManaged(serviceResource);
            serviceResource.setDescription(description);
        }
        if (nodeInstance != null) {
            failUpdateIfManaged(serviceResource);
            // FIXME perform validation of the node instance.
            nodeInstanceService.validate(nodeInstance);
            serviceResource.setNodeInstance(nodeInstance);
        }
        if (locations != null) {
            // Update the list of locations and ensure that every defined id actually exists.
            Set<String> previousLocations = CollectionUtils.safeNewHashSet(serviceResource.getLocationIds());
            for (String locationId : locations) {
                if (!previousLocations.contains(locationId) && !alienDAO.exist(Location.class, locationId)) {
                    throw new NotFoundException("Location with id <" + locationId + "> does not exist.");
                }
            }
            serviceResource.setLocationIds(locations);
        }

        save(serviceResource, ensureUniqueness);
    }

    private void failUpdateIfManaged(ServiceResource serviceResource) {
        if (serviceResource.getDeploymentId() != null) {
            throw new AuthorizationServiceException("Alien managed services cannot be updated via Service API.");
        }
    }

    /**
     * Remove a list of locations on which the service is accessible for matching.
     * 
     * @param resourceId The id of the location.
     * @param newLocationIds The ids of the locations to add.
     * @return The location list after update.
     */
    public String[] addLocations(String resourceId, String[] newLocationIds) {
        ServiceResource serviceResource = getOrFail(resourceId);
        Set<String> updatedLocationIds = CollectionUtils.safeNewHashSet(serviceResource.getLocationIds());
        for (String newLocationId : newLocationIds) {
            if (!updatedLocationIds.contains(newLocationId)) {
                // ensure location exist
                if (!alienDAO.exist(Location.class, newLocationId)) {
                    throw new NotFoundException("Location with id <" + newLocationId + "> does not exist.");
                }
                updatedLocationIds.add(newLocationId);
            }
        }
        serviceResource.setLocationIds(updatedLocationIds.toArray(new String[updatedLocationIds.size()]));
        save(serviceResource, false);
        return serviceResource.getLocationIds();
    }

    /**
     * Remove a list of locations for which the service is not accessible for matching anymore.
     *
     * @param resourceId The id of the location.
     * @param revokedLocationIds The ids of the locations to remove.
     * @return The location list after update.
     */
    public String[] removeLocations(String resourceId, String[] revokedLocationIds) {
        ServiceResource serviceResource = getOrFail(resourceId);

        String[] existingIds = serviceResource.getLocationIds();
        if (existingIds != null && existingIds.length > 0) {
            Set<String> updatedLocationIds = Sets.newHashSet(Arrays.asList(existingIds));
            updatedLocationIds.removeAll(Arrays.asList(revokedLocationIds));
            serviceResource.setLocationIds(updatedLocationIds.toArray(new String[updatedLocationIds.size()]));
            save(serviceResource, false);
        }
        return serviceResource.getLocationIds();
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
    public synchronized void delete(String id) {
        // FIXME check usage
        alienDAO.delete(ServiceResource.class, id);
    }
}
