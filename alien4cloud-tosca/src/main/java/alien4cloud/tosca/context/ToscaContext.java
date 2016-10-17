package alien4cloud.tosca.context;

import alien4cloud.component.ICSARRepositorySearchService;
import alien4cloud.tosca.model.ArchiveRoot;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.alien4cloud.tosca.model.CSARDependency;
import org.alien4cloud.tosca.model.Csar;
import org.alien4cloud.tosca.model.types.*;

import java.util.*;
import java.util.function.Predicate;

import static alien4cloud.utils.AlienUtils.safe;

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
     * Set an existing tosca context.
     * 
     * @param context The context to set.
     */
    public static void set(Context context) {
        contextThreadLocal.set(context);
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
    public static <T extends AbstractToscaType> T get(Class<T> elementClass, String elementId) {
        return contextThreadLocal.get().getElement(elementClass, elementId, false);
    }

    /**
     * Get an element from the local-cache or from ES.
     *
     * @param elementClass The class of the element to look for.
     * @param elementId The id of the element to look for.
     * @param <T> The type of element.
     * @return The requested element.
     */
    public static <T extends AbstractToscaType> T getOrFail(Class<T> elementClass, String elementId) {
        return contextThreadLocal.get().getElement(elementClass, elementId, true);
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
        /** Cached types in the context. <ElementType, <ElementId, Element>> */
        private final Map<String, Map<String, AbstractToscaType>> toscaTypesCache = Maps.newHashMap();

        public Context(Set<CSARDependency> dependencies) {
            this.dependencies = dependencies;
        }

        private CSARDependency getDependencyByName(String dependencyName) {
            for (CSARDependency d : dependencies) {
                if (d.getName().equals(dependencyName)) {
                    return d;
                }
            }
            return null;
        }

        /**
         * Update the ToscaContext to take in account the new dependencies.
         *
         * @param newDependencies The new list of dependencies for this context.
         */
        public void updateDependencies(Set<CSARDependency> newDependencies) {
            Map<String, CSARDependency> dependenciesByName = Maps.newHashMap();
            for (CSARDependency dependency : dependencies) {
                dependenciesByName.put(dependency.getName(), dependency);
            }
            // now add/ update /remove dependencies to match the new dependencies.
            for (CSARDependency dependency : newDependencies) {
                CSARDependency previous = dependenciesByName.remove(dependency.getName());
                if (previous == null) {
                    addDependency(dependency);
                } else if (!previous.getVersion().equals(dependency.getVersion())) {
                    updateDependency(dependency);
                }
            }
            for (CSARDependency dependency : dependenciesByName.values()) {
                removeDependency(dependency);
            }
        }

        /**
         * Add a dependency to the current context.
         * 
         * @param dependency The dependency to add.
         */
        public void addDependency(CSARDependency dependency) {
            log.debug("Add dependency to context", dependency);
            if (dependency.getHash() == null) {
                // we should try to get the hash from the repository
                Csar csar = getArchive(dependency.getName(), dependency.getVersion());
                dependency.setHash(csar.getHash());
            }
            dependencies.add(dependency);
        }

        /**
         * Remove a given dependency from the TOSCA Context by name.
         *
         * @param removedDependency The dependency to remove.
         */
        public void removeDependency(CSARDependency removedDependency) {
            CSARDependency existingDependency = getDependencyByName(removedDependency.getName());
            if (existingDependency != null) {
                dependencies.remove(existingDependency);
                List<String> toRemove = Lists.newArrayList();
                // unload all types from this dependency
                for (Map<String, AbstractToscaType> elementMap : toscaTypesCache.values()) {
                    for (Map.Entry<String, AbstractToscaType> entry : elementMap.entrySet()) {
                        if (entry.getValue().getArchiveName().equals(existingDependency.getName())
                                && entry.getValue().getArchiveVersion().equals(existingDependency.getVersion())) {
                            toRemove.add(entry.getKey());
                        }
                    }
                    for (String removeId : toRemove) {
                        elementMap.remove(removeId);
                    }
                    toRemove.clear();
                }

                // alse clean the archive cache
                Csar csar = new Csar(removedDependency.getName(), removedDependency.getVersion());
                archivesMap.remove(csar.getId());
                log.debug("Removed dependency {} from the TOSCA context.", removedDependency);
            } else {
                log.debug("Cannot remove dependency {} from the TOSCA context as it wasn't found in the dependencies.", removedDependency);
            }
        }

        /**
         * Performs an add or update of a dependency in a TOSCA context.
         *
         * @param dependency The updated dependency.
         */
        public void updateDependency(CSARDependency dependency) {
            // Do not update dependency if the version hasn't changed
            if (hasDependency(dependency)) {
                log.debug("Dependency already exist in context.");
                return;
            }
            removeDependency(dependency);
            addDependency(dependency);
        }

        private boolean hasDependency(CSARDependency dependency) {
            for (CSARDependency existDependency : this.dependencies) {
                if (existDependency.getName().equals(dependency.getName())) {
                    return existDependency.getVersion().equals(dependency.getVersion()) && Objects.equals(existDependency.getHash(), dependency.getHash());
                }
            }
            return false;
        }

        /**
         * Load all elements from the given archive in the context.
         * 
         * @param root The parsed archive to load.
         */
        public void register(ArchiveRoot root) {
            log.debug("Register archive {}", root);
            archivesMap.put(root.getArchive().getId(), root.getArchive());
            register(ArtifactType.class, root.getArtifactTypes());
            register(CapabilityType.class, root.getCapabilityTypes());
            register(DataType.class, root.getDataTypes());
            register(NodeType.class, root.getNodeTypes());
            register(RelationshipType.class, root.getRelationshipTypes());
        }

        private <T extends AbstractToscaType> void register(Class<T> elementClass, Map<String, T> elementMap) {
            String elementType = elementClass.getSimpleName();
            Map<String, AbstractToscaType> typeElements = toscaTypesCache.get(elementType);
            if (typeElements == null) {
                typeElements = new HashMap<>();
                toscaTypesCache.put(elementType, typeElements);
            }
            if (elementMap == null) {
                return;
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
            log.debug("get archive from map {} {} {}", id, archive);
            if (archive == null) {
                archive = csarSearchService.getArchive(name, version);
                log.debug("get archive from repo {} {} {}", id, archive, csarSearchService.getClass().getName());
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
        public <T extends AbstractToscaType> T getElement(Class<T> elementClass, String elementId, boolean required) {
            String elementType = elementClass.getSimpleName();
            Map<String, AbstractToscaType> typeElements = toscaTypesCache.get(elementType);
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

            T element = required ? csarSearchService.getRequiredElementInDependencies(elementClass, elementId, dependencies)
                    : csarSearchService.getElementInDependencies(elementClass, elementId, dependencies);
            if (element != null) {
                typeElements.put(elementId, element);
            }
            log.debug("Retrieve element {} {}", element, dependencies);
            return element;
        }

        public <T extends AbstractToscaType> Optional<AbstractToscaType> getElement(Class<T> elementClass, Predicate<AbstractToscaType> filter) {
            String elementType = elementClass.getSimpleName();
            Map<String, AbstractToscaType> typeElements = toscaTypesCache.get(elementType);
            return safe(typeElements).values().stream().filter(filter).findFirst();
        }
    }
}