package alien4cloud.events;

import lombok.Getter;
import lombok.Setter;
import alien4cloud.model.components.IndexedNodeType;
import alien4cloud.model.orchestrators.locations.LocationResourceTemplate;

/**
 * This event is fired when a location resource template is created (through UI or auto-config feature).
 */
@Getter
@Setter
public class LocationTemplateCreated extends LocationResourceEvent {

    private static final long serialVersionUID = 4523559660777770642L;

    private IndexedNodeType nodeType;

    private LocationResourceTemplate template;

    public LocationTemplateCreated(Object source) {
        super(source);
    }
    
}
