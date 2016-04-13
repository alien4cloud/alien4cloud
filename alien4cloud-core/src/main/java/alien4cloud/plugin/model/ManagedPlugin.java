package alien4cloud.plugin.model;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;

import lombok.Getter;
import lombok.Setter;

import org.springframework.context.annotation.AnnotationConfigApplicationContext;

import alien4cloud.plugin.Plugin;

@Getter
public class ManagedPlugin {
    private AnnotationConfigApplicationContext pluginContext;
    private Plugin plugin;
    private Path pluginPath;
    private Path pluginUiPath;

    @Setter
    private Map<String, Object> exposedBeans;

    // For testing purpose
    public ManagedPlugin(String pluginPath) {
        this.pluginPath = Paths.get(pluginPath);
    }

    public ManagedPlugin(AnnotationConfigApplicationContext pluginContext, Plugin plugin, Path pluginPath, Path pluginUiPath) {
        this.pluginContext = pluginContext;
        this.plugin = plugin;
        this.pluginPath = pluginPath;
        this.pluginUiPath = pluginUiPath;
    }
}
