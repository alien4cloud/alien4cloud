package org.alien4cloud.alm.events;

import alien4cloud.events.AlienEvent;
import alien4cloud.security.Subject;
import lombok.Getter;

/**
 * This event is dispatched <b>after</b> a permission is revoked for one or more subjects on a resource.
 */
@Getter
public class AfterPermissionRevokedEvent extends AlienEvent {
    private Subject subjectType;
    private String[] subjects;
    /* The summary of the resource on which the permission is being revoked */
    private BeforePermissionRevokedEvent.OnResource on;

    public AfterPermissionRevokedEvent(Object source, BeforePermissionRevokedEvent.OnResource on, Subject subjectType, String... subjects) {
        super(source);
        this.subjectType = subjectType;
        this.subjects = subjects;
        this.on = on;
    }
}
