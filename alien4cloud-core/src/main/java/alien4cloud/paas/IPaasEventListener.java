package alien4cloud.paas;

import alien4cloud.paas.model.AbstractMonitorEvent;

public interface IPaasEventListener<T extends AbstractMonitorEvent> {

    /**
     * Called when an event happened
     * 
     * @param event the monitor event to be delivered
     */
    void eventHappened(T event);

    /**
     * The event's type that this listener is waiting for
     * 
     * @return the type of the awaited events
     */
    Class<T> getEventType();
}
