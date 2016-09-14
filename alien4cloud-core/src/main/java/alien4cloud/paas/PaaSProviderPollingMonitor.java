package alien4cloud.paas;

import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.elasticsearch.mapping.QueryHelper;

import com.google.common.collect.Maps;
import com.google.common.collect.Sets;

import alien4cloud.dao.IGenericSearchDAO;
import alien4cloud.dao.model.GetMultipleDataResult;
import alien4cloud.model.deployment.Deployment;
import alien4cloud.paas.model.AbstractMonitorEvent;
import alien4cloud.paas.model.PaaSDeploymentStatusMonitorEvent;
import alien4cloud.utils.MapUtil;
import alien4cloud.utils.TypeScanner;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;

/**
 * Monitor service to watch a deployed topologies for a given PaaS provider.
 */
@SuppressWarnings("unchecked")
@Slf4j
public class PaaSProviderPollingMonitor implements Runnable {
    private static final int MAX_POLLED_EVENTS = 500;
    private static final int MAX_LISTENER_RETRY = 3;
    private static final long LISTENER_FAIL_RETRY_SLEEP_MS = 10;

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
            /**
             * FIXME below is true for our own cloudify 3 provider implementation but this is not documented and orchestrators may not implement it that way.
             * We should do that in a different fashion in cloudify 3 provider probably and not impact alien that way
             **/
            // The PaaSDeploymentStatusMonitorEvent is an internal generated event and so do not take into account
            eventClasses.remove(PaaSDeploymentStatusMonitorEvent.class);
        } catch (ClassNotFoundException e) {
            log.info("No event class derived from {} found", AbstractMonitorEvent.class.getName());
        }
        Map<String, String[]> filter = Maps.newHashMap();
        filter.put("orchestratorId", new String[] { this.orchestratorId });

        // sort by filed date DESC
        QueryHelper.ISearchQueryBuilderHelper searchQueryHelperBuilder = monitorDAO.getQueryHelper().buildQuery()
                .types(eventClasses.toArray(new Class<?>[eventClasses.size()])).filters(filter).prepareSearch("deploymentmonitorevents")
                .fieldSort("date", true);

        // the first one is the one with the latest date
        GetMultipleDataResult lastestEventResult = monitorDAO.search(searchQueryHelperBuilder, 0, 10);
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
                    Date lastEventDate = lastPollingDate;
                    for (AbstractMonitorEvent event : auditEvents) {
                        // Enrich event with cloud id before saving them
                        event.setOrchestratorId(orchestratorId);
                        // If not set initialize a date for event or update the last event date (last polling)
                        if (event.getDate() > 0) {
                            Date eventDate = new Date(event.getDate());
                            lastEventDate = eventDate.after(lastEventDate) ? eventDate : lastEventDate;
                        } else {
                            event.setDate(System.currentTimeMillis());
                        }

                        // dispatch the event to all listeners
                        for (IPaasEventListener listener : listeners) {
                            dispatchEvent(listener, event);
                        }
                    }
                    monitorDAO.save(auditEvents);
                    if (lastEventDate != null) {
                        lastPollingDate = lastEventDate;
                    }
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

    /**
     * Dispatch an event to the registered listener.
     *
     * @param listener The listener to which to send the event.
     * @param event The event to dispatch.
     */
    private void dispatchEvent(IPaasEventListener listener, AbstractMonitorEvent event) {
        dispatchEvent(listener, event, 0);
    }

    /**
     * Dispatch an event to the registered listener.
     * 
     * @param listener The listener to which to send the event.
     * @param event The event to dispatch.
     * @param retry The current retry index (0 for first dispatch)
     */
    @SneakyThrows
    private void dispatchEvent(IPaasEventListener listener, AbstractMonitorEvent event, int retry) {
        try {
            if (listener.canHandle(event)) {
                listener.eventHappened(event);
            }
        } catch (Exception e) {
            log.error("Failed to dispatch event {} to listener {} retry {} on {}.", event.toString(), listener.toString(), retry, MAX_LISTENER_RETRY, e);
            // Even if that fails
            if (retry < MAX_LISTENER_RETRY) {
                Thread.sleep(LISTENER_FAIL_RETRY_SLEEP_MS);
                dispatchEvent(listener, event, retry + 1);
            }
        }
    }

    @Override
    public synchronized void run() {
        if (log.isTraceEnabled()) {
            log.trace("Poll scheduled");
        }
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
