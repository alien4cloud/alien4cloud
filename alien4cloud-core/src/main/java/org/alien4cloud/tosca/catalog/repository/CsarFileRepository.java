package org.alien4cloud.tosca.catalog.repository;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;

import org.alien4cloud.tosca.model.Csar;
import org.springframework.beans.factory.annotation.Required;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.FileSystemUtils;

import alien4cloud.component.repository.exception.CSARDirectoryCreationFailureException;
import alien4cloud.component.repository.exception.CSARStorageFailureException;
import alien4cloud.exception.NotFoundException;
import alien4cloud.utils.DirectoryJSonWalker;
import alien4cloud.utils.FileUtil;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

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
    private static final String EXPANDED = "expanded";
    private Path rootPath;

    @Required
    @Value("${directories.alien}/${directories.csar_repository}")
    public void setRootPath(String path) {
        this.rootPath = Paths.get(path).toAbsolutePath();

        if (!Files.isDirectory(rootPath)) {
            try {
                Files.createDirectories(rootPath);
                log.info("Alien Repository folder set to " + rootPath.toAbsolutePath());
            } catch (IOException e) {
                throw new CSARDirectoryCreationFailureException(
                        "Error while trying to create the CSAR repository <" + rootPath.toString() + ">. " + e.getMessage(), e);
            }
        } else {
            log.info("Alien Repository folder already created! " + rootPath.toAbsolutePath());
        }
    }

    @Override
    public synchronized void storeCSAR(Csar csar, String yaml) {
        Path csarDirectoryPath = rootPath.resolve(csar.getName()).resolve(csar.getVersion());
        String realName = csar.getName().concat("-").concat(csar.getVersion()).concat("." + CSAR_EXTENSION);
        createCSARDirectory(csarDirectoryPath, realName);

        Path csarExpandedDirectoryPath = csarDirectoryPath.resolve(EXPANDED);
        try {
            Files.createDirectories(csarExpandedDirectoryPath);
            Path targetPath = csarExpandedDirectoryPath.resolve(csar.getYamlFilePath());

            try (BufferedWriter writer = Files.newBufferedWriter(targetPath)) {
                writer.write(yaml);
            }
        } catch (IOException e) {
            throw new CSARDirectoryCreationFailureException(
                    "Error while trying to create the CSAR directory <" + csarDirectoryPath.toString() + ">. " + e.getMessage(), e);
        }
    }

    @Override
    public synchronized void storeCSAR(Csar csar, Path tmpPath) {
        // check the tmpPath.
        if (!Files.isReadable(tmpPath)) {
            throw new CSARStorageFailureException("CSAR temp location <" + tmpPath.toString() + "> not found or not readable!");
        }

        Path csarDirectoryPath = rootPath.resolve(csar.getName()).resolve(csar.getVersion());
        String realName = csar.getName().concat("-").concat(csar.getVersion()).concat("." + CSAR_EXTENSION);

        // create the storage directory
        createCSARDirectory(csarDirectoryPath, realName);

        // move the archive
        try {
            if (log.isDebugEnabled()) {
                log.debug("tmp: " + tmpPath);
                log.debug(" Dest: " + csarDirectoryPath.resolve(realName));
            }
            Path csarTargetPath = csarDirectoryPath.resolve(realName);
            Path expandedPath = csarDirectoryPath.resolve("expanded");
            if (Files.isRegularFile(tmpPath)) {
                Files.copy(tmpPath, csarTargetPath);
                FileUtil.unzip(csarTargetPath, expandedPath);
            } else {
                FileUtil.copy(tmpPath, expandedPath, StandardCopyOption.REPLACE_EXISTING);
            }
            DirectoryJSonWalker.directoryJson(expandedPath, csarDirectoryPath.resolve("content.json"));
        } catch (IOException e) {
            throw new CSARStorageFailureException(
                    "Error while trying to store the CSAR: " + csar.getName() + ", Version: " + csar.getVersion() + "...." + e.getMessage(), e);
        }
    }

    private void createCSARDirectory(Path csarDirectoryPath, String realName) {
        if (Files.exists(csarDirectoryPath.resolve(realName))) {
            log.info("Overriding CSAR with new one.");
            try {
                FileUtil.delete(csarDirectoryPath);
            } catch (IOException e) {
                throw new CSARDirectoryCreationFailureException(
                        "Error while trying to delete the CSAR directory <" + csarDirectoryPath.toString() + ">. " + e.getMessage(), e);
            }
        }

        try {
            Files.createDirectories(csarDirectoryPath);
        } catch (IOException e) {
            throw new CSARDirectoryCreationFailureException(
                    "Error while trying to create the CSAR directory <" + csarDirectoryPath.toString() + ">. " + e.getMessage(), e);
        }
    }

    @Override
    public Path getCSAR(String name, String version) {
        Path csarDir = rootPath.resolve(name).resolve(version);
        Path expandedPath = csarDir.resolve("expanded");
        Path zippedPath = csarDir.resolve(name.concat("-").concat(version).concat("." + CSAR_EXTENSION));
        if (Files.exists(zippedPath)) {
            return zippedPath;
        } else if (Files.exists(expandedPath)) {
            // the csar wasn't stored as a zip file. Zip the expanded dir then
            try {
                FileUtil.zip(expandedPath, zippedPath);
                return zippedPath;
            } catch (IOException e) {
                log.error("Failed to zip directory " + expandedPath, e);
                throw new NotFoundException("CSAR: " + name + ", Version: " + version + " not found in the repository.");
            }
        }

        throw new NotFoundException("CSAR: " + name + ", Version: " + version + " not found in the repository.");
    }

    @Override
    public Path getExpandedCSAR(String name, String version) {
        Path csarDir = rootPath.resolve(name).resolve(version);
        Path expandedPath = csarDir.resolve("expanded");
        if (Files.exists(expandedPath)) {
            return expandedPath;
        }
        throw new NotFoundException("CSAR: " + name + ", Version: " + version + " not found in the repository.");
    }

    @Override
    public void removeCSAR(String name, String version) {
        Path csarDirectoryPath = rootPath.resolve(name).resolve(version);
        if (Files.isDirectory(csarDirectoryPath)) {
            FileSystemUtils.deleteRecursively(csarDirectoryPath.toFile());
        }
    }
}