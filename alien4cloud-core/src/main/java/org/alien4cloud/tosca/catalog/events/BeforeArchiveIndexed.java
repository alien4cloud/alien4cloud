package org.alien4cloud.tosca.catalog.events;

import alien4cloud.events.AlienEvent;
import alien4cloud.tosca.model.ArchiveRoot;

/**
 * Event triggered before an archive is indexed.
 */
public class BeforeArchiveIndexed extends AlienEvent {
    private ArchiveRoot archiveRoot;

    public BeforeArchiveIndexed(Object source) {
        super(source);
    }
}