package alien4cloud.component.repository;

import java.nio.file.Path;

import alien4cloud.component.repository.exception.CSARVersionAlreadyExistsException;
import alien4cloud.component.repository.exception.CSARVersionNotFoundException;

/**
 * Interface for all CSAR repositories
 */
public interface ICsarRepositry {
    /**
     * Store an CSAR into the repository. This method will perform a move of the temporary file to save IO disk operations
     * 
     * @param name the name of the CSAR to store.
     * @param version the version of the CSAR to store.
     * @param tmpPath the path to the temporary directory where the CSAR is located. The file will be moved to its new location inside the repository.
     * @throws CSARVersionAlreadyExistsException
     */
    void storeCSAR(String name, String version, Path tmpPath) throws CSARVersionAlreadyExistsException;

    /**
     * Get a CSAR stored into the repository
     *
     * @param name The name of the csar.
     * @param version The version of the CSAR
     * @return The path to the zipped csar file.
     * @throws CSARVersionNotFoundException
     */
    Path getCSAR(String name, String version) throws CSARVersionNotFoundException;

    /**
     * Get the path of the expended directory in which the CSAR is stored.
     * 
     * @param name The name of the csar.
     * @param version The version of the CSAR
     * @return The path to the expended csar file.
     * @throws CSARVersionNotFoundException
     */
    Path getExpandedCSAR(String name, String version) throws CSARVersionNotFoundException;

    void removeCSAR(String name, String version);

}