package alien4cloud.events;

import lombok.Getter;
import lombok.Setter;
import alien4cloud.model.orchestrators.locations.Location;

/**
 * An event linked to a location defined type or template.
 */
@Getter
@Setter
public abstract class LocationResourceEvent extends AlienEvent {

    private static final long serialVersionUID = 4523559660777770642L;

    private Location location;
    
    public LocationResourceEvent(Object source) {
        super(source);
    }
    
}
