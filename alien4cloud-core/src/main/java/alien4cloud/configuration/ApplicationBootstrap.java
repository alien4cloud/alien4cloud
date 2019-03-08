package alien4cloud.configuration;

import javax.annotation.Resource;
import javax.inject.Inject;

import org.springframework.context.ApplicationListener;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.stereotype.Component;

import com.google.common.util.concurrent.ListenableFuture;

import alien4cloud.dao.IGenericSearchDAO;
import alien4cloud.dao.model.GetMultipleDataResult;
import alien4cloud.model.orchestrators.Orchestrator;
import alien4cloud.orchestrators.services.OrchestratorStateService;
import alien4cloud.plugin.Plugin;
import alien4cloud.plugin.PluginManager;
import alien4cloud.plugin.model.PluginConfiguration;
import alien4cloud.repository.services.RepositoryService;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
public class ApplicationBootstrap implements ApplicationListener<ContextRefreshedEvent> {
    @Resource(name = "alien-es-dao")
    private IGenericSearchDAO alienDAO;
    @Inject
    private PluginManager pluginManager;
    @Inject
    private OrchestratorStateService orchestratorStateService;
    @Inject
    private RepositoryService repositoryService;
    @Inject
    private InitialLoader initialLoader;

    private boolean initialized = false;

    /**
     * This operation initialize the JVM of Alien with configured plugins and context
     */
    public ListenableFuture<?> bootstrap() {
        // Starting from 1.3.1 version we don't allow multiple versions of a given plugin in a4c. The orchestrator plugin id does not requires version anymore
        // and should be cleaned.
        migration();

        // try to load plugins from init folder.
        initialLoader.loadPlugins();

        // Initialize existing enabled plugins.
        pluginManager.initialize();
        repositoryService.initialize();
        return orchestratorStateService.initialize();
    }

    /**
     * Performs data migration for 1.3.1 version where plugin ids are not generated using the version anymore.
     */
    private void migration() {
        log.debug("Initializing plugin id migrations");
        int count = 0;
        // This code updates the ids of plugin configurations and plugins in elasticsearch to remove the version reference.
        GetMultipleDataResult<Plugin> pluginResult = alienDAO.buildQuery(Plugin.class).prepareSearch().search(0, 10000);
        for (Plugin plugin : pluginResult.getData()) {
            if (plugin.getEsId().contains(":")) {
                PluginConfiguration pluginConfiguration = alienDAO.findById(PluginConfiguration.class, plugin.getEsId());
                if (pluginConfiguration != null) {
                    pluginConfiguration.setPluginId(plugin.getId());
                    alienDAO.save(pluginConfiguration);
                    alienDAO.delete(PluginConfiguration.class, plugin.getEsId());
                }
                alienDAO.save(plugin);
                alienDAO.delete(Plugin.class, plugin.getEsId());
                count++;
            }
        }
        if (count > 0) {
            log.info("{} plugins migrated", count);
        }
        count = 0;

        // This code updates the plugin id in the orchestrators.
        GetMultipleDataResult<Orchestrator> orchestratorResult = alienDAO.buildQuery(Orchestrator.class).prepareSearch().search(0, 10000);
        for (Orchestrator orchestrator : orchestratorResult.getData()) {
            if (orchestrator.getPluginId().contains(":")) {
                orchestrator.setPluginId(orchestrator.getPluginId().split(":")[0]);
                alienDAO.save(orchestrator);
                count++;
            }
        }
        if (count > 0) {
            log.info("Orchestrator migrated: {}.", count);
        }
        log.debug("plugin id migration done.");
    }

    /**
     * This operation unloads all plugin and orchestrator
     */
    public void teardown() {
        orchestratorStateService.unloadAllOrchestrators();
        repositoryService.unloadAllResolvers();
        pluginManager.unloadAllPlugins();
    }

    @Override
    public void onApplicationEvent(ContextRefreshedEvent event) {
        if (initialized) {
            return;
        }
        initialized = true;
        bootstrap();
    }
}
