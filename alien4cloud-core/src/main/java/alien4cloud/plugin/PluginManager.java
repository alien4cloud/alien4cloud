package alien4cloud.plugin;

import java.io.IOException;
import java.net.URL;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.*;
import java.util.Map.Entry;

import javax.annotation.Resource;

import org.apache.commons.collections4.MapUtils;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.mapping.MappingBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.config.ConstructorArgumentValues;
import org.springframework.beans.factory.support.GenericBeanDefinition;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.stereotype.Component;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;

import alien4cloud.dao.IGenericSearchDAO;
import alien4cloud.dao.model.GetMultipleDataResult;
import alien4cloud.exception.AlreadyExistException;
import alien4cloud.exception.NotFoundException;
import alien4cloud.plugin.exception.MissingPlugingDescriptorFileException;
import alien4cloud.plugin.exception.PluginLoadingException;
import alien4cloud.plugin.model.*;
import alien4cloud.utils.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

/**
 * Manages plugins.
 */
@Slf4j
@Component("plugin-manager")
@SuppressWarnings({ "rawtypes", "unchecked" })
public class PluginManager {
    private static final String UNKNOWN_PLUGIN_COMPONENT_TYPE = "Unknown component type";
    private static final String LIB_DIRECTORY = "lib";
    private static final String UI_DIRECTORY = "ui";
    private static final String PLUGIN_DESCRIPTOR_FILE = "META-INF/plugin.yml";

    @Value("${directories.alien}/plugins")
    private String pluginsDirectory; // directory in which plugins are placed so they are loaded when alien is starting - for initialization.
    @Value("${directories.alien}/work/plugins/content")
    private String pluginsWorkDirectory; // directory in which alien place plugins that are loaded.
    @Value("${directories.alien}/work/plugins/ui")
    private String pluginsUiDirectory; // directory in which alien place ui files from plugins so they are available from clients.

    @Resource(name = "alien-es-dao")
    private IGenericSearchDAO alienDAO;
    @Resource
    private ApplicationContext alienContext;
    private Map<String, ManagedPlugin> pluginContexts = Maps.newHashMap();
    @Getter
    private List<PluginLinker> linkers = null;

    public void unloadAllPlugins() {
        log.info("Unloading plugins");
        GetMultipleDataResult<Plugin> results = alienDAO.find(Plugin.class, MapUtil.newHashMap(new String[] { "enabled" }, new String[][] { { "true" } }),
                Integer.MAX_VALUE);
        for (Plugin plugin : results.getData()) {
            unloadPlugin(plugin.getId(), false, false);
        }
        log.info("{} Plugins unloaded", results.getData().length);
    }

    /**
     * Initialize the plugins for alien.
     *
     * @throws IOException In case we fail to iterate over the plugin directory.
     */
    public void initialize() throws IOException {
        // Initialize alien's plugin linkers.
        if (linkers == null) {
            linkers = Lists.newArrayList();
            for (IPluginLinker linker : SpringUtils.getBeansOfType(alienContext, IPluginLinker.class)) {
                linkers.add(new PluginLinker(linker, getLinkedType(linker)));
            }
        }

        // Ensure plugin directory exists.
        Path path = FileSystems.getDefault().getPath(pluginsWorkDirectory);
        if (!Files.exists(path)) {
            Files.createDirectories(path);
            log.info("Plugin work directory created at <" + path.toAbsolutePath().toString() + ">");
        }

        log.info("Initializing plugins");
        // Load enabled plugins in alien, query using max value as anyway we must be able to load all plugins in memory.
        GetMultipleDataResult<Plugin> results = alienDAO.find(Plugin.class, MapUtil.newHashMap(new String[] { "enabled" }, new String[][] { { "true" } }),
                Integer.MAX_VALUE);
        loadPlugins(results.getData());
        log.info("{} Plugins initialized.", results.getData().length);
    }

