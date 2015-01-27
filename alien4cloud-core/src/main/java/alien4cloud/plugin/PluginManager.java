package alien4cloud.plugin;

import java.io.IOException;
import java.net.URL;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.UUID;

import javax.annotation.Resource;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import org.apache.commons.collections4.MapUtils;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.mapping.MappingBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.stereotype.Component;

import alien4cloud.dao.IGenericSearchDAO;
import alien4cloud.dao.model.GetMultipleDataResult;
import alien4cloud.exception.AlreadyExistException;
import alien4cloud.exception.NotFoundException;
import alien4cloud.utils.FileUtil;
import alien4cloud.utils.MapUtil;
import alien4cloud.utils.ReflectionUtil;
import alien4cloud.utils.YamlParserUtil;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

/**
 * Manages plugins.
 */
@Slf4j
@Component("plugin-manager")
@SuppressWarnings({ "rawtypes", "unchecked" })
public class PluginManager {
    private static final String LIB_DIRECTORY = "lib";
    private static final String PLUGIN_DESCRIPTOR_FILE = "META-INF/plugin.yml";

    @Value("${directories.alien}/${directories.plugins}")
    private String pluginDirectory;

    @Resource(name = "alien-es-dao")
    private IGenericSearchDAO alienDAO;
    @Resource
    private ApplicationContext alienContext;
    private Map<String, ManagedPlugin> pluginContexts = Maps.newHashMap();
    @Getter
    private List<PluginLinker> linkers = null;

    /**
     * Initialize the plugins for alien.
     * 
     * @throws IOException In case we fail to iterate over the plugin directory.
     */
    public void initialize() throws IOException {
        // Initialize alien's plugin linkers.
        if (linkers == null) {
            linkers = Lists.newArrayList();
            Map<String, IPluginLinker> pluginLinkers = alienContext.getBeansOfType(IPluginLinker.class);
            for (IPluginLinker linker : pluginLinkers.values()) {
                linkers.add(new PluginLinker(linker, getLinkedType(linker)));
            }
        }

        // Ensure plugin directory exists.
        Path path = FileSystems.getDefault().getPath(pluginDirectory);
        if (!Files.exists(path)) {
            Files.createDirectories(path);
            log.info("Plugin work directory created at <" + path.toAbsolutePath().toString() + ">");
        }

        // Load enabled plugins in alien
        int from = 0;
        long totalResult = 0;
        do {
            GetMultipleDataResult<Plugin> results = alienDAO.find(Plugin.class, MapUtil.newHashMap(new String[] { "enabled" }, new String[][] { { "true" } }),
                    100);
            for (Plugin plugin : results.getData()) {
                try {
                    loadPlugin(plugin);
                } catch (PluginLoadingException e) {
                    log.error("Alien server Initialization: failed to load plugin <" + plugin.getId() + ">");
                }
            }
            from += results.getData().length;
            totalResult = results.getTotalResults();
        } while (from < totalResult);

    }

    private Class<?> getLinkedType(IPluginLinker<?> linker) {
        return ReflectionUtil.getGenericArgumentType(linker.getClass(), IPluginLinker.class, 0);
    }

    /**
     * Upload a plugin from a given path.
     * 
     * @param uploadedPluginPath The path of the plugin to upload.
     * @throws IOException In case there is an issue with the access to the plugin file.
     * @throws PluginLoadingException
     * @throws AlreadyExistException if a plugin with the same id already exists in the repository
     * @return the uploaded plugin
     */
    public Plugin uploadPlugin(Path uploadedPluginPath) throws IOException, PluginLoadingException {
        // load the plugin descriptor
        FileSystem fs = FileSystems.newFileSystem(uploadedPluginPath, null);
        try {
            PluginDescriptor descriptor = YamlParserUtil.parseFromUTF8File(fs.getPath(PLUGIN_DESCRIPTOR_FILE), PluginDescriptor.class);

            String pluginPathId = getPluginPathId();
            Plugin plugin = new Plugin(descriptor, pluginPathId);

            Path pluginPath = getPluginPath(pluginPathId);
            FileUtil.unzip(uploadedPluginPath, pluginPath);

            // check plugin already exists
            long count = alienDAO.count(Plugin.class, QueryBuilders.idsQuery(MappingBuilder.indexTypeFromClass(Plugin.class)).ids(plugin.getId()));
            if (count > 0) {
                log.warn("Uploading Plugin <{}> impossible (already exists)", plugin.getId());
                throw new AlreadyExistException("A plugin with the given id and version already exists.");
            }
            loadPlugin(plugin);
            plugin.setConfigurable(isPluginConfigurable(plugin.getId()));
            alienDAO.save(plugin);
            log.info("Plugin <" + plugin.getId() + "> has been enabled.");
            return plugin;
        } finally {
            fs.close();
        }
    }

