package org.alien4cloud.tosca.catalog.events;

import alien4cloud.events.AlienEvent;
import alien4cloud.tosca.model.ArchiveRoot;
import lombok.Getter;

/**
 * Event triggered before an archive is indexed.
 */
@Getter
public class BeforeArchiveIndexed extends AlienEvent {
    private final ArchiveRoot archiveRoot;

    public BeforeArchiveIndexed(Object source, ArchiveRoot archiveRoot) {
        super(source);
        this.archiveRoot = archiveRoot;
    }
}