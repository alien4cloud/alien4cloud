package alien4cloud.events;

import java.util.List;

import lombok.Getter;
import lombok.Setter;
import org.alien4cloud.tosca.model.Csar;
import alien4cloud.model.orchestrators.locations.Location;
import alien4cloud.orchestrators.plugin.IOrchestratorPluginFactory;

/**
 * Fired when a location is deleted, this event concerns a CSAR provided by the resource.
 */
@Getter
@Setter
public class LocationArchiveDeleteRequested extends LocationResourceEvent {

    private static final long serialVersionUID = 4523559660777770642L;

    /**
     * The CSAR provided by this location.
     */
    private Csar csar;

    /**
     * True if the CSAR has been effectively deleted. If the CSAR is exposed by another remaining location that expose the same CSAR, then this CSAR is not
     * removed.
     */
    private boolean deleted;

    /**
     * The remaining locations that exposes this CSAR.
     */
    private List<Location> locationsExposingArchive;
    
    private IOrchestratorPluginFactory<?, ?> orchestratorFactory;

    public LocationArchiveDeleteRequested(Object source) {
        super(source);
    }
    
}
