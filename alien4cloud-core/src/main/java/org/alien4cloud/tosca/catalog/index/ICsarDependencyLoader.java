package org.alien4cloud.tosca.catalog.index;

import java.util.Set;

import org.alien4cloud.tosca.model.CSARDependency;

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

    /**
     * Build a {@link CSARDependency} bean given an archive name and version. This will also fill in the dependency hash.
     *
     * @param name The name of the dependendy
     * @param version The version of the dependency
     * @return
     */
    CSARDependency buildDependencyBean(String name, String version);
}
