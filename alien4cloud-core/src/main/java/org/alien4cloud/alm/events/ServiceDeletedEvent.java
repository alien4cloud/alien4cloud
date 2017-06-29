package org.alien4cloud.alm.events;

import alien4cloud.model.service.ServiceResource;
import lombok.Getter;

/**
 * An event published when a {@link ServiceResource} is deleted (no more exposed as a service).
 */
@Getter
public class ServiceDeletedEvent extends ServiceEvent {

    private static final long serialVersionUID = 4523559660777770642L;

    private String serviceResourceId;

    public ServiceDeletedEvent(Object source, final String serviceResourceId) {
        super(source);
        this.serviceResourceId = serviceResourceId;
    }

}
