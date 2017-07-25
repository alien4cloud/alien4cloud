package org.alien4cloud.alm.deployment.configuration.events;

import alien4cloud.events.AlienEvent;
import alien4cloud.model.application.ApplicationEnvironment;
import lombok.Getter;
import org.alien4cloud.tosca.model.templates.NodeGroup;

import java.util.Map;

/**
 * This event is triggered when the location associated with an environment has changed.
 *
 * You can process this event in order to reset location related configurations.
 */
@Getter
public class OnMatchedLocationChangedEvent extends AlienEvent {
    private ApplicationEnvironment environment;
    private String orchestratorId;
    private Map<String, String> groupToLocation;

    public OnMatchedLocationChangedEvent(Object source, ApplicationEnvironment environment, String orchestratorId, Map<String, String> groupToLocation) {
        super(source);
        this.environment = environment;
        this.orchestratorId = orchestratorId;
        this.groupToLocation = groupToLocation;
    }
}
