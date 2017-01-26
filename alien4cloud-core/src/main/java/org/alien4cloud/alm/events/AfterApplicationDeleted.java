package org.alien4cloud.alm.events;

import alien4cloud.events.AlienEvent;
import lombok.Getter;

/**
 * This event is dispatched after an application has been deleted.
 */
@Getter
public class AfterApplicationDeleted extends AlienEvent {
    private final String applicationId;

    public AfterApplicationDeleted(Object source, String applicationId) {
        super(source);
        this.applicationId = applicationId;
    }
}
