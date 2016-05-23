package alien4cloud.tosca.context;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import com.google.common.collect.Maps;

import alien4cloud.component.ICSARRepositorySearchService;
import alien4cloud.model.components.*;
import alien4cloud.tosca.model.ArchiveRoot;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

/**
 * Manage thread-local tosca contexts.
 */
@Slf4j
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
        /** Current context dependencies. */
        private final Set<CSARDependency> dependencies;
        /** Context archives. */
        private final Map<String, Csar> archivesMap = Maps.newHashMap();
        /** Cached types in the context. */
        private final Map<String, Map<String, IndexedToscaElement>> toscaTypesCache = Maps.newHashMap();

        private Context(Set<CSARDependency> dependencies) {
            this.dependencies = dependencies;
        }

        /**
         * Add a dependency to the current context.
         * 
         * @param dependency The dependency to add.
         */
        public void addDependency(CSARDependency dependency) {
            log.info("Add dependency to context", dependency);
            dependencies.add(dependency);
        }

        /**
         * Load all elements from the given archive in the context.
         * 
         * @param root The parsed archive to load.
         */
        public void register(ArchiveRoot root) {
            log.info("Register archive {}", root);
            archivesMap.put(root.getArchive().getId(), root.getArchive());
            register(IndexedArtifactType.class, root.getArtifactTypes());
            register(IndexedCapabilityType.class, root.getCapabilityTypes());
            register(IndexedDataType.class, root.getDataTypes());
            register(IndexedNodeType.class, root.getNodeTypes());
            register(IndexedRelationshipType.class, root.getRelationshipTypes());
        }

        private <T extends IndexedToscaElement> void register(Class<T> elementClass, Map<String, T> elementMap) {
            String elementType = elementClass.getSimpleName();
            Map<String, IndexedToscaElement> typeElements = toscaTypesCache.get(elementType);
            if (typeElements == null) {
                typeElements = new HashMap<>();
                toscaTypesCache.put(elementType, typeElements);
            }
            typeElements.putAll(elementMap);
        }

        /**
         * Get an archive from it's id.
         * 
         * @param name The name of the archive to get.
         * @param version The version of the archive to get.
         * @return The archive from it's id.
         */
        public Csar getArchive(String name, String version) {
            String id = new Csar(name, version).getId();
            Csar archive = archivesMap.get(id);
            log.info("get archive from map {} {}", id, archive);
            if (archive == null) {
                archive = csarSearchService.getArchive(id);
                log.info("get archive from repo {} {} {}", id, archive, csarSearchService.getClass().getName());
                archivesMap.put(id, archive);
            }
            return archive;
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
            log.info("Retrieve element {} {}", element, dependencies);
            return element;
        }
    }
}