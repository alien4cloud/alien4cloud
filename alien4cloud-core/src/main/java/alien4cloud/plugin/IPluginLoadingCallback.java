package alien4cloud.plugin;

/**
 * Interface to implement to get events when plugins are loaded or unloaded.
 */
public interface IPluginLoadingCallback {
    void onPluginLoaded(ManagedPlugin managedPlugin);
    void onPluginClosed(ManagedPlugin managedPlugin);
}