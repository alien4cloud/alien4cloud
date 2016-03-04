package alien4cloud.configuration;

import java.io.IOException;

import javax.inject.Inject;

import alien4cloud.suggestions.services.SuggestionService;
import lombok.extern.slf4j.Slf4j;

import org.springframework.context.ApplicationListener;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.stereotype.Component;

import alien4cloud.orchestrators.services.OrchestratorStateService;
import alien4cloud.plugin.PluginManager;

import com.google.common.util.concurrent.ListenableFuture;

@Slf4j
@Component
public class ApplicationBootstrap implements ApplicationListener<ContextRefreshedEvent> {
    @Inject
    private PluginManager pluginManager;
    @Inject
    private OrchestratorStateService orchestratorStateService;
    @Inject
    private InitialLoader initialLoader;
    @Inject
    private SuggestionService suggestionService;

    private boolean initialized = false;

    /**
     * This operation initialize the JVM of Alien with configured plugins and context
     */
    public ListenableFuture<?> bootstrap() {
        try {
            // initialize existing plugins
            pluginManager.initialize();

            // try to load plugins from init folder.
            initialLoader.loadPlugins();

            // try to init the suggestion indexes
            suggestionService.loadDefaultSuggestions();
        } catch (IOException e) {
            log.error("Error while loading plugins.", e);
        }
        return orchestratorStateService.initialize();
    }

    /**
     * This operation unloads all plugin and orchestrator
     */
    public void teardown() {
        orchestratorStateService.unloadAllOrchestrators();
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