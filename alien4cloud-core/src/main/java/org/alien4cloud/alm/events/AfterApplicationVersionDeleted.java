package org.alien4cloud.alm.events;

import alien4cloud.events.AlienEvent;
import lombok.Getter;

/**
 * This event is dispatched after an application version has been deleted.
 */
@Getter
public class AfterApplicationVersionDeleted extends AlienEvent {
    private final String applicationId;
    private final String versionId;

    public AfterApplicationVersionDeleted(Object source, String applicationId, String versionId) {
        super(source);
        this.applicationId = applicationId;
        this.versionId = versionId;
    }
}
