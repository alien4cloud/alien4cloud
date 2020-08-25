package alien4cloud.orchestrators.services;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Executors;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import javax.annotation.Resource;
import javax.inject.Inject;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.ListeningExecutorService;
import com.google.common.util.concurrent.MoreExecutors;

import org.alien4cloud.tosca.model.CSARDependency;
import org.aspectj.weaver.ast.Or;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

import alien4cloud.dao.IGenericSearchDAO;
import alien4cloud.dao.model.GetMultipleDataResult;
import alien4cloud.deployment.DeploymentService;
import alien4cloud.exception.AlreadyExistException;
import alien4cloud.model.common.Usage;
import alien4cloud.model.deployment.Deployment;
import alien4cloud.model.orchestrators.Orchestrator;
import alien4cloud.model.orchestrators.OrchestratorConfiguration;
import alien4cloud.model.orchestrators.OrchestratorState;
import alien4cloud.orchestrators.events.AfterOrchestratorEnabled;
import alien4cloud.orchestrators.events.BeforeOrchestratorDisabled;
import alien4cloud.orchestrators.locations.services.LocationService;
import alien4cloud.orchestrators.locations.services.PluginArchiveIndexer;
import alien4cloud.orchestrators.plugin.ILocationAutoConfigurer;
import alien4cloud.orchestrators.plugin.IOrchestratorPlugin;
import alien4cloud.orchestrators.plugin.IOrchestratorPluginFactory;
import alien4cloud.paas.IPaaSProviderConfiguration;
import alien4cloud.paas.OrchestratorPluginService;
import alien4cloud.paas.exception.PluginConfigurationException;
import alien4cloud.utils.MapUtil;
import lombok.extern.slf4j.Slf4j;

/**
 * Service to manage state of an orchestrator
 */
@Slf4j
@Component
public class OrchestratorStateService {
    @Resource(name = "alien-es-dao")
    private IGenericSearchDAO alienDAO;
    @Inject
    private OrchestratorConfigurationService orchestratorConfigurationService;
    @Inject
    private OrchestratorPluginService orchestratorPluginService;
    @Inject
    private DeploymentService deploymentService;
    @Inject
    private OrchestratorService orchestratorService;
    @Inject
    private LocationService locationService;
    @Inject
    private PluginArchiveIndexer archiveIndexer;

    @Inject
    private ApplicationEventPublisher publisher;

    // Lock per Orchestrator ID
    private final Map<String, ReentrantLock> mapLock = Maps.newConcurrentMap();

    /**
     * Unload all orchestrators from JVM memory, it's typically to refresh/reload code
     */
    public void unloadAllOrchestrators() {
        List<Orchestrator> enabledOrchestratorList = orchestratorService.getAllEnabledOrchestrators();
        if (enabledOrchestratorList != null && !enabledOrchestratorList.isEmpty()) {
            log.info("Unloading orchestrators");
            for (final Orchestrator orchestrator : enabledOrchestratorList) {
                // un-register the orchestrator.
                IOrchestratorPlugin orchestratorInstance = orchestratorPluginService.unregister(orchestrator.getId());
                if (orchestratorInstance != null) {
                    IOrchestratorPluginFactory orchestratorFactory = orchestratorService.getPluginFactory(orchestrator);
                    orchestratorFactory.destroy(orchestratorInstance);
                }
            }
            log.info("{} Orchestrators Unloaded", enabledOrchestratorList.size());
        }
    }

    /**
     * Initialize all orchestrator that have a non-disabled state.
     */
    public ListenableFuture<?> initialize() {
        return initialize(null);
    }

