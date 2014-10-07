package alien4cloud.plugin;

import lombok.Getter;
import lombok.Setter;

import org.springframework.stereotype.Service;

/**
 * Describe a component of a plugin (can be an IPaaSProvider etc.)
 */
@Setter
@Getter
@Service
@SuppressWarnings("PMD.UnusedPrivateField")
public class PluginComponentDescriptor {
    /** Name of the component bean in the plugin spring context. */
    private String beanName;
    /** Name of the plugin component. */
    private String name;
    /** Description of the plugin. */
    private String description;
    /** Type of the plugin (injected by ALIEN plugin loader) */
    private String type;
}