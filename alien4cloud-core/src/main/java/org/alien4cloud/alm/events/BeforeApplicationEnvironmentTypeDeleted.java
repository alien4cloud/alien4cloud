package org.alien4cloud.alm.events;

import alien4cloud.events.AlienEvent;
import lombok.Getter;

/**
 * This event is dispatched before an application environment type is going to be deleted.
 */
@Getter
public class BeforeApplicationEnvironmentTypeDeleted extends AlienEvent {
    private final String applicationId;
    private final String applicationEnvironmentType;

    public BeforeApplicationEnvironmentTypeDeleted(Object source, String applicationId, String applicationEnvironmentType) {
        super(source);
        this.applicationId = applicationId;
        this.applicationEnvironmentType = applicationEnvironmentType;
    }
}
