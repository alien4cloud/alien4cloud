package org.alien4cloud.alm.events;

import alien4cloud.events.AlienEvent;
import lombok.Getter;

/**
 * This event is dispatched before an application environment is going to be deleted.
 */
@Getter
public class BeforeApplicationEnvironmentDeleted extends AlienEvent {
    private final String applicationId;
    private final String applicationEnvironmentId;

    public BeforeApplicationEnvironmentDeleted(Object source, String applicationId, String applicationEnvironmentId) {
        super(source);
        this.applicationId = applicationId;
        this.applicationEnvironmentId = applicationEnvironmentId;
    }
}