    /**
     * Disable a plugin.
     * 
     * @param pluginId The id of the plugin to disable.
     * @param remove If true the plugin is not only disabled but also removed from the plugin repository.
     * @return Empty list if the plugin was successfully disabled (and removed), or a list of usages that prevent the plugin to be disabled/removed.
     */
    public List<PluginUsage> disablePlugin(String pluginId, boolean remove) {
        List<PluginUsage> usages = Lists.newArrayList();
        ManagedPlugin managedPlugin = pluginContexts.get(pluginId);
        if (managedPlugin != null) {
            for (PluginLinker linker : linkers) {
                usages.addAll(linker.linker.usage(pluginId));
            }
        }

        if (usages.isEmpty()) {
            Path pluginPath;
            if (managedPlugin != null) {
                // send events to plugin loading callbacks
                Map<String, IPluginLoadingCallback> beans = alienContext.getBeansOfType(IPluginLoadingCallback.class);
                for (IPluginLoadingCallback callback : beans.values()) {
                    callback.onPluginClosed(managedPlugin);
                }

                // destroy the plugin context
                managedPlugin.getPluginContext().destroy();
                pluginPath = managedPlugin.getPluginPath();
            } else {
                Plugin plugin = alienDAO.findById(Plugin.class, pluginId);
                pluginPath = getPluginPath(plugin.getPluginPathId());
            }

            // unlink the plugin
            for (PluginLinker linker : linkers) {
                linker.linker.unlink(pluginId);
            }

            // eventually remove it from elastic search and disk.
            if (remove) {
                alienDAO.delete(Plugin.class, pluginId);
                // remove also the configuration
                alienDAO.delete(PluginConfiguration.class, pluginId);
                // try to delete the plugin dir in the repo
                try {
                    FileUtil.delete(pluginPath);
                    FileUtil.delete(getPluginZipFilePath(pluginId));
                } catch (IOException e) {
                    log.error("Failed to delete the plugin <" + pluginId + "> in the repository. You'll have to do it manually", e);
                }
            } else {
                disablePlugin(pluginId);
            }
            pluginContexts.remove(pluginId);
        }
        return usages;
    }

    private void disablePlugin(String pluginId) {
        Plugin plugin = alienDAO.findById(Plugin.class, pluginId);
        plugin.setEnabled(false);
        alienDAO.save(plugin);
    }

    /**
     * Enable a plugin in alien.
     * 
     * @param pluginId The id of the plugin to load.
     * @throws PluginLoadingException In case plugin loading fails.
     */
    public void enablePlugin(String pluginId) throws PluginLoadingException {
        if (this.pluginContexts.get(pluginId) != null) {
            log.info("Plugin <" + pluginId + "> is already loaded.");
            return;
        }

        log.info("Loading plugin <" + pluginId + ">");

        // save the plugin's file in the work directory
        Plugin plugin = alienDAO.findById(Plugin.class, pluginId);
        if (plugin == null) {
            throw new NotFoundException("The plugin <" + pluginId + "> doesn't exists in alien.");
        }
        loadPlugin(plugin);
        plugin.setEnabled(true);
        alienDAO.save(plugin);
        log.info("Plugin <" + pluginId + "> has been enabled.");
    }

