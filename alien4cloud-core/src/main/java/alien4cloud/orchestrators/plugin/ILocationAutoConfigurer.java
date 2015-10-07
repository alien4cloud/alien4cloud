package alien4cloud.orchestrators.plugin;

import alien4cloud.model.orchestrators.locations.Location;

import java.util.List;

/**
 * Created to allow plugins to auto-configure the locations (if so users won't be able to configure them in ui)
 */
public interface ILocationAutoConfigurer {
    List<Location> getLocations();
}
