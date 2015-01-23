package alien4cloud.documentation;

import java.lang.reflect.Field;
import java.util.List;
import java.util.Map;

import javax.annotation.Resource;

import lombok.extern.slf4j.Slf4j;

import org.springframework.boot.context.embedded.EmbeddedServletContainerInitializedEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.DispatcherServlet;
import org.springframework.web.servlet.HandlerMapping;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;

import alien4cloud.exception.InitializationException;
import alien4cloud.plugin.IPluginLoadingCallback;
import alien4cloud.plugin.ManagedPlugin;
import alien4cloud.plugin.PluginManager;

import com.google.common.collect.Maps;

/**
 * Map rest services to the rest dispatcher.
 */
@Slf4j
@Component
public class PluginRestMapper implements IPluginLoadingCallback, ApplicationListener<EmbeddedServletContainerInitializedEvent> {
    private boolean dispatchedReady = false;
    private List<HandlerMapping> handlerMappings;
    @Resource
    private DispatcherServlet dispatcher;
    @Resource
    private PluginManager pluginManager;
    private Map<String, RequestMappingHandlerMapping> pluginMappings = Maps.newHashMap();

    @Override
    public synchronized void onPluginLoaded(ManagedPlugin managedPlugin) {
        if (dispatchedReady) {
            mapContext(managedPlugin);
        }
    }

    @Override
    public synchronized void onPluginClosed(ManagedPlugin managedPlugin) {
        RequestMappingHandlerMapping mapping = this.pluginMappings.remove(managedPlugin.getDescriptor().getId());
        for (int i = 0; i < this.handlerMappings.size(); i++) {
            // identity check and remove if this is the mapping associated with the plugin to close.
            if (this.handlerMappings.get(i) == mapping) {
                this.handlerMappings.remove(i);
                return;
            }
        }
    }

    @Override
    public synchronized void onApplicationEvent(EmbeddedServletContainerInitializedEvent event) {
        try {
            Field field = DispatcherServlet.class.getDeclaredField("handlerMappings");
            field.setAccessible(true);
            this.handlerMappings = (List<HandlerMapping>) field.get(dispatcher);
            this.dispatchedReady = true;
        } catch (NoSuchFieldException | IllegalAccessException e) {
            throw new InitializationException("Unable to initialize plugins rest mapping.", e);
        }

        log.info("Initialize plugin rest mappings");
        for (ManagedPlugin managedPlugin : pluginManager.getPluginContexts().values()) {
            mapContext(managedPlugin);
        }
        log.info("Plugin rest mappings initialized.");
    }

    private void mapContext(ManagedPlugin managedPlugin) {
        // dispatcher context is ready after the plugin mapper actually.
        RequestMappingHandlerMapping mapper = new RequestMappingHandlerMapping();
        mapper.setApplicationContext(managedPlugin.getPluginContext());
        mapper.afterPropertiesSet();
        this.handlerMappings.add(this.handlerMappings.size()-2, mapper);
        this.pluginMappings.put(managedPlugin.getDescriptor().getId(), mapper);
    }
}