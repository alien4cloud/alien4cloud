package alien4cloud.events;

import java.util.List;

import lombok.Getter;
import lombok.Setter;
import alien4cloud.model.components.Csar;
import alien4cloud.model.orchestrators.locations.Location;
import alien4cloud.orchestrators.plugin.IOrchestratorPluginFactory;

@Getter
@Setter
public class LocationArchiveDeleteRequested extends LocationResourceEvent {

    private static final long serialVersionUID = 4523559660777770642L;

    private Csar csar;

    private boolean deleted;

    private List<Location> locationsExposingArchive;
    
    private IOrchestratorPluginFactory<?, ?> orchestratorFactory;

    public LocationArchiveDeleteRequested(Object source) {
        super(source);
    }
    
}
