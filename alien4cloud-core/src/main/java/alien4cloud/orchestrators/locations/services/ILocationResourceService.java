package alien4cloud.orchestrators.locations.services;

import java.util.Collection;
import java.util.List;
import java.util.Map;

import alien4cloud.model.orchestrators.locations.Location;
import alien4cloud.model.orchestrators.locations.LocationCustomResourceTemplate;
import alien4cloud.model.orchestrators.locations.LocationResourceTemplate;
import alien4cloud.model.orchestrators.locations.LocationResources;
import alien4cloud.orchestrators.plugin.ILocationResourceAccessor;
import alien4cloud.plugin.aop.Overridable;
import alien4cloud.tosca.properties.constraints.exception.ConstraintValueDoNotMatchPropertyTypeException;
import alien4cloud.tosca.properties.constraints.exception.ConstraintViolationException;

@Overridable
public interface ILocationResourceService {

    /**
     * Get the list of resources definitions for a given orchestrator.
     *
     * @param location the location.
     * @return A list of resource definitions for the given location.
     */
    LocationResources getLocationResources(Location location);

    /**
     * Get the list of resources definitions for a given orchestrator.
     *
     * @param location the location.
     * @return A list of resource definitions for the given location.
     */
    LocationResources getLocationResourcesFromOrchestrator(Location location);

    LocationResourceTypes getLocationResourceTypes(Collection<LocationResourceTemplate> resourceTemplates);

    /**
     * Create an instance of an ILocationResourceAccessor that will perform queries on LocationResourceTemplate for a given location.
     *
     * @param locationId Id of the location for which to get the accessor.
     * @return An instance of the ILocationResourceAccessor.
     */
    ILocationResourceAccessor accessor(String locationId);

    List<LocationResourceTemplate> getResourcesTemplates(String locationId);

    Map<String, LocationResourceTemplate> getMultiple(Collection<String> ids);

    @Deprecated
    LocationCustomResourceTemplate addResourceTemplate(String locationId, String resourceName, String resourceTypeName);

    LocationCustomResourceTemplate addCustomResourceTemplate(String locationId, String resourceName, String resourceTypeName, String archiveName, String archiveVersion);

    void deleteResourceTemplate(String resourceId);

    LocationResourceTemplate getOrFail(String resourceId);

    void merge(Object mergeRequest, String resourceId);

    void setTemplateProperty(String resourceId, String propertyName, Object propertyValue)
            throws ConstraintValueDoNotMatchPropertyTypeException, ConstraintViolationException;

    void setTemplateCapabilityProperty(LocationResourceTemplate resourceTemplate, String capabilityName, String propertyName,
            Object propertyValue) throws ConstraintViolationException, ConstraintValueDoNotMatchPropertyTypeException;

    void setTemplateCapabilityProperty(String resourceId, String capabilityName, String propertyName, Object propertyValue)
            throws ConstraintViolationException, ConstraintValueDoNotMatchPropertyTypeException;

    /**
     * Auto configure resources for the given location.
     *
     * @param locationId Id of the location.
     */
    List<LocationResourceTemplate> autoConfigureResources(String locationId);

    /**
     * Delete all generated {@link LocationResourceTemplate} for a given location
     *
     * @param locationId
     */
    void deleteGeneratedResources(String locationId);

    void saveResource(Location location, LocationResourceTemplate resourceTemplate);

    void saveResource(LocationResourceTemplate resourceTemplate);

}