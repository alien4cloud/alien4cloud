package org.alien4cloud.alm.events;

import alien4cloud.events.AlienEvent;
import alien4cloud.security.Subject;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

/**
 * This event is dispatched <b>before</b> a permission is revoked for one or more subjects on a resource.
 */
@Getter
public class BeforePermissionRevokedEvent extends AlienEvent {
    private Subject subjectType;
    private String[] subjects;
    /* The summary of the resource on which the permission is being revoked */
    private OnResource on;

    public BeforePermissionRevokedEvent(Object source, OnResource on, Subject subjectType, String... subjects) {
        super(source);
        this.subjectType = subjectType;
        this.subjects = subjects;
        this.on = on;
    }

    @AllArgsConstructor
    @Getter
    @Setter
    public static class OnResource {
        private Class<?> clazz;
        private String id;
    }
}
