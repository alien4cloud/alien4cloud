package alien4cloud.component.repository;

import java.nio.file.Path;

import alien4cloud.component.repository.exception.CSARVersionAlreadyExistsException;
import alien4cloud.component.repository.exception.CSARVersionNotFoundException;
import alien4cloud.model.components.Csar;
import alien4cloud.tosca.parser.ParsingResult;

/**
 * Interface for all CSAR repositories
 */
public interface ICsarRepositry {

    /**
     * Store the given parsing results in the repository.
     * 
     * @param name the name of the CSAR to store.
     * @param version the version of the CSAR to store.
     * @param parsingResult the result of the parsing of the CSAR.
     */
    void storeParsingResults(String name, String version, ParsingResult<Csar> parsingResult);

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
}