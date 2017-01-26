package org.alien4cloud.alm.events;

import alien4cloud.events.AlienEvent;
import lombok.Getter;

/**
 * This event is dispatched after an application version has been deleted.
 */
@Getter
public class AfterApplicationEnvironmentDeleted extends AlienEvent {
    private static final long serialVersionUID = -1126617350064097857L;

    private final String applicationId;
    private final String applicationEnvironmentId;

    public AfterApplicationEnvironmentDeleted(Object source, String applicationId, String applicationEnvironmentId) {
        super(source);
        this.applicationId = applicationId;
        this.applicationEnvironmentId = applicationEnvironmentId;
    }
}