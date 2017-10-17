package alien4cloud.events;

import org.alien4cloud.tosca.model.types.AbstractInheritableToscaType;

import alien4cloud.model.orchestrators.locations.AbstractLocationResourceTemplate;
import lombok.Getter;
import lombok.Setter;

/**
 * This event is fired when a location resource template is created (through UI or auto-config feature).
 */
@Getter
@Setter
public class LocationTemplateCreated extends LocationResourceEvent {

    private static final long serialVersionUID = 4523559660777770642L;

    private AbstractInheritableToscaType toscaType;

    private AbstractLocationResourceTemplate template;

    public LocationTemplateCreated(Object source) {
        super(source);
    }
    
}
