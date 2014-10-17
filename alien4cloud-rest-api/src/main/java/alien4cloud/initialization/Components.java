package alien4cloud.initialization;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Properties;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import javax.servlet.ServletContext;

import lombok.extern.slf4j.Slf4j;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import alien4cloud.component.model.IndexedNodeType;
import alien4cloud.component.repository.exception.CSARVersionAlreadyExistsException;
import alien4cloud.dao.IGenericSearchDAO;
import alien4cloud.tosca.container.archive.CsarUploadService;
import alien4cloud.tosca.container.exception.CSARParsingException;
import alien4cloud.tosca.container.exception.CSARValidationException;

/**
 * First default components initialization
 * Based on configuration in alien4cloud-config.yaml parameter
 *
 * @author mourouvi
 */
@Slf4j
@Component
public class Components {
    private static final String ARCHIVES_FOLDER = "/WEB-INF/archives";
    private static final String ARCHIVES_UPLOAD_PROPERTIES = "upload.properties";

    @Value("${archive.upload_all}")
    private Boolean uploadAllArchive;

    @Resource(name = "alien-es-dao")
    private IGenericSearchDAO componentsDAO;

    @Resource
    private CsarUploadService csarUploadService;

    @Autowired
    private ServletContext context;

    @PostConstruct
    public void importDefaultComponents() throws IOException {

        // Components
        long nodetypesCount = componentsDAO.count(IndexedNodeType.class, null);
        if (uploadAllArchive.equals(Boolean.TRUE) && nodetypesCount == 0) {

            Properties archivesUploadProperties = new Properties();
            String archiveRootFolder = context.getRealPath(ARCHIVES_FOLDER) + "/";
            Path uploadPropertyFile = Paths.get(archiveRootFolder + ARCHIVES_UPLOAD_PROPERTIES);
            Path archiveFile = null;
            InputStream propertyFile = new FileInputStream(uploadPropertyFile.toFile());
            archivesUploadProperties.load(propertyFile);

            for (Object key : archivesUploadProperties.keySet()) {
                log.info("Importing components zip file [" + archivesUploadProperties.get(key) + "]");
                archiveFile = Paths.get(archiveRootFolder + archivesUploadProperties.get(key));
                try {
                    // uploading the current file
                    csarUploadService.uploadCsar(archiveFile);
                } catch (CSARVersionAlreadyExistsException e) {
                    log.error("A ZIP with the same name and the same version already existed in the repository", e);
                } catch (CSARParsingException | CSARValidationException e) {
                    log.error("The ZIP file [" + archivesUploadProperties.get(key) + "] content is invalid", e);
                }
            }

        } else {
            log.info("No default components import");
        }
    }
}
