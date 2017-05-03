package org.alien4cloud.tosca.catalog.events;

import java.util.Collection;
import java.util.Collections;
import java.util.List;

import com.google.common.collect.Lists;

import alien4cloud.events.AlienEvent;
import alien4cloud.model.common.Usage;
import lombok.Getter;

/**
 * This event is dispatched to find usage on an archive from the catalog, all archive consumers can intercept this event when triggered and record their usage.
 */
public class ArchiveUsageRequestEvent extends AlienEvent {
    private final List<Usage> usages = Lists.newArrayList();
    @Getter
    private final String archiveName;
    @Getter
    private final String archiveVersion;

    public ArchiveUsageRequestEvent(Object source, String archiveName, String archiveVersion) {
        super(source);
        this.archiveName = archiveName;
        this.archiveVersion = archiveVersion;
    }

    /**
     * Add usage information.
     *
     * @param usages The usage information to be added.
     */
    public synchronized void addUsages(Usage[] usages) {
        Collections.addAll(this.usages, usages);
    }

    /**
     * Add usage information.
     *
     * @param usages The usage information to be added.
     */
    public synchronized void addUsages(Collection<Usage> usages) {
        this.usages.addAll(usages);
    }

    /**
     * Add a usage information.
     * 
     * @param usage The usage information.
     */
    public synchronized void addUsage(Usage usage) {
        this.usages.add(usage);
    }

    public List<Usage> getUsages() {
        return usages;
    }
}