package alien4cloud.paas;

import java.util.Date;
import java.util.List;
import java.util.Set;

import lombok.extern.slf4j.Slf4j;

import org.elasticsearch.mapping.QueryHelper.SearchQueryHelperBuilder;

import alien4cloud.dao.IGenericSearchDAO;
import alien4cloud.dao.model.GetMultipleDataResult;
import alien4cloud.paas.model.AbstractMonitorEvent;
import alien4cloud.paas.model.PaaSInstanceStateMonitorEvent;
import alien4cloud.utils.TypeScanner;

/**
 * Monitor service to watch a deployed topologies for a given PaaS provider.
 */
@SuppressWarnings("unchecked")
@Slf4j
public class PaaSProviderPollingMonitor implements Runnable {
    private static final int MAX_POLLED_EVENTS = 100;
    private final IGenericSearchDAO dao;
    private final IPaaSProvider paaSProvider;
    private Date lastPollingDate;
    private List<IPaasEventListener> listeners;

    /**
     * Create a new instance of the {@link PaaSProviderPollingMonitor} to monitor the given paas provider.
     *
     * @param paaSProvider The paas provider to monitor.
     */
    public PaaSProviderPollingMonitor(IGenericSearchDAO dao, IPaaSProvider paaSProvider, List<IPaasEventListener> listeners) {
        this.dao = dao;
        this.paaSProvider = paaSProvider;
        this.listeners = listeners;
        Set<Class<?>> eventClasses = null;
        try {
            eventClasses = TypeScanner.scanTypes("alien4cloud.paas.model", AbstractMonitorEvent.class);
        } catch (ClassNotFoundException e) {
            log.info("No event class derived from {} found", AbstractMonitorEvent.class.getName());
        }

        // sort by filed date DESC
        SearchQueryHelperBuilder searchQueryHelperBuilder = dao.getQueryHelper().buildSearchQuery("deploymentmonitorevents")
                .types(eventClasses.toArray(new Class<?>[eventClasses.size()])).fieldSort("date", true);

        // the first one is the one with the latest date
        GetMultipleDataResult lastestEventResult = dao.search(searchQueryHelperBuilder, 0, 1);
        if (lastestEventResult.getData().length > 0) {
            AbstractMonitorEvent lastEvent = (AbstractMonitorEvent) lastestEventResult.getData()[0];
            Date lastEventDate = new Date(lastEvent.getDate());
            log.info("Recovering events from the last in elasticsearch {} of type {}", lastEventDate, lastEvent.getClass().getName());
            this.lastPollingDate = lastEventDate;
        } else {
            this.lastPollingDate = new Date();
        }
    }

    @Override
    public void run() {
        AbstractMonitorEvent[] auditEvents = paaSProvider.getEventsSince(lastPollingDate, MAX_POLLED_EVENTS);
        Date monitorEvent = null;
        if (auditEvents != null && auditEvents.length > 0) {
            dao.save(auditEvents);
            for (AbstractMonitorEvent event : auditEvents) {
                if (event instanceof PaaSInstanceStateMonitorEvent) {
                    PaaSInstanceStateMonitorEvent instanceStateMonitorEvent = (PaaSInstanceStateMonitorEvent) event;
                    monitorEvent = new Date(instanceStateMonitorEvent.getDate());
                    if (monitorEvent.after(lastPollingDate)) {
                        lastPollingDate = monitorEvent;
                    }
                }
            }
            for (IPaasEventListener listener : listeners) {
                for (AbstractMonitorEvent event : auditEvents) {
                    if (listener.getEventType().isAssignableFrom(event.getClass())) {
                        listener.eventHappened(event);
                    }
                }
            }
        }
    }
}
