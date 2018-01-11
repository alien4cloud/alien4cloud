package alien4cloud.orchestrators.locations.events;

import alien4cloud.events.AlienEvent;
import alien4cloud.model.orchestrators.locations.Location;

public class AfterLocationCreated extends AlienEvent {
    private final Location location;

    public AfterLocationCreated(Object source, Location location) {
        super(source);
        this.location = location;
    }
}
