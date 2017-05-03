package org.alien4cloud.alm.service.events;

import java.util.Collections;
import java.util.List;

import com.google.common.collect.Lists;

import alien4cloud.events.AlienEvent;
import alien4cloud.model.common.Usage;
import lombok.Getter;

/**
 * This event is dispatched to find usage on a service, all service consumers can intercept this event when triggered and add their usage consumptions.
 */
public class ServiceUsageRequestEvent extends AlienEvent {
    private final List<Usage> usages = Lists.newArrayList();
    @Getter
    private final String serviceId;

    public ServiceUsageRequestEvent(Object source, String serviceId) {
        super(source);
        this.serviceId = serviceId;
    }

    /**
     * Add usage information.
     * 
     * @param usages The usage information to be added.
     */
    public synchronized void addUsages(Usage[] usages) {
        Collections.addAll(this.usages, usages);
    }

    public Usage[] getUsages() {
        return usages.toArray(new Usage[usages.size()]);
    }
}