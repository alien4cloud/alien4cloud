package org.alien4cloud.alm.events;

import alien4cloud.events.AlienEvent;
import lombok.Getter;

/**
 * This event is dispatched before an application version is going to be deleted.
 */
@Getter
public class BeforeApplicationVersionDeleted extends AlienEvent {
    private final String applicationId;
    private final String versionId;

    public BeforeApplicationVersionDeleted(Object source, String applicationId, String versionId) {
        super(source);
        this.applicationId = applicationId;
        this.versionId = versionId;
    }
}
