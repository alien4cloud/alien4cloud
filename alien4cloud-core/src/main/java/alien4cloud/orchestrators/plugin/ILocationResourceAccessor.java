package alien4cloud.orchestrators.plugin;

import alien4cloud.model.orchestrators.locations.LocationResourceTemplate;

import java.util.List;

/**
 * Allows to access the resources of a defined location.
 */
public interface ILocationResourceAccessor {
    /**
     * Get all configured resources for the current location.
     * 
     * @return A list with all configured resources for the location this instance is associated with.
     */
    List<LocationResourceTemplate> getResources();

    /**
     * Get all the resources of the given type for a given location.
     * 
     * @param type The type of the location for which to get resources.
     * @return A list with all configured resources of the given type for the location this instance is associated with.
     */
    List<LocationResourceTemplate> getResources(String type);
}