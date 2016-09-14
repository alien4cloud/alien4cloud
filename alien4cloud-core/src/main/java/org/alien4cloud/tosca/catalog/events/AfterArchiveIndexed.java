package org.alien4cloud.tosca.catalog.events;

import alien4cloud.events.AlienEvent;
import alien4cloud.tosca.model.ArchiveRoot;

/**
 * Event triggered after an archive has been indexed.
 */
public class AfterArchiveIndexed extends AlienEvent {
    private ArchiveRoot archiveRoot;

    public AfterArchiveIndexed(Object source) {
        super(source);
    }
}