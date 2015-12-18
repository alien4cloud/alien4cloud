package alien4cloud.paas;

import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Set;

import lombok.extern.slf4j.Slf4j;

import org.elasticsearch.mapping.QueryHelper.SearchQueryHelperBuilder;

import alien4cloud.dao.IGenericSearchDAO;
import alien4cloud.dao.model.GetMultipleDataResult;
import alien4cloud.model.deployment.Deployment;
import alien4cloud.paas.model.AbstractMonitorEvent;
import alien4cloud.utils.MapUtil;
import alien4cloud.utils.TypeScanner;

import com.google.common.collect.Maps;
import com.google.common.collect.Sets;

/**
 * Monitor service to watch a deployed topologies for a given PaaS provider.
 */
@SuppressWarnings("unchecked")
@Slf4j
public class PaaSProviderPollingMonitor implements Runnable {
    private static final int MAX_POLLED_EVENTS = 500;
    private final IGenericSearchDAO dao;
    private final IGenericSearchDAO monitorDAO;
    private final IPaaSProvider paaSProvider;
    private Date lastPollingDate;
    @SuppressWarnings("rawtypes")
    private List<IPaasEventListener> listeners;
    private PaaSEventsCallback paaSEventsCallback;
    private String orchestratorId;
    private boolean hasDeployments = false;
    private boolean getEventsInProgress = false;

    /**
     * Create a new instance of the {@link PaaSProviderPollingMonitor} to monitor the given paas provider.
     *
     * @param paaSProvider The paas provider to monitor.
     */
    @SuppressWarnings("rawtypes")
    public PaaSProviderPollingMonitor(IGenericSearchDAO dao, IGenericSearchDAO monitorDAO, IPaaSProvider paaSProvider, List<IPaasEventListener> listeners,
            String orchestratorId) {
        this.orchestratorId = orchestratorId;
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
        filter.put("orchestratorId", new String[] { this.orchestratorId });
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
        public void onSuccess(AbstractMonitorEvent[] auditEvents) {
            synchronized (PaaSProviderPollingMonitor.this) {
                if (log.isTraceEnabled()) {
                    log.trace("Polled from date {}", lastPollingDate);
                }
                if (log.isDebugEnabled() && auditEvents != null && auditEvents.length > 0) {
                    log.debug("Saving events for orchestrator {}", orchestratorId);
                    for (AbstractMonitorEvent event : auditEvents) {
                        log.debug(event.toString());
                    }
                }
                if (auditEvents != null && auditEvents.length > 0) {
                    for (AbstractMonitorEvent event : auditEvents) {
                        // Enrich event with cloud id before saving them
                        event.setOrchestratorId(orchestratorId);
                    }
                    for (IPaasEventListener listener : listeners) {
                        for (AbstractMonitorEvent event : auditEvents) {
                            if (listener.canHandle(event)) {
                                listener.eventHappened(event);
                            }
                            if (event.getDate() > 0) {
                                Date eventDate = new Date(event.getDate());
                                lastPollingDate = eventDate.after(lastPollingDate) ? eventDate : lastPollingDate;
                            } else {
                                event.setDate(System.currentTimeMillis());
                            }
                        }
                    }
                    monitorDAO.save(auditEvents);
                }
                getEventsInProgress = false;
            }
        }

        @Override
        public void onFailure(Throwable throwable) {
            synchronized (PaaSProviderPollingMonitor.this) {
                getEventsInProgress = false;
                // Make it re-verify if has deployment returns something in order to no loop infinitely
                // If the PaaS is down, there might be a chance that the deployment has been marked as failed
                hasDeployments = false;
                log.error("Error happened while trying to retrieve events from PaaS provider", throwable);
            }
        }
    }

    @Override
    public synchronized void run() {
        if (getEventsInProgress) {
            // Get events since is running
            return;
        }
        getEventsInProgress = true;
        if (hasDeployments) {
            paaSProvider.getEventsSince(lastPollingDate, MAX_POLLED_EVENTS, paaSEventsCallback);
        } else {
            getEventsInProgress = false;
            hasDeployments = getActiveDeployment() != null;
        }
    }

    private Deployment getActiveDeployment() {
        Deployment deployment = null;
        GetMultipleDataResult<Deployment> dataResult = dao.search(Deployment.class, null,
                MapUtil.newHashMap(new String[] { "orchestratorId", "endDate" }, new String[][] { new String[] { orchestratorId }, new String[] { null } }), 1);
        if (dataResult.getData() != null && dataResult.getData().length > 0) {
            deployment = dataResult.getData()[0];
        }
        return deployment;
    }
}