    /**
     * Initialize all orchestrator that have a non-disabled state.
     * Note: Each orchestrator initialization is down in it's own thread so it doesn't impact application startup or other orchestrator connection.
     *
     * @param callback the callback to be executed when initialize finish
     */
    public ListenableFuture<?> initialize(FutureCallback callback) {
        ListeningExecutorService executorService = MoreExecutors.listeningDecorator(Executors.newCachedThreadPool());
        try {
            List<ListenableFuture<?>> futures = new ArrayList<>();
            // get all the orchestrator that are not disabled
            final List<Orchestrator> enabledOrchestratorList = orchestratorService.getAllEnabledOrchestrators();

            if (enabledOrchestratorList == null || enabledOrchestratorList.isEmpty()) {
                return Futures.immediateFuture(null);
            }
            log.info("Initializing orchestrators");
            for (final Orchestrator orchestrator : enabledOrchestratorList) {
                // error in initialization and timeouts should not impact startup time of Alien 4 cloud and other PaaS Providers.
                ListenableFuture<?> future = executorService.submit(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            load(orchestrator);
                        } catch (AlreadyExistException e) {
                            log.info("Orchestrator was already loaded at initialization for {}.", orchestrator.getId());
                        } catch (Exception e) {
                            // we have to catch everything as we don't know what a plugin can do here and cannot interrupt startup.
                            // Any orchestrator that failed to load will be considered as DISABLED as the registration didn't occurred
                            log.error("Unexpected error in plugin", e);
                            orchestrator.setState(OrchestratorState.DISABLED);
                            alienDAO.save(orchestrator);
                        }
                    }
                });
                futures.add(future);
            }
            ListenableFuture<?> combinedFuture = Futures.allAsList(futures);
            if (callback != null) {
                Futures.addCallback(combinedFuture, callback);
            }
            Futures.addCallback(combinedFuture, new FutureCallback<Object>() {
                @Override
                public void onSuccess(Object result) {
                    log.info("{} Orchestrators loaded", enabledOrchestratorList.size());
                }

                @Override
                public void onFailure(Throwable t) {
                    log.error("Unable to load orchestrators", t);
                }
            });
            return combinedFuture;
        } finally {
            executorService.shutdown();
        }
    }

    /**
     * Enable an orchestrator.
     *
     * @param orchestrator The orchestrator to enable.
     */
    public void enable(Orchestrator orchestrator) throws PluginConfigurationException {
        Lock lock = lockFor(orchestrator);

        try {
            lock.lock();

            if (orchestrator.getState().equals(OrchestratorState.DISABLED)) {
                load(orchestrator);

            } else {
                log.debug("Request to enable ignored: orchestrator {} (id: {}) is already enabled", orchestrator.getName(), orchestrator.getId());
                throw new AlreadyExistException("Orchestrator {} is already instanciated.");
            }
        } finally {
            lock.unlock();
        }
    }

    /**
     * Load and connect the given orchestrator.
     *
     * @param orchestrator the orchestrator to load and connect.
     */
    private void load(Orchestrator orchestrator) throws PluginConfigurationException {
        log.info("Loading and connecting orchestrator {} (id: {})", orchestrator.getName(), orchestrator.getId());
        // check that the orchestrator is not already loaded.
        if (orchestratorPluginService.get(orchestrator.getId()) != null) {
            throw new AlreadyExistException("Plugin is already loaded.");
        }
        // switch the state to connecting
        orchestrator.setState(OrchestratorState.CONNECTING);
        alienDAO.save(orchestrator);

        // Set the configuration for the provider
        OrchestratorConfiguration orchestratorConfiguration = orchestratorConfigurationService.getConfigurationOrFail(orchestrator.getId());
        try {
            Object configuration = orchestratorConfigurationService.configurationAsValidObject(orchestrator.getId(),
                    orchestratorConfiguration.getConfiguration());

            if (configuration instanceof IPaaSProviderConfiguration) {
                ((IPaaSProviderConfiguration)configuration).setOrchestratorName(orchestrator.getName());
                ((IPaaSProviderConfiguration)configuration).setOrchestratorId(orchestrator.getId());
            }

            IOrchestratorPluginFactory orchestratorFactory = orchestratorService.getPluginFactory(orchestrator);
            IOrchestratorPlugin<Object> orchestratorInstance = orchestratorFactory.newInstance(configuration);
            // just kept for backward compatibility
            orchestratorInstance.setConfiguration(orchestrator.getId(), configuration);

            // index the archive in alien catalog
            archiveIndexer.indexOrchestratorArchives(orchestratorFactory, orchestratorInstance);

            try {
                Deployment[] deployments = deploymentService.getOrchestratorActiveDeployments(orchestrator.getId());
                Map<String, String> deploymentIdsMap = Maps.newHashMap();
                Map<String, Deployment> deploymentsMap = Maps.newHashMap();
                if (deployments != null) {
                    for (Deployment deployment : deployments) {
                        deploymentIdsMap.put(deployment.getOrchestratorDeploymentId(), deployment.getId());
                        deploymentsMap.put(deployment.getOrchestratorDeploymentId(), deployment);
                    }
                }
                // connect the orchestrator
                Set<String> reallyActiveDeployments = orchestratorInstance.init(deploymentIdsMap);
                deploymentsMap.forEach((deploymentPaasId, deployment) -> {
                    if (!reallyActiveDeployments.contains(deploymentPaasId)) {
                        // this deployment is not known by the orchestrator, maybe it has been undeployed during downtime
                        log.info("Deployment {} ({}) no longer exists in orchestrator deployments, maybe undeployed during downtime, marking it as undeployed", deployment.getId(), deployment.getOrchestratorDeploymentId());
                        deploymentService.markUndeployed(deployment);
                    }
                });

            } catch(Exception e) {
                // Destroy contexts
                orchestratorFactory.destroy(orchestratorInstance);

                // Reset the connection state
                orchestrator.setState(OrchestratorState.DISABLED);
                alienDAO.save(orchestrator);

                // Propagate the exception
                throw e;
            }

            // register the orchestrator instance to be polled for updates
            orchestratorPluginService.register(orchestrator.getId(), orchestratorInstance);
            orchestrator.setState(OrchestratorState.CONNECTED);
            alienDAO.save(orchestrator);
            if (orchestratorInstance instanceof ILocationAutoConfigurer) {
                // trigger locations auto-configurations
                locationService.autoConfigure(orchestrator, (ILocationAutoConfigurer) orchestratorInstance);
            }
            indexLocationsArchives(orchestrator);
            publisher.publishEvent(new AfterOrchestratorEnabled(this, orchestrator));

        } catch (IOException e) {
            // TODO: change orchestrator state ?
            throw new PluginConfigurationException("Failed convert configuration json in object.", e);
        }

        // TODO move below in a thread to perform plugin loading and connection asynchronously

    }

    private void indexLocationsArchives(Orchestrator orchestrator) {
        locationService.getAll(orchestrator.getId()).forEach(location -> {
            Set<CSARDependency> dependencies = archiveIndexer.indexLocationArchives(orchestrator, location);
            location.getDependencies().addAll(dependencies);
            alienDAO.save(location);
        });
    }

    /**
     * Disable an orchestrator.
     *
     * @param orchestrator The orchestrator to disable.
     * @param force If true the orchestrator is disabled even if some deployments are currently running.
     */
    public List<Usage> disable(Orchestrator orchestrator, boolean force) {
        Lock lock = lockFor(orchestrator);

        try {
            lock.lock();

            if (!force) {
                // If there is at least one active deployment.
                GetMultipleDataResult<Deployment> result = alienDAO.buildQuery(Deployment.class)
                        .setFilters(MapUtil.newHashMap(new String[]{"orchestratorId", "endDate"},
                                new String[][]{new String[]{orchestrator.getId()}, new String[]{null}}))
                        .prepareSearch().setFieldSort("_timestamp", "long", true).search(0, 1);

                // TODO place a lock to avoid deployments during the disabling of the orchestrator.
                if (result.getData().length > 0) {
                    List<Usage> usages = generateDeploymentUsages(result.getData());
                    return usages;
                }
            }
            publisher.publishEvent(new BeforeOrchestratorDisabled(this, orchestrator));
            try {
                // unregister the orchestrator.
                IOrchestratorPlugin orchestratorInstance = orchestratorPluginService.unregister(orchestrator.getId());
                if (orchestratorInstance != null) {
                    IOrchestratorPluginFactory orchestratorFactory = orchestratorService.getPluginFactory(orchestrator);
                    orchestratorFactory.destroy(orchestratorInstance);
                }
            } catch (Exception e) {
                log.info("Unable to destroy orchestrator, it may not be created yet", e);
            } finally {
                // Mark the orchestrator as disabled
                orchestrator.setState(OrchestratorState.DISABLED);
                alienDAO.save(orchestrator);
            }
        } finally {
            lock.unlock();
        }

        return null;
    }

    private List<Usage> generateDeploymentUsages(Deployment[] data) {
        List<Usage> usages = Lists.newArrayList();
        for (Deployment deployment : data) {
            usages.add(new Usage(deployment.getSourceName(), deployment.getSourceType().getSourceType().getSimpleName(), deployment.getSourceId(), null));
        }
        return usages;
    }

    private Lock lockFor(Orchestrator orchestrator) {
        return mapLock.computeIfAbsent(orchestrator.getId(), k -> new ReentrantLock());
    }

}
