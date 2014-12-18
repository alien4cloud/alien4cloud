package alien4cloud.paas;

import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Set;

import lombok.extern.slf4j.Slf4j;

import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.mapping.QueryHelper.SearchQueryHelperBuilder;

import alien4cloud.dao.IGenericSearchDAO;
import alien4cloud.dao.model.GetMultipleDataResult;
import alien4cloud.model.deployment.Deployment;
import alien4cloud.paas.model.AbstractMonitorEvent;
import alien4cloud.utils.TypeScanner;

import com.google.common.collect.Maps;
import com.google.common.collect.Sets;

/**
 * Monitor service to watch a deployed topologies for a given PaaS provider.
 */
@SuppressWarnings("unchecked")
@Slf4j
public class PaaSProviderPollingMonitor implements Runnable {
    private static final int MAX_POLLED_EVENTS = 100;
    private final IGenericSearchDAO dao;
    private final IGenericSearchDAO monitorDAO;
    private final IPaaSProvider paaSProvider;
    private Date lastPollingDate;
    @SuppressWarnings("rawtypes")
    private List<IPaasEventListener> listeners;
    private PaaSEventsCallback paaSEventsCallback;
    private String cloudId;
    private boolean hasDeployments;

    /**
     * Create a new instance of the {@link PaaSProviderPollingMonitor} to monitor the given paas provider.
     *
     * @param paaSProvider The paas provider to monitor.
     */
    @SuppressWarnings("rawtypes")
    public PaaSProviderPollingMonitor(IGenericSearchDAO dao, IGenericSearchDAO monitorDAO, IPaaSProvider paaSProvider, List<IPaasEventListener> listeners,
            String cloudId) {
        this.cloudId = cloudId;
        this.dao = dao;
        this.monitorDAO = monitorDAO;
        this.paaSProvider = paaSProvider;
        this.listeners = listeners;
        Set<Class<?>> eventClasses = Sets.newHashSet();
        try {
            eventClasses = TypeScanner.scanTypes("alien4cloud.paas.model", AbstractMonitorEvent.class);
        } catch (ClassNotFoundException e) {
            log.info("No event class derived from {} found", AbstractMonitorEvent.class.getName());
        }
        Map<String, String[]> filter = Maps.newHashMap();
        filter.put("cloudId", new String[] { this.cloudId });
        // sort by filed date DESC
        SearchQueryHelperBuilder searchQueryHelperBuilder = monitorDAO.getQueryHelper().buildSearchQuery("deploymentmonitorevents")
                .types(eventClasses.toArray(new Class<?>[eventClasses.size()])).filters(filter).fieldSort("date", true);

        // the first one is the one with the latest date
        GetMultipleDataResult lastestEventResult = monitorDAO.search(searchQueryHelperBuilder, 0, 1);
        if (lastestEventResult.getData().length > 0) {
            AbstractMonitorEvent lastEvent = (AbstractMonitorEvent) lastestEventResult.getData()[0];
            Date lastEventDate = new Date(lastEvent.getDate());
            log.info("Recovering events from the last in elasticsearch {} of type {}", lastEventDate, lastEvent.getClass().getName());
            this.lastPollingDate = lastEventDate;
        } else {
            this.lastPollingDate = new Date();
            log.debug("No monitor events found, the last polling date will be current date {}", this.lastPollingDate);
        }
        paaSEventsCallback = new PaaSEventsCallback();
    }

    private class PaaSEventsCallback implements IPaaSCallback<AbstractMonitorEvent[]> {

        @Override
        public void onData(AbstractMonitorEvent[] auditEvents) {
            if (log.isDebugEnabled()) {
                log.debug("Polled from date {}", lastPollingDate);
                if (auditEvents != null && auditEvents.length > 0) {
                    log.debug("Saving events for cloud {}", cloudId);
                    for (AbstractMonitorEvent event : auditEvents) {
                        log.debug(event.toString());
                    }
                }
            }
            if (auditEvents != null && auditEvents.length > 0) {
                for (AbstractMonitorEvent event : auditEvents) {
                    // Enrich event with cloud id before saving them
                    event.setCloudId(cloudId);
                }
                monitorDAO.save(auditEvents);
                for (IPaasEventListener listener : listeners) {
                    for (AbstractMonitorEvent event : auditEvents) {
                        if (listener.canHandle(event)) {
                            listener.eventHappened(event);
                        }
                        Date eventDate = new Date(event.getDate());
                        lastPollingDate = eventDate.after(lastPollingDate) ? eventDate : lastPollingDate;
                    }
                }
            }
        }

        @Override
        public void onFailure(Throwable throwable) {
            log.error("Error happened while trying to retrieve events from PaaS provider", throwable);
        }
    }

    @Override
    @SuppressWarnings("rawtypes")
    public void run() {
        // TODO Work-around for cloudify 3
        if (hasDeployments) {
            paaSProvider.getEventsSince(lastPollingDate, MAX_POLLED_EVENTS, paaSEventsCallback);
        } else {
            hasDeployments = this.dao.count(Deployment.class, QueryBuilders.matchAllQuery()) > 0;
        }
    }
}
