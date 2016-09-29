package org.alien4cloud.tosca.catalog.events;

import alien4cloud.events.AlienEvent;
import lombok.Getter;

/**
 * This event is dispatched by alien4cloud after an archived has been removed from the catalog.
 *
 * Note that it is not dispatched when an archive content is overriden by more recent one. If you need to process things when archive is overriden look at the
 * Before/AfterArchiveIndexed.
 */
@Getter
public class AfterArchiveDeleted extends AlienEvent {
    private final String archiveId;

    public AfterArchiveDeleted(Object source, String archiveId) {
        super(source);
        this.archiveId = archiveId;
    }
}
