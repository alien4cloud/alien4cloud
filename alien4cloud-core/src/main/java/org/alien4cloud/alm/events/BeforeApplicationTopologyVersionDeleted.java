package org.alien4cloud.alm.events;

import alien4cloud.events.AlienEvent;
import lombok.Getter;

/**
 * This event is dispatched after an application topology version has been deleted.
 */
@Getter
public class BeforeApplicationTopologyVersionDeleted extends AlienEvent {
    private final String applicationId;
    private final String versionId;
    private final String topologyVersion;

    public BeforeApplicationTopologyVersionDeleted(Object source, String applicationId, String versionId, String topologyVersion) {
        super(source);
        this.applicationId = applicationId;
        this.versionId = versionId;
        this.topologyVersion = topologyVersion;
    }
}
