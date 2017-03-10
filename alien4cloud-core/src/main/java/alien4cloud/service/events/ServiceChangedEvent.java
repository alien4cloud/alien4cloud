package alien4cloud.service.events;

import alien4cloud.events.AlienEvent;

/**
 * This event is triggered when a service attribute changes (this includes the service state attribute).
 */
public class ServiceChangedEvent extends AlienEvent {
    private final String serviceId;

    public ServiceChangedEvent(Object source, String serviceId) {
        super(source);
        this.serviceId = serviceId;
    }
}
