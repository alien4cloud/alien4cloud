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
     * Whether can handle or not the happened
     *
     * @return the true id this listener can handle this event, false if not
     */
    boolean canHandle(T event);

}
