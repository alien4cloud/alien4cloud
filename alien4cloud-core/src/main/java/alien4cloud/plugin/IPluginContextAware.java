package alien4cloud.plugin;

import alien4cloud.plugin.model.ManagedPlugin;

/**
 * Plugin beans that requires context should implements this class.
 */
public interface IPluginContextAware {
    /**
     * On plugin loading this method is called to provide the plugin with the managed plugin context (including local path etc.)
     * 
     * @param selfContext Context of the plugin the bean that implements this interface belongs to.
     */
    void setContext(ManagedPlugin selfContext);
}