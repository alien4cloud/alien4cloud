package alien4cloud.orchestrators.locations.events;

import alien4cloud.events.AlienEvent;
import lombok.Getter;

/**
 * Event to be triggered when a location resource has been updated.
 *
 * This may be triggered when a new on-demand resource is created or when a service is set as available on the location.
 *
 * The location service will catch this event and ensure
 */
@Getter
public class OnLocationResourceChangeEvent extends AlienEvent {
    private final String locationId;

    public OnLocationResourceChangeEvent(Object source, String locationId) {
        super(source);
        this.locationId = locationId;
    }
}