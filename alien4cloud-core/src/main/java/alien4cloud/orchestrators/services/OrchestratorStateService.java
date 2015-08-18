package alien4cloud.orchestrators.services;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import alien4cloud.dao.IGenericSearchDAO;
import alien4cloud.orchestrators.plugin.IOrchestrator;
import alien4cloud.orchestrators.plugin.IOrchestratorFactory;
import alien4cloud.paas.IConfigurablePaaSProviderFactory;
import alien4cloud.paas.IPaaSProviderFactory;
import lombok.extern.slf4j.Slf4j;
import alien4cloud.dao.model.GetMultipleDataResult;
import alien4cloud.model.cloud.Cloud;
import alien4cloud.model.orchestrators.Orchestrator;
import alien4cloud.model.orchestrators.OrchestratorState;
import alien4cloud.paas.IPaaSProvider;
import alien4cloud.paas.exception.PluginConfigurationException;

import javax.annotation.Resource;

/**
 * Service to manage state of an orchestrator
 */
@Slf4j
public class OrchestratorStateService {
    @Resource(name = "alien-es-dao")
    private IGenericSearchDAO alienDAO;
    @Resource
    private OrchestratorFactoriesRegistry orchestratorFactoriesRegistry;

    /**
     * Initialize all orchestrator that have a non-disabled state.
     * Note: Each orchestrator initialization is down in it's own thread so it doesn't impact application startup or other orchestrator connection.
     *
     * @return a list of futures for those who want to wait for task to be done.
     */
    public List<Future<?>> initialize() {
        ExecutorService executorService = Executors.newCachedThreadPool();
        List<Future<?>> futures = new ArrayList<Future<?>>();
        int from = 0;
        long totalResult;
        do {
            GetMultipleDataResult<Orchestrator> enabledOrchestrators = get(null, true, from, 20, null);
            if (enabledOrchestrators.getData() == null) {
                return futures;
            }

            for (final Orchestrator orchestrator : enabledOrchestrators.getData()) {
                // error in initialization and timeouts should not impact startup time of Alien 4 cloud and other PaaS Providers.
                Future<?> future = executorService.submit(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            enable(orchestrator);
                        } catch (Throwable t) {
                            // we have to catch everything as we don't know what a plugin can do here and cannot interrupt startup.
                            disableOnInitFailure(orchestrator, t);
                        }
                    }
                });
                futures.add(future);
            }
            from += enabledOrchestrators.getData().length;
            totalResult = enabledOrchestrators.getTotalResults();
        } while (from < totalResult);
        return futures;
    }

    private void disableOnInitFailure(Orchestrator orchestrator, Throwable t) {
        log.error("Failed to start cloud Enable cloud <" + orchestrator.getId() + "> <" + orchestrator.getName() + "> - will switch it to disabled", t);
        disable(orchestrator, true);
    }

    /**
     * Enable an orchestrator.
     * 
     * @param orchestrator The orchestrator to enable.
     */
    public synchronized void enable(Orchestrator orchestrator) {
        if (orchestrator.getState().equals(OrchestratorState.DISABLED)) {

        } else {
            log.debug("Request to enable ignored: orchestrator {} (id: {}) is already enabled", orchestrator.getName(), orchestrator.getId());
        }
    }

    /**
     *
     */
    private void load(Orchestrator orchestrator) {
        log.info("Loading and connecting orchestrator {} (id: {})", orchestrator.getName(), orchestrator.getId());
        // switch the state to connecting
        orchestrator.setState(OrchestratorState.CONNECTING);
        alienDAO.save(orchestrator);

        IOrchestratorFactory orchestratorFactory = orchestratorFactoriesRegistry.getPluginBean(orchestrator.getPluginId(), orchestrator.getPluginBean());
        IOrchestrator orchestratorInstance = orchestratorFactory.newInstance();

        

        // get a PaaSProvider bean and configure it.
        IPaaSProviderFactory passProviderFactory = paaSProviderFactoriesService.getPluginBean(cloud.getPaasPluginId(), cloud.getPaasPluginBean());
        // create and configure a IPaaSProvider instance.
        IPaaSProvider provider = null;
        if (passProviderFactory instanceof IConfigurablePaaSProviderFactory) {
            provider = ((IConfigurablePaaSProviderFactory<Object>) passProviderFactory).newInstance();
        } else {
            provider = passProviderFactory.newInstance();
        }
        refreshCloud(cloud, provider);
        provider.init(deploymentService.getCloudActiveDeploymentContexts(cloud.getId()));
        // register the IPaaSProvider for the cloud.
        paaSProviderService.register(cloud.getId(), provider);
    }

    /**
     * Disable an orchestrator.
     * 
     * @param orchestrator The orchestrator to disable.
     * @param force If true the orchestrator is disabled even if some deployments are currently running.
     */
    public synchronized void disable(Orchestrator orchestrator, boolean force) {

    }
}