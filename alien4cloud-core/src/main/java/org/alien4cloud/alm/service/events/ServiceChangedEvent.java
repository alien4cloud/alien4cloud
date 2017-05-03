package org.alien4cloud.alm.service.events;

import alien4cloud.events.AlienEvent;
import lombok.Getter;

/**
 * This event is triggered when a service attribute changes (this includes the service state attribute).
 */
@Getter
public class ServiceChangedEvent extends AlienEvent {
    private final String serviceId;

    public ServiceChangedEvent(Object source, String serviceId) {
        super(source);
        this.serviceId = serviceId;
    }
}