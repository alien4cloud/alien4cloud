package alien4cloud.plugin.model;

import java.nio.file.Path;
import java.util.Map;

import lombok.Getter;
import lombok.Setter;

import org.springframework.context.annotation.AnnotationConfigApplicationContext;

import alien4cloud.plugin.Plugin;

@Getter
public class ManagedPlugin {
    private final AnnotationConfigApplicationContext pluginContext;
    private final Plugin plugin;
    private final Path pluginPath;
    private final Path pluginUiPath;

    @Setter
    private Map<String, Object> exposedBeans;

    public ManagedPlugin(AnnotationConfigApplicationContext pluginContext, Plugin plugin, Path pluginPath, Path pluginUiPath) {
        this.pluginContext = pluginContext;
        this.plugin = plugin;
        this.pluginPath = pluginPath;
        this.pluginUiPath = pluginUiPath;
    }
}
