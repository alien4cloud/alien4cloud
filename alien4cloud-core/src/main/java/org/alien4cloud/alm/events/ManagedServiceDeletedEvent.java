package org.alien4cloud.alm.events;

import alien4cloud.model.service.ServiceResource;
import lombok.Getter;

/**
 * An event published when a managed {@link ServiceResource} is deleted (no more exposed as a service).
 */
@Getter
public class ManagedServiceDeletedEvent extends ManagedServiceEvent {

    private static final long serialVersionUID = 4523559660777770642L;

    public ManagedServiceDeletedEvent(Object source, final ServiceResource serviceResource) {
        super(source, serviceResource);
    }

}
