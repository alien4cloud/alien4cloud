package org.alien4cloud.alm.events;

import alien4cloud.model.service.ServiceResource;
import lombok.Getter;

/**
 * An event published when a managed {@link ServiceResource} is reset (back to initial state).
 */
@Getter
public class ManagedServiceResetEvent extends ManagedServiceEvent {

    private static final long serialVersionUID = 4523559660777770642L;

    public ManagedServiceResetEvent(Object source, final ServiceResource serviceResource) {
        super(source, serviceResource);
    }

}
