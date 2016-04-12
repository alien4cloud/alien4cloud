package alien4cloud.events;

import lombok.Getter;
import lombok.Setter;
import alien4cloud.model.components.IndexedNodeType;
import alien4cloud.orchestrators.plugin.IOrchestratorPluginFactory;

@Getter
@Setter
public class LocationTypeToBeIndexed extends LocationResourceEvent {

    private static final long serialVersionUID = 4523559660777770642L;

    private IndexedNodeType nodeType;

    private IOrchestratorPluginFactory<?, ?> orchestratorFactory;

    public LocationTypeToBeIndexed(Object source) {
        super(source);
    }
    
}
