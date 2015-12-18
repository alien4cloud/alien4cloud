package alien4cloud.orchestrators.services;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import javax.annotation.Resource;
import javax.inject.Inject;

import lombok.extern.slf4j.Slf4j;

import org.elasticsearch.mapping.QueryHelper;
import org.springframework.stereotype.Component;

import alien4cloud.component.repository.exception.CSARVersionAlreadyExistsException;
import alien4cloud.csar.services.CsarService;
import alien4cloud.dao.IGenericSearchDAO;
import alien4cloud.dao.model.GetMultipleDataResult;
import alien4cloud.deployment.DeploymentService;
import alien4cloud.exception.AlreadyExistException;
import alien4cloud.model.deployment.Deployment;
import alien4cloud.model.orchestrators.Orchestrator;
import alien4cloud.model.orchestrators.OrchestratorConfiguration;
import alien4cloud.model.orchestrators.OrchestratorState;
import alien4cloud.orchestrators.locations.services.LocationService;
import alien4cloud.orchestrators.plugin.ILocationAutoConfigurer;
import alien4cloud.orchestrators.plugin.IOrchestratorPlugin;
import alien4cloud.orchestrators.plugin.IOrchestratorPluginFactory;
import alien4cloud.orchestrators.plugin.model.PluginArchive;
import alien4cloud.paas.OrchestratorPluginService;
import alien4cloud.paas.exception.PluginConfigurationException;
import alien4cloud.tosca.ArchiveIndexer;
import alien4cloud.tosca.parser.ParsingError;
import alien4cloud.utils.MapUtil;

import com.google.common.collect.Lists;

/**
 * Service to manage state of an orchestrator
 */
@Slf4j
@Component
public class OrchestratorStateService {
    @Inject
    private QueryHelper queryHelper;
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
    private ArchiveIndexer archiveIndexer;

    /**
     * Initialize all orchestrator that have a non-disabled state.
     * Note: Each orchestrator initialization is down in it's own thread so it doesn't impact application startup or other orchestrator connection.
     *
     * @return a list of futures for those who want to wait for task to be done.
     */
    public List<Future<?>> initialize() {
        ExecutorService executorService = Executors.newCachedThreadPool();
        List<Future<?>> futures = new ArrayList<Future<?>>();
        // get all the orchestrators that are not disabled
        List<Orchestrator> enabledOrchestrators = orchestratorService.getAllEnabledOrchestrators();

        if (enabledOrchestrators == null) {
            return futures;
        }

        for (final Orchestrator orchestrator : enabledOrchestrators) {
            // error in initialization and timeouts should not impact startup time of Alien 4 cloud and other PaaS Providers.
            Future<?> future = executorService.submit(new Runnable() {
                @Override
                public void run() {
                    try {
                        load(orchestrator);
                    } catch (AlreadyExistException e) {
                        log.info("Orchestrator was already loaded at initialization for {}.", orchestrator.getId());
                    } catch (Throwable t) {
                        // we have to catch everything as we don't know what a plugin can do here and cannot interrupt startup.
                        // Any orchestrator that failed to load will be considered as DISABLED as the registration didn't occurred
                        log.error("Unexpected error in plugin", t);
                        orchestrator.setState(OrchestratorState.DISABLED);
                        alienDAO.save(orchestrator);
                    }
                }
            });
            futures.add(future);
        }
        return futures;
    }

    /**
     * Enable an orchestrator.
     *
     * @param orchestrator
     *            The orchestrator to enable.
     */
    public synchronized void enable(Orchestrator orchestrator) throws PluginConfigurationException {
        if (orchestrator.getState().equals(OrchestratorState.DISABLED)) {
            load(orchestrator);
        } else {
            log.debug("Request to enable ignored: orchestrator {} (id: {}) is already enabled", orchestrator.getName(), orchestrator.getId());
            throw new AlreadyExistException("Orchestrator {} is already instanciated.");
        }
    }

    /**
     * Load and connect the given orchestrator.
     *
     * @param orchestrator
     *            the orchestrator to load and connect.
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

        // TODO move below in a thread to perform plugin loading and connection asynchronously
        IOrchestratorPluginFactory orchestratorFactory = orchestratorService.getPluginFactory(orchestrator);
        IOrchestratorPlugin<Object> orchestratorInstance = orchestratorFactory.newInstance();
        // index the archive in alien catalog
        try {
            for (PluginArchive pluginArchive : orchestratorInstance.pluginArchives()) {
                archiveIndexer.importArchive(pluginArchive.getArchive(), pluginArchive.getArchiveFilePath(), Lists.<ParsingError> newArrayList());
            }
        } catch (CSARVersionAlreadyExistsException e) {
            log.info("Skipping location archive import as the released version already exists in the repository.");
        }
        // Set the configuration for the provider
        OrchestratorConfiguration orchestratorConfiguration = orchestratorConfigurationService.getConfigurationOrFail(orchestrator.getId());
        try {
            Object configuration = orchestratorConfigurationService.configurationAsValidObject(orchestrator.getId(),
                    orchestratorConfiguration.getConfiguration());
            orchestratorInstance.setConfiguration(configuration);
        } catch (IOException e) {
            throw new PluginConfigurationException("Failed convert configuration json in object.", e);
        }

        // connect the orchestrator
        orchestratorInstance.init(deploymentService.getCloudActiveDeploymentContexts(orchestrator.getId()));

        // register the orchestrator instance to be polled for updates
        orchestratorPluginService.register(orchestrator.getId(), orchestratorInstance);
        orchestrator.setState(OrchestratorState.CONNECTED);
        alienDAO.save(orchestrator);
        if (orchestratorInstance instanceof ILocationAutoConfigurer) {
            // trigger locations auto-configurations
            locationService.autoConfigure(orchestrator, (ILocationAutoConfigurer) orchestratorInstance);
        }
    }

    /**
     * Disable an orchestrator.
     *
     * @param orchestrator
     *            The orchestrator to disable.
     * @param force
     *            If true the orchestrator is disabled even if some deployments are currently running.
     */
    public synchronized boolean disable(Orchestrator orchestrator, boolean force) {
        if (!force) {
            QueryHelper.SearchQueryHelperBuilder searchQueryHelperBuilder = queryHelper.buildSearchQuery(alienDAO.getIndexForType(Deployment.class))
                    .types(Deployment.class).filters(MapUtil.newHashMap(new String[] { "orchestratorId", "endDate" },
                            new String[][] { new String[] { orchestrator.getId() }, new String[] { null } }))
                    .fieldSort("_timestamp", true);
            // If there is at least one active deployment.
            GetMultipleDataResult<Object> result = alienDAO.search(searchQueryHelperBuilder, 0, 1);

            // TODO place a lock to avoid deployments during disablement of the orchestrator.
            if (result.getData().length > 0) {
                return false;
            }
        }

        try {
            // un-register the orchestrator.
            IOrchestratorPlugin orchestratorInstance = (IOrchestratorPlugin) orchestratorPluginService.unregister(orchestrator.getId());
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

        return true;
    }
}