    /**
     * Load all the plugins that have their dependencies fullfilled by loaded plugin.
     *
     * @param plugins the plugins to load.
     */
    private void loadPlugins(Plugin[] plugins) {
        List<Plugin> missingDependencyPlugins = Lists.newArrayList();
        for (Plugin plugin : plugins) {
            // if the plugin has no unresolved dependency, load it
            if (getMissingDependencies(plugin).size() == 0) {
                try {
                    loadPlugin(plugin);
                } catch (PluginLoadingException e) {
                    log.error("Alien server Initialization: failed to load plugin <" + plugin.getId() + ">");
                    disablePlugin(plugin.getId());
                }
            } else {
                missingDependencyPlugins.add(plugin);
            }
        }
        if (missingDependencyPlugins.size() == plugins.length) {
            // No plugins have been loaded meaning that remaining plugins are not loadable because some dependencies are missing
            for (Plugin plugin : plugins) {
                log.error("Failed to load plugin <" + plugin.getId() + "> as some dependencies are missing <" + getMissingDependencies(plugin) + ">");
                disablePlugin(plugin.getId());
            }
        } else {
            if (missingDependencyPlugins.size() > 0) {
                loadPlugins(missingDependencyPlugins.toArray(new Plugin[missingDependencyPlugins.size()]));
            }
        }
    }

    private Set<String> getMissingDependencies(Plugin plugin) {
        Set<String> missingDependencies = Sets.newHashSet();
        String[] dependencies = plugin.getDescriptor().getDependencies();
        if (dependencies != null && dependencies.length > 0) {
            for (String dependency : dependencies) {
                if (this.pluginContexts.get(dependency) == null) {
                    missingDependencies.add(dependency);
                }
            }
        }
        return missingDependencies;
    }

    private Class<?> getLinkedType(IPluginLinker<?> linker) {
        return ReflectionUtil.getGenericArgumentType(linker.getClass(), IPluginLinker.class, 0);
    }

