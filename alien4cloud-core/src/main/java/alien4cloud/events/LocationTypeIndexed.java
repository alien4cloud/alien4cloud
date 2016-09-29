package alien4cloud.events;

import lombok.Getter;
import lombok.Setter;
import org.alien4cloud.tosca.model.types.NodeType;
import alien4cloud.orchestrators.plugin.IOrchestratorPluginFactory;

/**
 * This event is fired just after a type exposed by a location has been proposed for indexation.
 * <p>
 * This means that this type is now associated with this location.
 */
@Getter
@Setter
public class LocationTypeIndexed extends LocationResourceEvent {

    private static final long serialVersionUID = 4523559660777770642L;

    private NodeType nodeType;

    private IOrchestratorPluginFactory<?, ?> orchestratorFactory;

    public LocationTypeIndexed(Object source) {
        super(source);
    }
    
}
