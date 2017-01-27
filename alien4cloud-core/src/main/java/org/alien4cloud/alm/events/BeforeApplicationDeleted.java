package org.alien4cloud.alm.events;

import alien4cloud.events.AlienEvent;
import lombok.Getter;

/**
 * This event is dispatched before an application is going to be deleted.
 */
@Getter
public class BeforeApplicationDeleted extends AlienEvent {
    private String applicationId;

    public BeforeApplicationDeleted(Object source, String applicationId) {
        super(source);
        this.applicationId = applicationId;
    }
}
