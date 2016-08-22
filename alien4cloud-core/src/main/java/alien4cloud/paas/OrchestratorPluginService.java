package alien4cloud.paas;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ScheduledFuture;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import javax.inject.Inject;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.stereotype.Component;

import com.google.common.collect.Maps;

import alien4cloud.dao.IGenericSearchDAO;
import alien4cloud.exception.AlreadyExistException;
import alien4cloud.orchestrators.plugin.IOrchestratorPlugin;
import alien4cloud.paas.exception.OrchestratorDisabledException;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Manages the paas providers currently used by an enabled Cloud in ALIEN.
 */
@Slf4j
@Component
public class OrchestratorPluginService implements IPaasEventService {
    @Resource(name = "alien-es-dao")
    private IGenericSearchDAO alienDao;
    @Resource(name = "alien-monitor-es-dao")
    private IGenericSearchDAO alienMonitorDao;
    @Resource(name = "paas-monitor-scheduler")
    private TaskScheduler scheduler;
    /** Interval in milliseconds on which to retrieve monitoring events from a PaaS provider. */
    @Value("${paas_monitor.monitor_interval_ms}")
    private long monitorIntervalMs = 1000 * 30;
    @Inject
    private DeploymentStatusEventHandler deploymentStatusEventHandler;

    private Map<String, Registration> monitorRegistrations = Maps.newHashMap();

    @SuppressWarnings("rawtypes")
    private List<IPaasEventListener> listeners = Collections.synchronizedList(new ArrayList<IPaasEventListener>());

    public OrchestratorPluginService() {
        log.info("Create new PaaSProvider instance.");
    }

    @Override
    public void addListener(IPaasEventListener<?> listener) {
        listeners.add(listener);
    }

    @PostConstruct
    public void init() {
        // Deployment status event handler should be the first handler has quite important.
        listeners.add(0, deploymentStatusEventHandler);
    }

    /**
     * Register an {@link IOrchestratorPlugin} for a given cloud.
     *
     * @param orchestratorId Id of the cloud.
     * @param instance Instance of the IOrchestratorPlugin for the given cloud.
     */
    public void register(String orchestratorId, IOrchestratorPlugin instance) {
        log.info("Register provider with id {}", orchestratorId);
        if (monitorRegistrations.containsKey(orchestratorId)) {
            throw new AlreadyExistException("Cloud [" + orchestratorId + "] has already been registered");
        }
        // create the polling monitor responsible to monitor this instance.
        PaaSProviderPollingMonitor monitor = new PaaSProviderPollingMonitor(alienDao, alienMonitorDao, instance, listeners, orchestratorId);
        ScheduledFuture<?> monitorFuture = scheduler.scheduleAtFixedRate(monitor, monitorIntervalMs);
        Registration registration = new Registration(instance, monitorFuture);
        monitorRegistrations.put(orchestratorId, registration);
    }

    /**
     * Remove the registration for the given cloud (will stop monitoring the cloud using the registered IOrchestratorPlugin).
     *
     * @param orchestratorId The id of the cloud for which to remove registration.
     */
    public IOrchestratorPlugin unregister(String orchestratorId) {
        log.info("Unregister provider with id {}", orchestratorId);
        Registration registration = monitorRegistrations.remove(orchestratorId);
        if (registration != null) {
            registration.registration.cancel(false);
            return registration.instance;
        } else {
            return null;
        }
    }

    /**
     * Get a registered IOrchestratorPlugin for a cloud.
     *
     * @param orchestratorId The id of the cloud for which to get the IOrchestratorPlugin instance.
     * @return The {@link IOrchestratorPlugin} for the given cloud or null if none is registered for the given cloud id.
     */
    public IOrchestratorPlugin get(String orchestratorId) {
        Registration registration = monitorRegistrations.get(orchestratorId);
        return registration == null ? null : registration.instance;
    }

    /**
     * Get a registered IOrchestratorPlugin for a cloud.
     *
     * @param orchestratorId The id of the cloud for which to get the IOrchestratorPlugin instance.
     * @return The {@link IOrchestratorPlugin} for the given cloud or throw OrchestratorDisabledException if none is registered for the given cloud id.
     */
    public IOrchestratorPlugin getOrFail(String orchestratorId) {
        Registration registration = monitorRegistrations.get(orchestratorId);
        if (registration == null) {
            throw new OrchestratorDisabledException("The orchestrator with id <" + orchestratorId + "> is not enabled or loaded yet.");
        }
        return registration == null ? null : registration.instance;
    }

    /**
     * A registration for a paasProvider and the associated monitoring registration.
     */
    @AllArgsConstructor(suppressConstructorProperties = true)
    private class Registration {
        private IOrchestratorPlugin instance;
        private ScheduledFuture<?> registration;
    }
}
