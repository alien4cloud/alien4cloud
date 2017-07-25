package org.alien4cloud.alm.deployment.configuration.events;

import alien4cloud.events.AlienEvent;
import alien4cloud.model.application.ApplicationEnvironment;
import lombok.Getter;

/**
 * This event is triggered to instruct the copy of deployment configurations from an source environment to a target environment.
 */
@Getter
public class OnDeploymentConfigCopyEvent extends AlienEvent {
    private ApplicationEnvironment sourceEnvironment;
    private ApplicationEnvironment targetEnvironment;

    public OnDeploymentConfigCopyEvent(Object source, ApplicationEnvironment sourceEnvironment, ApplicationEnvironment targetEnvironment) {
        super(source);
        this.sourceEnvironment = sourceEnvironment;
        this.targetEnvironment = targetEnvironment;
    }
}