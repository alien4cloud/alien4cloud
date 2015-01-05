package alien4cloud.configuration;

import java.io.BufferedOutputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;

import lombok.extern.slf4j.Slf4j;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;
import org.springframework.util.StreamUtils;

import alien4cloud.model.components.IndexedNodeType;
import alien4cloud.component.repository.CsarFileRepository;
import alien4cloud.component.repository.exception.CSARVersionAlreadyExistsException;
import alien4cloud.dao.IGenericSearchDAO;
import alien4cloud.tosca.ArchiveUploadService;
import alien4cloud.tosca.parser.ParsingException;
import alien4cloud.utils.FileUtil;

/**
 * First default components initialization
 * Based on configuration in alien4cloud-config.yaml parameter
 *
 * @author mourouvi
 */
@Slf4j
@Component
public class Components {

    private static final String ARCHIVES_FOLDER = "default-normative-types/";
    private static final String ARCHIVES_UPLOAD_PROPERTIES = "upload.properties";

    @Value("${archive.upload_all}")
    private Boolean uploadAllArchive;

    @Resource(name = "alien-es-dao")
    private IGenericSearchDAO componentsDAO;

    @Resource
    private ArchiveUploadService csarUploadService;

    @Value("${directories.alien}/${directories.upload_temp}")
    private String alienTempUpload;

    @PostConstruct
    public void importDefaultComponents() throws IOException {
        // Components
        long nodetypesCount = componentsDAO.count(IndexedNodeType.class, null);
        if (uploadAllArchive.equals(Boolean.TRUE) && nodetypesCount == 0) {

            // prepare upload.properties file
            Properties archivesUploadProperties = new Properties();
            ClassPathResource resource = new ClassPathResource(ARCHIVES_FOLDER + ARCHIVES_UPLOAD_PROPERTIES);
            archivesUploadProperties.load(resource.getInputStream());

            // create or
            Path tempAlienDirectory = FileUtil.createDirectoryIfNotExists(alienTempUpload);

            for (Object key : archivesUploadProperties.keySet()) {
                log.info("Inporting components zip file [" + archivesUploadProperties.get(key) + "]");
                doUpload(tempAlienDirectory, archivesUploadProperties.get(key).toString());
            }
        } else {
            log.info("No default components import");
        }
    }

    private void doUpload(Path tempAlienDirectory, String archivePath) throws IOException {
        ClassPathResource archiveResource = new ClassPathResource(ARCHIVES_FOLDER + archivePath);
        // create a temp file an copy the archive content temporary
        Path temp = Files.createTempFile(tempAlienDirectory, "", '.' + CsarFileRepository.CSAR_EXTENSION);
        StreamUtils.copy(archiveResource.getInputStream(), new BufferedOutputStream(new FileOutputStream(temp.toFile())));

        try {
            // uploading the current file
            csarUploadService.upload(temp);
        } catch (CSARVersionAlreadyExistsException e) {
            log.error("A ZIP with the same name and the same version already existed in the repository", e);
        } catch (ParsingException e) {
            log.error("The ZIP file [" + archivePath + "] content is invalid", e);
        } finally {
            if (temp != null) {
                // Clean up
                try {
                    FileUtil.delete(temp);
                } catch (IOException e) {
                    // The repository might just move the file instead of copying to save IO disk access
                }
            }
        }
    }
}