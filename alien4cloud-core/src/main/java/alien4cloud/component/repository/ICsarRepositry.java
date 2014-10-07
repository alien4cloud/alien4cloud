package alien4cloud.component.repository;

import java.nio.file.Path;

import alien4cloud.component.repository.exception.CSARVersionAlreadyExistsException;
import alien4cloud.component.repository.exception.CSARVersionNotFoundException;

/**
 * 
 * Interface for all CSAR repository
 * 
 * @author 'Igor Ngouagna'
 * 
 */
public interface ICsarRepositry {

    /**
     * Store an CSAR into the repository. This method will perform a move of the temporary file to save IO disk operations
     * 
     * @param name
     *            the name of the CSAR to store.
     * @param version
     *            the version of the CSAR to store.
     * @param tmpPath
     *            the path to the temporary directory where the CSAR is located. The file will be moved to its new location inside the repository.
     * @throws CSARVersionAlreadyExistsException
     */
    void storeCSAR(String name, String version, Path tmpPath) throws CSARVersionAlreadyExistsException;

    /**
     * Get a CSAR stored into the repository
     * 
     * @param name
     *            the name of the CSAR to store.
     * @param version
     *            the version of the CSAR to store.
     * 
     * @return {@link Path} <br>
     *         The path to the CSAR file
     * @throws CSARVersionNotFoundException
     */
    Path getCSAR(String name, String version) throws CSARVersionNotFoundException;

    /**
     * Get the root path of the repository
     * 
     * @return {@link String}<br>
     * 
     */
    String getRootPathString();

    /**
     * Get the name of the repository
     * 
     * @return {@link String}<br>
     */
    String getName();

}