    private void loadPlugin(Plugin plugin) throws PluginLoadingException {
        try {
            Path pluginPath = getPluginPath(plugin.getPluginPathId());
            loadPlugin(plugin, pluginPath);
        } catch (Throwable e) {
            log.error("Failed to load plugin <" + plugin.getId() + "> alien will ignore this plugin.", e);
            throw new PluginLoadingException("Failed to load plugin <" + plugin.getId() + ">", e);
        }
    }

    private String getPluginPathId() {
        // JVM has a bug that makes a classloader keep a lock on loaded files while the VM is running.
        // we create a unique random folder for plugins so the lock cannot prevent to delete and re-upload the same plugin.
        String pluginId = UUID.randomUUID().toString();
        Path pluginPath = getPluginPath(pluginId);
        while (Files.exists(pluginPath)) {
            pluginId = UUID.randomUUID().toString();
            pluginPath = getPluginPath(pluginId);
        }
        return pluginId;
    }

    private Path getPluginPath(String pluginPathId) {
        return FileSystems.getDefault().getPath(pluginDirectory, pluginPathId);
    }

    private Path getPluginZipFilePath(String pluginId) {
        String pluginFileName = pluginId.replaceAll(":", "-");
        return FileSystems.getDefault().getPath(pluginDirectory, pluginFileName + ".cpa");
    }

    /**
     * Actually load and link a plugin in Alien 4 Cloud.
     *
     * @param plugin The plugin the load and link.
     * @param pluginPath the path to the directory that contains the un-zipped plugin.
     * @throws IOException In case there is an IO issue with the file.
     * @throws ClassNotFoundException If we cannot load the class
     */
    private void loadPlugin(Plugin plugin, Path pluginPath) throws IOException, ClassNotFoundException {
        // create a class loader to manage this plugin.
        final List<URL> classPathUrls = Lists.newArrayList();
        classPathUrls.add(pluginPath.toUri().toURL());
        Path libPath = pluginPath.resolve(LIB_DIRECTORY);
        if (Files.exists(libPath)) {
            Files.walkFileTree(libPath, new SimpleFileVisitor<Path>() {
                @Override
                public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                    file.endsWith(".jar");
                    classPathUrls.add(file.toUri().toURL());
                    return FileVisitResult.CONTINUE;
                }
            });
        }
        ClassLoader pluginClassLoader = new PluginClassloader(classPathUrls.toArray(new URL[classPathUrls.size()]), Thread.currentThread()
                .getContextClassLoader());

        // load a spring context for the plugin that will be a child of the current spring context
        AnnotationConfigApplicationContext pluginContext = new AnnotationConfigApplicationContext();
        pluginContext.setParent(alienContext);
        pluginContext.setClassLoader(pluginClassLoader);
        pluginContext.register(pluginClassLoader.loadClass(plugin.getDescriptor().getConfigurationClass()));
        pluginContext.refresh();

        ManagedPlugin managedPlugin = new ManagedPlugin(pluginContext, plugin.getDescriptor(), pluginPath);

        // send events to plugin loading callbacks
        Map<String, IPluginLoadingCallback> beans = alienContext.getBeansOfType(IPluginLoadingCallback.class);
        for (IPluginLoadingCallback callback : beans.values()) {
            callback.onPluginLoaded(managedPlugin);
        }

        Map<String, PluginComponentDescriptor> componentDescriptors = getPluginComponentDescriptorAsMap(plugin);

        // register plugin elements in Alien
        for (PluginLinker linker : linkers) {
            Map<String, ?> instancesToLink = pluginContext.getBeansOfType(linker.linkedType);
            for (Entry<String, ?> instanceToLink : instancesToLink.entrySet()) {
                linker.linker.link(plugin.getId(), instanceToLink.getKey(), instanceToLink.getValue());
                PluginComponentDescriptor componentDescriptor = componentDescriptors.get(instanceToLink.getKey());
                if (componentDescriptor == null) {
                    componentDescriptor = new PluginComponentDescriptor();
                    componentDescriptor.setBeanName(instanceToLink.getKey());
                    componentDescriptor.setName(instanceToLink.getKey());
                }
                componentDescriptor.setType(linker.linkedType.getSimpleName());
            }
        }

