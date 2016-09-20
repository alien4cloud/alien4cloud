package org.alien4cloud.tosca.catalog.repository;

import java.nio.file.Path;

import org.alien4cloud.tosca.model.Csar;

/**
 * Interface for all CSAR repositories
 */
public interface ICsarRepositry {
    /**
     * Store a new csar in the repository from the content of the TOSCA yaml file.
     *
     * @param csar The archive to store.
     * @param yaml The content of the TOSCA yaml file.
     */
    void storeCSAR(Csar csar, String yaml);

    /**
     * Store an CSAR into the repository. This method will perform a move of the temporary file to save IO disk operations
     *
     * @param csar The archive to store.
     * @param tmpPath the path to the temporary directory where the CSAR is located. The file will be moved to its new location inside the repository.
     */
    void storeCSAR(Csar csar, Path tmpPath);

    /**
     * Get a CSAR stored into the repository
     *
     * @param name The name of the csar.
     * @param version The version of the CSAR
     * @return The path to the zipped csar file.
     */
    Path getCSAR(String name, String version);

    /**
     * Get the path of the expended directory in which the CSAR is stored.
     *
     * @param name The name of the csar.
     * @param version The version of the CSAR
     * @return The path to the expended csar file.
     */
    Path getExpandedCSAR(String name, String version);

    /**
     * Delete an archive from the local repository.
     *
     * @param name The archive to delete.
     * @param version The version of the archive to delete.
     */
    void removeCSAR(String name, String version);
}