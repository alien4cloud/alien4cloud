package alien4cloud.csar.services;

import java.util.Set;

import alien4cloud.tosca.container.model.CSARDependency;

/**
 * Interface to manage dependencies around Alien 4 Cloud
 */
public interface ICsarDependencyLoader {

    /**
     * Get a set of all transitive dependencies for a given archive.
     * 
     * @param csarName The archive name.
     * @param csarVersion the archive version.
     * @return The set of all transitive dependencies for a given archive
     */
    Set<CSARDependency> getDependencies(String csarName, String csarVersion);
}