        // TODO get configurable elements from the plugin and get configuration from elastic-search

        // install static resources to be available for the application.
        pluginContexts.put(plugin.getId(), managedPlugin);
    }

    private Map<String, PluginComponentDescriptor> getPluginComponentDescriptorAsMap(Plugin plugin) {
        Map<String, PluginComponentDescriptor> componentDescriptors = Maps.newHashMap();
        if (plugin == null || plugin.getDescriptor() == null || plugin.getDescriptor().getComponentDescriptors() == null) {
            return componentDescriptors;
        }
        for (PluginComponentDescriptor componentDescriptor : plugin.getDescriptor().getComponentDescriptors()) {
            componentDescriptors.put(componentDescriptor.getBeanName(), componentDescriptor);
        }
        return componentDescriptors;
    }

    /**
     * The the plugin descriptor for a given plugin.
     * 
     * @param pluginId The id of the plugin for which to get the descriptor.
     * @return The plugin descriptor for the given plugin.
     */
    public PluginDescriptor getPluginDescriptor(String pluginId) {
        return pluginContexts.get(pluginId).getDescriptor();
    }

    private Class<?> getConfigurationType(IPluginConfigurator<?> configurator) {
        return ReflectionUtil.getGenericArgumentType(configurator.getClass(), IPluginConfigurator.class, 0);
    }

    /**
     * Return true if the plugin can be configured using a configuration object (basically if the plugin spring context contains an instance of
     * {@link IPluginConfigurator}.
     * 
     * @param pluginId Id of the plugin for which to know if configurable.
     * @return True if the plugin can be configured, false if not.
     */
    public boolean isPluginConfigurable(String pluginId) {
        AnnotationConfigApplicationContext pluginContext = pluginContexts.get(pluginId).getPluginContext();
        Map<String, IPluginConfigurator> configurators = pluginContext.getBeansOfType(IPluginConfigurator.class);
        return MapUtils.isNotEmpty(configurators);
    }

    /**
     * Get the class of the configuration object for a given plugin.
     * 
     * @param pluginId Id of the plugin for which to get configuration object's class.
     * @return The class of the plugin configuration object.
     */
    public Class<?> getConfigurationType(String pluginId) {
        AnnotationConfigApplicationContext pluginContext = pluginContexts.get(pluginId).getPluginContext();
        Map<String, IPluginConfigurator> configurators = pluginContext.getBeansOfType(IPluginConfigurator.class);
        if (MapUtils.isNotEmpty(configurators)) {
            // TODO: manage case multiple configuration beans
            return getConfigurationType(configurators.values().iterator().next());
        }
        return null;
    }

    /**
     * Get the instance of the {@link IPluginConfigurator} for a given plugin.
     * 
     * @param pluginId Id of the plugin for which to get a the {@link IPluginConfigurator}
     * @return Null if no {@link IPluginConfigurator} is defined within the plugin's spring context or the first instance of {@link IPluginConfigurator}.
     */
    public <T> IPluginConfigurator<T> getConfiguratorFor(String pluginId) {
        AnnotationConfigApplicationContext pluginContext = pluginContexts.get(pluginId).getPluginContext();
        Map<String, IPluginConfigurator> configurators = pluginContext.getBeansOfType(IPluginConfigurator.class);
        if (MapUtils.isNotEmpty(configurators)) {
            // TODO: manage case multiple configuration beans
            return configurators.values().iterator().next();
        }
        return null;
    }

    public Map<String, ManagedPlugin> getPluginContexts() {
        return pluginContexts;
    }

    @AllArgsConstructor
    private final class PluginLinker<T> {
        private IPluginLinker<T> linker;
        private Class<T> linkedType;
    }
}
