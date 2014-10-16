package alien4cloud.configuration;

import java.io.IOException;

import javax.annotation.Resource;

import lombok.extern.slf4j.Slf4j;

import org.springframework.context.ApplicationListener;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.stereotype.Component;

import alien4cloud.cloud.CloudService;
import alien4cloud.plugin.PluginManager;

@Slf4j
@Component
public class ApplicationBootstrap implements ApplicationListener<ContextRefreshedEvent> {
    @Resource
    private PluginManager pluginManager;
    @Resource
    private CloudService cloudService;

    private boolean initialized = false;

    @Override
    public void onApplicationEvent(ContextRefreshedEvent event) {
        if (initialized) {
            return;
        }
        initialized = true;
        try {
            pluginManager.initialize();
        } catch (IOException e) {
            log.error("Error while loading plugins.", e);
        }
        cloudService.initialize();
    }
}