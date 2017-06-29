package org.alien4cloud.alm.events;

import alien4cloud.events.AlienEvent;
import alien4cloud.model.service.ServiceResource;
import lombok.Getter;

/**
 * Something happend to a {@link ServiceResource}.
 */
@Getter
public abstract class ServiceEvent extends AlienEvent {

    private static final long serialVersionUID = 4523559660777770642L;

    public ServiceEvent(Object source) {
        super(source);
    }

}
