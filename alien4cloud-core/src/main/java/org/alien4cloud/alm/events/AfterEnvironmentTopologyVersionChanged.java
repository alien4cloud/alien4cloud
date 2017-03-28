package org.alien4cloud.alm.events;

import alien4cloud.events.AlienEvent;
import lombok.Getter;

@Getter
public class AfterEnvironmentTopologyVersionChanged extends AlienEvent {

    private String oldVersion;
    private String newVersion;
    private String environmentId;
    private String applicationId;

    public AfterEnvironmentTopologyVersionChanged(Object source, String oldVersion, String newVersion, String environmentId, String applicationId) {
        super(source);
        this.oldVersion = oldVersion;
        this.newVersion = newVersion;
        this.environmentId = environmentId;
        this.applicationId = applicationId;
    }
}
