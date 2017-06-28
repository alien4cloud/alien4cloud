package org.alien4cloud.alm.events;

import alien4cloud.model.deployment.DeploymentTopology;
import alien4cloud.model.service.ServiceResource;
import lombok.Getter;

/**
 * An event published when a managed {@link ServiceResource} is updated (generally started).
 */
@Getter
public class ManagedServiceUpdatedEvent extends ManagedServiceEvent {

    private static final long serialVersionUID = 4523559660777770642L;

    private DeploymentTopology topology;

    public ManagedServiceUpdatedEvent(Object source, final ServiceResource serviceResource, final DeploymentTopology topology) {
        super(source, serviceResource);
        this.topology = topology;
    }

}
