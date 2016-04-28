package alien4cloud.tosca;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import lombok.Setter;
import alien4cloud.component.ICSARRepositorySearchService;
import alien4cloud.model.components.CSARDependency;
import alien4cloud.model.components.IndexedToscaElement;

import com.google.common.collect.Maps;

/**
 * Manage thread-local tosca contexts.
 */
public class ToscaContext {
    @Setter
    private static ICSARRepositorySearchService csarSearchService;
    private final static ThreadLocal<Context> contextThreadLocal = new ThreadLocal<>();

    /**
     * Create a new instance of Context.
     *
     * @param dependencies The list of dependencies for this context.
     * @return An instance of a Context.
     */
    public static void init(final Set<CSARDependency> dependencies) {
        contextThreadLocal.set(new Context(dependencies));
    }

    /**
     * Get the context for the current thread.
     *
     * @return The context.
     */
    public static Context get() {
        return contextThreadLocal.get();
    }

    /**
     * Get an element from the local-cache or from ES.
     *
     * @param elementClass The class of the element to look for.
     * @param elementId The id of the element to look for.
     * @param <T> The type of element.
     * @return The requested element.
     */
    public static <T extends IndexedToscaElement> T get(Class<T> elementClass, String elementId) {
        return contextThreadLocal.get().getElement(elementClass, elementId);
    }

    /**
     * Destroy the tosca context.
     */
    public static void destroy() {
        contextThreadLocal.remove();
    }

    /**
     * Tosca context allows to cache TOSCA elements
     */
    public static class Context {
        private final Map<String, Map<String, IndexedToscaElement>> toscaTypesCache = Maps.newHashMap();
        private final Set<CSARDependency> dependencies;

        private Context(Set<CSARDependency> dependencies) {
            this.dependencies = dependencies;
        }

        /**
         * Get an element from the local-cache or from ES.
         *
         * @param elementClass The class of the element to look for.
         * @param elementId The id of the element to look for.
         * @param <T> The type of element.
         * @return The requested element.
         */
        public <T extends IndexedToscaElement> T getElement(Class<T> elementClass, String elementId) {
            String elementType = elementClass.getSimpleName();
            Map<String, IndexedToscaElement> typeElements = toscaTypesCache.get(elementType);
            if (typeElements == null) {
                typeElements = new HashMap<>();
                toscaTypesCache.put(elementType, typeElements);
            } else {
                // find in local-cache
                T element = (T) typeElements.get(elementId);
                if (element != null) {
                    return element;
                }
            }

            T element = csarSearchService.getRequiredElementInDependencies(elementClass, elementId, dependencies);
            typeElements.put(elementId, element);
            return element;
        }
    }
}