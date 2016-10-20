package org.alien4cloud.tosca.catalog.events;

import alien4cloud.events.AlienEvent;
import lombok.Getter;

@Getter
public class BeforeArchivePromoted extends AlienEvent {

    private String archiveId;

    public BeforeArchivePromoted(Object source, String archiveId) {
        super(source);
        this.archiveId = archiveId;
    }
}
