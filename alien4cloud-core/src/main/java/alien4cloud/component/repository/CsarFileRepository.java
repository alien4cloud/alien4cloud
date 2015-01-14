package alien4cloud.component.repository;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

import org.springframework.beans.factory.annotation.Required;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.FileSystemUtils;

import com.fasterxml.jackson.databind.ObjectMapper;

import alien4cloud.component.repository.exception.CSARDirectoryCreationFailureException;
import alien4cloud.component.repository.exception.CSARStorageFailureException;
import alien4cloud.component.repository.exception.CSARVersionAlreadyExistsException;
import alien4cloud.component.repository.exception.CSARVersionNotFoundException;
import alien4cloud.model.components.Csar;
import alien4cloud.tosca.parser.ParsingResult;
import alien4cloud.utils.DirectoryJSonWalker;
import alien4cloud.utils.FileUtil;

/**
 * CSAR Repository implementation<br/>
 *
 * Implements {@link ICsarRepositry}
 *
 * @author 'Igor Ngouagna'
 */
@Slf4j
@Getter
@Setter
@Component
public class CsarFileRepository implements ICsarRepositry {
    public static final String CSAR_EXTENSION = "csar";
    private Path rootPath;

    public CsarFileRepository() {
    }

    public CsarFileRepository(Path rootPath) {
        checkCSARRepository(rootPath);
        this.rootPath = rootPath;
    }

    public CsarFileRepository(String rootPathString) {
        this(Paths.get(rootPathString));
    }

    @Override
    public void storeParsingResults(String name, String version, ParsingResult<Csar> parsingResult) {
        Path csarDirectoryPath = rootPath.resolve(name).resolve(version);

        try {
            Files.createDirectories(csarDirectoryPath);
        } catch (IOException e) {
            throw new CSARDirectoryCreationFailureException("Error while trying to create the CSAR directory <" + csarDirectoryPath.toString() + ">. "
                    + e.getMessage(), e);
        }

        try {
            // save the parsing result as a json file
            ObjectMapper mapper = new ObjectMapper();
            mapper.writeValue(csarDirectoryPath.resolve("parsingresult.json").toFile(), parsingResult);
        } catch (IOException e) {
            throw new CSARDirectoryCreationFailureException("Error while saving parsing results", e);
        }
    }

    @Override
    public void storeCSAR(String name, String version, Path tmpPath) throws CSARVersionAlreadyExistsException {
        // check the tmpPath.
        if (!Files.isReadable(tmpPath)) {
            throw new CSARStorageFailureException("CSAR temp location <" + tmpPath.toString() + "> not found or not readable!");
        }

        Path csarDirectoryPath = rootPath.resolve(name).resolve(version);
        String realName = name.concat("-").concat(version).concat("." + CSAR_EXTENSION);

        // create the storage directory
        createCSARDirectory(csarDirectoryPath, realName);

        // move the archive
        try {
            if (log.isDebugEnabled()) {
                log.debug("tmp: " + tmpPath);
                log.debug(" Dest: " + csarDirectoryPath.resolve(realName));
            }
            Path csarTargetPath = csarDirectoryPath.resolve(realName);
            Files.copy(tmpPath, csarTargetPath);

            // unzip the csar
            Path expandedPath = csarDirectoryPath.resolve("expanded");
            FileUtil.unzip(csarTargetPath, expandedPath);
            DirectoryJSonWalker.directoryJson(expandedPath, csarDirectoryPath.resolve("content.json"));
        } catch (IOException e) {
            throw new CSARStorageFailureException("Error while trying to store the CSAR: " + name + ", Version: " + version + "...." + e.getMessage(), e);
        }
    }

    @Override
    public Path getCSAR(String name, String version) throws CSARVersionNotFoundException {
        String realName = name.concat("-").concat(version).concat("." + CSAR_EXTENSION);

        Path path = rootPath.resolve(name).resolve(version).resolve(realName);
        if (Files.exists(path)) {
            return path;
        }

        throw new CSARVersionNotFoundException("CSAR: " + name + ", Version: " + version + " not found in the repository.");
    }

    private void checkCSARRepository(Path rootPath) {
        if (!Files.isDirectory(rootPath)) {
            try {
                Files.createDirectories(rootPath);
                log.info("Alien Repository folder set to " + rootPath.toAbsolutePath());
            } catch (IOException e) {
                throw new CSARDirectoryCreationFailureException("Error while trying to create the CSAR repository <" + rootPath.toString() + ">. "
                        + e.getMessage(), e);
            }
        } else {
            log.info("Alien Repository folder already created! " + rootPath.toAbsolutePath());
        }
    }

    private void createCSARDirectory(Path csarDirectoryPath, String realName) throws CSARVersionAlreadyExistsException {
        if (Files.exists(csarDirectoryPath.resolve(realName))) {
            log.info("Overriding CSAR with new one.");
            try {
                FileUtil.delete(csarDirectoryPath);
            } catch (IOException e) {
                throw new CSARDirectoryCreationFailureException("Error while trying to delete the CSAR directory <" + csarDirectoryPath.toString() + ">. "
                        + e.getMessage(), e);
            }
        }

        try {
            Files.createDirectories(csarDirectoryPath);
        } catch (IOException e) {
            throw new CSARDirectoryCreationFailureException("Error while trying to create the CSAR directory <" + csarDirectoryPath.toString() + ">. "
                    + e.getMessage(), e);
        }
    }

    @Required
    @Value("${directories.alien}/${directories.csar_repository}")
    public void setRootPath(String path) {
        this.rootPath = Paths.get(path).toAbsolutePath();
        checkCSARRepository(rootPath);
    }

    @Override
    public void removeCSAR(String name, String version) {
        Path csarDirectoryPath = rootPath.resolve(name).resolve(version);
        if (Files.isDirectory(csarDirectoryPath)) {
            FileSystemUtils.deleteRecursively(csarDirectoryPath.toFile());
        }
    }
    
}
