package alien4cloud.paas;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ScheduledFuture;

import javax.annotation.Resource;

import lombok.AllArgsConstructor;

import org.elasticsearch.mapping.QueryHelper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.stereotype.Component;

import alien4cloud.dao.IGenericSearchDAO;
import alien4cloud.exception.AlreadyExistException;

import com.google.common.collect.Maps;

/**
 * Manages the paas providers currently used by an enabled Cloud in ALIEN.
 */
@Component
public class PaaSProviderService implements IPaasEventService {
    @Resource
    private QueryHelper queryHelper;
    @Resource(name = "alien-es-dao")
    private IGenericSearchDAO alienDao;
    @Resource(name = "alien-monitor-es-dao")
    private IGenericSearchDAO alienMonitorDao;
    @Resource(name = "paas-monitor-scheduler")
    private TaskScheduler scheduler;
    /** Interval in milliseconds on which to retrieve monitoring events from a PaaS provider. */
    @Value("${paas_monitor.monitor_interval_ms}")
    private long monitorIntervalMs = 1000 * 30;

    private Map<String, Registration> monitorRegistrations = Maps.newHashMap();

    @SuppressWarnings("rawtypes")
    private List<IPaasEventListener> listeners = Collections.synchronizedList(new ArrayList<IPaasEventListener>());

    @Override
    public void addListener(IPaasEventListener<?> listener) {
        listeners.add(listener);
    }

    /**
     * Register an {@link IPaaSProvider} for a given cloud.
     *
     * @param cloudId Id of the cloud.
     * @param instance Instance of the IPaaSProvider for the given cloud.
     */
    public void register(String cloudId, IPaaSProvider instance) {
        if (monitorRegistrations.containsKey(cloudId)) {
            throw new AlreadyExistException("Cloud [" + cloudId + "] has already been registered");
        }
        // create the polling monitor responsible to monitor this instance.
        PaaSProviderPollingMonitor monitor = new PaaSProviderPollingMonitor(alienMonitorDao, instance, listeners);
        ScheduledFuture<?> monitorFuture = scheduler.scheduleAtFixedRate(monitor, monitorIntervalMs);
        Registration registration = new Registration(instance, monitorFuture);
        monitorRegistrations.put(cloudId, registration);
    }

    /**
     * Remove the registration for the given cloud (will stop monitoring the cloud using the registered IPaaSProvider).
     *
     * @param cloudId The id of the cloud for which to remove registration.
     */
    public void unregister(String cloudId) {
        Registration registration = monitorRegistrations.remove(cloudId);
        if (registration != null) {
            registration.registration.cancel(false);
        }
    }

    /**
     * Get a registered IPaaSProvider for a cloud.
     *
     * @param cloudId The id of the cloud for which to get the IPaaSProvider instance.
     * @return The {@link IPaaSProvider} for the given cloud or null if none is registered for the given cloud id.
     */
    public IPaaSProvider getPaaSProvider(String cloudId) {
        Registration registration = monitorRegistrations.get(cloudId);
        return registration == null ? null : registration.providerInstance;
    }

    /**
     * A registration for a paasProvider and the associated monitoring registration.
     */
    @AllArgsConstructor
    private class Registration {
        private IPaaSProvider providerInstance;
        private ScheduledFuture<?> registration;
    }
}