    /**
     * Upload a plugin from a given path.
     *
     * @param uploadedPluginPath The path of the plugin to upload.
     * @return the uploaded plugin
     * @throws IOException In case there is an issue with the access to the plugin file.
     * @throws PluginLoadingException
     * @throws AlreadyExistException if a plugin with the same id already exists in the repository
     * @throws MissingPlugingDescriptorFileException
     */
    public Plugin uploadPlugin(Path uploadedPluginPath) throws PluginLoadingException, IOException, MissingPlugingDescriptorFileException {
        // load the plugin descriptor
        FileSystem fs = FileSystems.newFileSystem(uploadedPluginPath, null);
        PluginDescriptor descriptor = null;
        try {
            try {
                descriptor = YamlParserUtil.parseFromUTF8File(fs.getPath(PLUGIN_DESCRIPTOR_FILE), PluginDescriptor.class);
            } catch (IOException e) {
                if (e instanceof NoSuchFileException) {
                    throw new MissingPlugingDescriptorFileException();
                } else {
                    throw e;
                }
            }

            String pluginPathId = getPluginPathId();
            Plugin plugin = new Plugin(descriptor, pluginPathId);

            // check plugin already exists
            long count = alienDAO.count(Plugin.class, QueryBuilders.idsQuery(MappingBuilder.indexTypeFromClass(Plugin.class)).ids(plugin.getId()));
            if (count > 0) {
                log.warn("Uploading Plugin <{}> impossible (already exists)", plugin.getId());
                throw new AlreadyExistException("A plugin with the given id and version already exists.");
            }

            Path pluginPath = getPluginPath(pluginPathId);
            FileUtil.unzip(uploadedPluginPath, pluginPath);

            // copy ui directory in case it exists
            Path pluginUiSourcePath = pluginPath.resolve(UI_DIRECTORY);
            Path pluginUiPath = getPluginUiPath(pluginPathId);
            if (Files.exists(pluginUiSourcePath)) {
                FileUtil.copy(pluginUiSourcePath, pluginUiPath);
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

    private void unloadPlugin(String pluginId, boolean disable, boolean remove) {
        ManagedPlugin managedPlugin = pluginContexts.get(pluginId);
        Path pluginPath;
        Path pluginUiPath;
        if (managedPlugin != null) {
            // send events to plugin loading callbacks
            for (IPluginLoadingCallback callback : SpringUtils.getBeansOfType(alienContext, IPluginLoadingCallback.class)) {
                callback.onPluginClosed(managedPlugin);
            }

            managedPlugin.getPluginContext().stop();
            // destroy the plugin context
            managedPlugin.getPluginContext().destroy();
            pluginPath = managedPlugin.getPluginPath();
            pluginUiPath = managedPlugin.getPluginUiPath();
        } else {
            Plugin plugin = alienDAO.findById(Plugin.class, pluginId);
            pluginPath = getPluginPath(plugin.getPluginPathId());
            pluginUiPath = getPluginUiPath(plugin.getPluginPathId());
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
                FileUtil.delete(pluginUiPath);
            } catch (IOException e) {
                log.error("Failed to delete the plugin <" + pluginId + "> in the repository. You'll have to do it manually", e);
            }
        } else if (disable) {
            disablePlugin(pluginId);
        }
        pluginContexts.remove(pluginId);
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
            unloadPlugin(pluginId, true, remove);
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
            Path pluginUiPath = getPluginUiPath(plugin.getPluginPathId());
            loadPlugin(plugin, pluginPath, pluginUiPath);
        } catch (Exception e) {
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
        return FileSystems.getDefault().getPath(pluginsWorkDirectory, pluginPathId);
    }

    private Path getPluginUiPath(String pluginPathId) {
        return FileSystems.getDefault().getPath(pluginsUiDirectory, pluginPathId);
    }

    private Path getPluginZipFilePath(String pluginId) {
        String pluginFileName = pluginId.replaceAll(":", "-");
        return FileSystems.getDefault().getPath(pluginsWorkDirectory, pluginFileName + ".cpa");
    }

    /**
     * Actually load and link a plugin in Alien 4 Cloud.
     *
     * @param plugin The plugin the load and link.
     * @param pluginPath The path to the directory that contains the un-zipped plugin.
     * @param pluginUiPath The path in which the ui files are located.
     * @throws IOException In case there is an IO issue with the file.
     * @throws ClassNotFoundException If we cannot load the class
     */
    private void loadPlugin(Plugin plugin, Path pluginPath, Path pluginUiPath) throws IOException, ClassNotFoundException {
        // create a class loader to manage this plugin.
        final List<URL> classPathUrls = Lists.newArrayList();
        pluginPath = pluginPath.toRealPath();
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
        ClassLoader pluginClassLoader = new PluginClassloader(classPathUrls.toArray(new URL[classPathUrls.size()]),
                Thread.currentThread().getContextClassLoader());

        // load a spring context for the plugin that will be a child of the current spring context
        AnnotationConfigApplicationContext pluginContext = new AnnotationConfigApplicationContext();
        pluginContext.setParent(alienContext);
        pluginContext.setClassLoader(pluginClassLoader);

        // Register beans from dependencies
        // FIXME and if the plugin doesn't have any config class? for ex a typical ui plugin?
        registerDependencies(plugin, pluginContext);
        if (plugin.getDescriptor().getConfigurationClass() != null) {
            pluginContext.register(pluginClassLoader.loadClass(plugin.getDescriptor().getConfigurationClass()));
        }

        // Register the context so that it can be injected by other beans
        // pluginContext.getBeanFactory().registerSingleton("alien-plugin-context", managedPlugin);
        GenericBeanDefinition beanDefinition = new GenericBeanDefinition();
        beanDefinition.setPrimary(true);
        beanDefinition.setBeanClass(ManagedPlugin.class);
        ConstructorArgumentValues constructorArgumentValues = new ConstructorArgumentValues();
        constructorArgumentValues.addIndexedArgumentValue(0, pluginContext);
        constructorArgumentValues.addIndexedArgumentValue(1, plugin);
        constructorArgumentValues.addIndexedArgumentValue(2, pluginPath);
        constructorArgumentValues.addIndexedArgumentValue(3, pluginUiPath);
        beanDefinition.setConstructorArgumentValues(constructorArgumentValues);
        pluginContext.registerBeanDefinition("alien-plugin-context", beanDefinition);

        pluginContext.refresh();
        pluginContext.start();
        ManagedPlugin managedPlugin = (ManagedPlugin) pluginContext.getBean("alien-plugin-context");

        Map<String, PluginComponentDescriptor> componentDescriptors = getPluginComponentDescriptorAsMap(plugin);

        // expose plugin elements so they are available to plugins that depends from them.
        expose(managedPlugin, componentDescriptors);
        // register plugin elements in Alien
        link(plugin, managedPlugin, componentDescriptors);

        // install static resources to be available for the application.
        pluginContexts.put(plugin.getId(), managedPlugin);
    }

    private void registerDependencies(Plugin plugin, AnnotationConfigApplicationContext pluginContext) {
        if (plugin.getDescriptor().getDependencies() == null) {
            return; // no dependencies for this plugin.
        }

        for (String dependency : plugin.getDescriptor().getDependencies()) {
            ManagedPlugin dependencyPlugin = this.pluginContexts.get(dependency);
            for (Entry<String, Object> exposed : dependencyPlugin.getExposedBeans().entrySet()) {
                pluginContext.getBeanFactory().registerSingleton(exposed.getKey(), exposed.getValue());
            }
        }
    }

    /**
     * Initialize the list of exposed beans for the given plugin.
     *
     * @param managedPlugin The plugin for which to configure exposed beans.
     * @param componentDescriptors The components descriptor of the plugin.
     */
    private void expose(ManagedPlugin managedPlugin, Map<String, PluginComponentDescriptor> componentDescriptors) {
        Map<String, Object> exposedBeans = Maps.newHashMap();
        for (Entry<String, PluginComponentDescriptor> componentDescriptorEntry : componentDescriptors.entrySet()) {
            String beanName = componentDescriptorEntry.getValue().getBeanName();
            // TODO handle NoSuchBeanDefinitionException with a nice error message to the user.
            Object bean = managedPlugin.getPluginContext().getBean(beanName);
            if (bean == null) {
                log.warn("Plugin bean <" + beanName + "> is referenced in descriptor but doesn't exist in context.");
            } else {
                exposedBeans.put(beanName, bean);
            }
        }
        managedPlugin.setExposedBeans(exposedBeans);
    }

    /**
     * Link the plugin against alien components that may need to use it.
     *
     * @param plugin The plugin to link.
     * @param managedPlugin The managed plugin related to the plugin.
     * @param componentDescriptors The map of component descriptors.
     */
    private void link(Plugin plugin, ManagedPlugin managedPlugin, Map<String, PluginComponentDescriptor> componentDescriptors) {
        // Global linking (rest-mapping for example)
        for (IPluginLoadingCallback callback : SpringUtils.getBeansOfType(alienContext, IPluginLoadingCallback.class)) {
            callback.onPluginLoaded(managedPlugin);
        }

        // Specific bean linking to bean types.
        for (PluginLinker linker : linkers) {
            Map<String, ?> instancesToLink = managedPlugin.getPluginContext().getBeansOfType(linker.linkedType);
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
        // Add a default undefined type for all componentDescriptor that haven't been linked.
        for (PluginComponentDescriptor componentDescriptor : componentDescriptors.values()) {
            if (componentDescriptor.getType() == null) {
                componentDescriptor.setType(UNKNOWN_PLUGIN_COMPONENT_TYPE);
            }
        }
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
        return pluginContexts.get(pluginId).getPlugin().getDescriptor();
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

    public List<PluginComponent> getPluginComponents(String type) {
        List<PluginComponent> pluginComponents = new ArrayList<>();
        for (ManagedPlugin plugin : pluginContexts.values()) {
            PluginDescriptor descriptor = plugin.getPlugin().getDescriptor();
            if (descriptor.getComponentDescriptors() != null) {
                for (PluginComponentDescriptor componentDescriptor : descriptor.getComponentDescriptors()) {
                    if (componentDescriptor.getType().equals(type)) {
                        pluginComponents
                                .add(new PluginComponent(plugin.getPlugin().getId(), descriptor.getName(), descriptor.getVersion(), componentDescriptor));
                    }
                }
            }
        }
        return pluginComponents;
    }

    @AllArgsConstructor(suppressConstructorProperties = true)
    private final class PluginLinker<T> {
        private IPluginLinker<T> linker;
        private Class<T> linkedType;
    }
}
