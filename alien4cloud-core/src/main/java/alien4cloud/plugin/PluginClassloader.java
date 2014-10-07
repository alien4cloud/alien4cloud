package alien4cloud.plugin;

import java.net.URL;
import java.net.URLClassLoader;

import lombok.extern.slf4j.Slf4j;

/**
 * Specific classloader for plugins that do not delegates first to the parent classloader.
 */
@Slf4j
public class PluginClassloader extends URLClassLoader {
    public PluginClassloader(URL[] urls, ClassLoader parent) {
        super(urls, parent);
    }

    @Override
    public Class<?> loadClass(String name) throws ClassNotFoundException {
        Class<?> loadedClass = findLoadedClass(name);
        // if the class has not been loaded already the
        if (loadedClass == null) {
            loadedClass = doLoadClass(name);
        }

        return loadedClass;
    }

    private Class<?> doLoadClass(String name) throws ClassNotFoundException {
        // some dependencies must should not be resolved by the plugin classloader.
        if (name.startsWith("org.slf4j")) {
            return super.loadClass(name);
        }

        // try to resolve locally before delegating to parent.
        try {
            return findClass(name);
        } catch (ClassNotFoundException e) {
            log.debug("Class {} is not found in the plugin - delegating to parent", name);
        }

        return super.loadClass(name);
    }
}