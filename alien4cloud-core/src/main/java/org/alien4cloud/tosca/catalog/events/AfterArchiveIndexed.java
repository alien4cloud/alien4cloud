package org.alien4cloud.tosca.catalog.events;

import alien4cloud.events.AlienEvent;
import alien4cloud.tosca.model.ArchiveRoot;
import lombok.Getter;

/**
 * Event triggered after an archive has been indexed.
 */
@Getter
public class AfterArchiveIndexed extends AlienEvent {
    private final ArchiveRoot archiveRoot;

    public AfterArchiveIndexed(Object source, ArchiveRoot archiveRoot) {
        super(source);
        this.archiveRoot = archiveRoot;
    }
}