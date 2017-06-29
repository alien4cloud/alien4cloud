package org.alien4cloud.alm.events;

import alien4cloud.model.service.ServiceResource;
import lombok.Getter;

/**
 * An event published when a managed {@link ServiceResource} is created.
 */
@Getter
public abstract class ManagedServiceEvent extends ServiceEvent {

    private static final long serialVersionUID = 4523559660777770642L;

    private ServiceResource serviceResource;

    public ManagedServiceEvent(Object source, final ServiceResource serviceResource) {
        super(source);
        this.serviceResource = serviceResource;
    }

}
