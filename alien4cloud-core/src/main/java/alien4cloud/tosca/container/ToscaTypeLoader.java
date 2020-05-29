package alien4cloud.tosca.container;

import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import org.alien4cloud.tosca.catalog.index.ICsarDependencyLoader;
import org.alien4cloud.tosca.model.CSARDependency;

import com.google.common.collect.Maps;
import com.google.common.collect.Sets;

import alien4cloud.utils.VersionUtil;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

@Getter
@Setter
@Slf4j
public class ToscaTypeLoader {
    /** Map of type names per archives. */
    private Map<CSARDependency, Set<String>> dependenciesMap = Maps.newHashMap();

    /** Map of Dependencies by name */
    private Map<String,CSARDependency> dependenciesByName = Maps.newHashMap();

    /** Count the usage of a given type in the current context. */
    private Map<String, Integer> typeUsagesMap = Maps.newHashMap();

    private ICsarDependencyLoader csarDependencyLoader;

    public ToscaTypeLoader(ICsarDependencyLoader csarDependencyLoader) {
        this.csarDependencyLoader = csarDependencyLoader;
    }

    public Set<CSARDependency> getLoadedDependencies() {
        return Sets.newHashSet(dependenciesMap.keySet());
    }

    /**
     * Try to unload the given type
     *
     * @param type name of the type
     */
    public void unloadType(String type) {
        if (log.isDebugEnabled()) {
            log.debug("Unload type [" + type + "]");
        }
        Integer currentUsageCount = typeUsagesMap.get(type);
        if (currentUsageCount != null) {
            if (currentUsageCount <= 1) {
                // Not used anymore in the topology
                typeUsagesMap.remove(type);
                Iterator<Map.Entry<CSARDependency, Set<String>>> dependencyIterator = dependenciesMap.entrySet().iterator();
                while (dependencyIterator.hasNext()) {
                    Map.Entry<CSARDependency, Set<String>> entry = dependencyIterator.next();
                    entry.getValue().remove(type);
                    if (entry.getValue().isEmpty()) {
                        if (log.isDebugEnabled()) {
                            log.debug("Dependency is not used anymore and will be removed [" + entry.getKey() + "] due to unload of type [" + type + "]");
                            log.debug("Type usage [" + typeUsagesMap + "]");
                            log.debug("Dependencies usage [" + dependenciesMap + "]");
                        }
                        dependenciesByName.remove(entry.getKey().getName());
                        dependencyIterator.remove();
                    }
                }
            } else {
                // Still used
                typeUsagesMap.put(type, currentUsageCount - 1);
            }
        }

        if (log.isDebugEnabled()) {
            log.debug("Type usage [" + typeUsagesMap + "]");
            log.debug("Dependencies usage [" + dependenciesMap + "]");
        }
    }

    /**
     * Add a dependency
     *
     * @param dependency
     * @param type
     * @return True if the dependecy has been upgraded into the topology. False if not.
     */
    private boolean addNewDependency(CSARDependency dependency, String type) {
        CSARDependency currentDependency = dependenciesByName.get(dependency.getName());

        // New dependency that never exists before
        if (currentDependency == null) {
            dependenciesByName.put(dependency.getName(),dependency);
            dependenciesMap.put(dependency, Sets.newHashSet(type));
            return false;
        }
        // Dependency that already existed,
        // The new version is more recent, we will override with new version with warning
        if (VersionUtil.compare(dependency.getVersion(), currentDependency.getVersion()) > 0) {
            Set<String> typesLoadedByConflictingArchive = dependenciesMap.remove(currentDependency);
            typesLoadedByConflictingArchive.add(type);
            dependenciesMap.put(dependency, typesLoadedByConflictingArchive);
            dependenciesByName.put(dependency.getName(),dependency);
            log.warn("Version conflicting for archive [" + dependency.getName() + "] override current version [" + currentDependency.getVersion() + "] with ["
                    + dependency.getVersion() + "]");
            return true;
        }

        log.warn("Version conflicting for archive [" + dependency.getName() + "] do not override and use current version [" + currentDependency.getVersion()
                + "] ignore old version [" + dependency.getVersion() + "]");
        dependenciesMap.get(currentDependency).add(type);
        return false;
    }

    /**
     * Add directDependency for the given type from the given archive.
     * If the directDependency is of an deprecated version ( < than found in the existing dependencies), ignore it.
     * If the directDependency is of a more recent version, force the dependency of the topology to the more recent one
     *
     * @param type name of the type
     * @param directDependency the direct directDependency to load the type
     * @return True if the dependency has been upgraded into the topology. False if not.
     */
    public boolean loadType(String type, CSARDependency directDependency) {
        boolean upgraded = false;
        if (log.isDebugEnabled()) {
            log.debug("Load type [" + type + "] from dependency [" + directDependency + "]");
        }

        Set<String> typesLoadedByDependency = dependenciesMap.get(directDependency);
        // Increment usage count
        Integer currentUsageCount = typeUsagesMap.get(type);
        if (currentUsageCount == null) {
            currentUsageCount = Integer.valueOf(0);
        }
        typeUsagesMap.put(type, currentUsageCount + 1);
        if (typesLoadedByDependency != null) {
            typesLoadedByDependency.add(type);
            // make sure we replace the key because the Equals on CSARDependency is only based on the name and the version
            dependenciesMap.remove(directDependency);
            dependenciesMap.put(directDependency, typesLoadedByDependency);
        } else {
            upgraded = addNewDependency(directDependency, type);
        }
        Set<CSARDependency> transitiveDependencies = csarDependencyLoader.getDependencies(directDependency.getName(), directDependency.getVersion());
        for (CSARDependency transitiveDependency : transitiveDependencies) {
            Set<String> transitiveTypesLoadedByDependency = dependenciesMap.get(transitiveDependency);
            if (transitiveTypesLoadedByDependency == null) {
                addNewDependency(csarDependencyLoader.buildDependencyBean(transitiveDependency.getName(), transitiveDependency.getVersion()), type);
            } else {
                transitiveTypesLoadedByDependency.add(type);
            }
        }
        if (log.isDebugEnabled()) {
            log.debug("Type usage [" + typeUsagesMap + "]");
            log.debug("Dependencies usage [" + dependenciesMap + "]");
        }
        return upgraded;
    }

}