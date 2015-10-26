package alien4cloud.configuration;

import java.io.IOException;

import javax.inject.Inject;

import lombok.extern.slf4j.Slf4j;

import org.springframework.context.ApplicationListener;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.stereotype.Component;

import alien4cloud.orchestrators.services.OrchestratorStateService;
import alien4cloud.plugin.PluginManager;

@Slf4j
@Component
public class ApplicationBootstrap implements ApplicationListener<ContextRefreshedEvent> {
    @Inject
    private PluginManager pluginManager;
    @Inject
    private OrchestratorStateService orchestratorStateService;
    @Inject
    private InitialLoader initialLoader;

    private boolean initialized = false;

    @Override
    public void onApplicationEvent(ContextRefreshedEvent event) {
        if (initialized) {
            return;
        }
        initialized = true;
        try {
            // initialize existing plugins
            pluginManager.initialize();

            // try to load plugins from init folder.
            initialLoader.loadPlugins();
        } catch (IOException e) {
            log.error("Error while loading plugins.", e);
        }
        orchestratorStateService.initialize();
    }
}