package alien4cloud.plugin.model;

import java.nio.file.Path;

import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Primary;

import alien4cloud.plugin.Plugin;

/**
 * Plugin created by alien 4 cloud is the preferred one over a local instance that can be created for testing.
 */
@Primary
public class AlienManagedPlugin extends ManagedPlugin {
    /**
     * Create a new instance of an alien managed plugin.
     *
     * @param pluginContext The application context of the plugin.
     * @param plugin The plugin meta-data object.
     * @param pluginPath The path of the plugin content.
     * @param pluginUiPath The path the of ui content of the plugin.
     */
    public AlienManagedPlugin(AnnotationConfigApplicationContext pluginContext, Plugin plugin, Path pluginPath, Path pluginUiPath) {
        super(pluginContext, plugin, pluginPath, pluginUiPath);
    }
}
