package alien4cloud.orchestrators.locations.events;

import alien4cloud.events.AlienEvent;
import lombok.Getter;

/**
 * Event dispatched after a location has been deleted.
 */
@Getter
public class AfterLocationDeleted extends AlienEvent {
    private final String locationId;

    public AfterLocationDeleted(Object source, String locationId) {
        super(source);
        this.locationId = locationId;
    }
}
