package alien4cloud.tosca.container.archive;

import java.nio.file.Path;

import javax.annotation.Resource;

import org.springframework.stereotype.Component;

import alien4cloud.component.repository.ICsarRepositry;
import alien4cloud.component.repository.exception.CSARVersionAlreadyExistsException;
import alien4cloud.csar.model.Csar;
import alien4cloud.csar.services.CsarService;
import alien4cloud.tosca.container.exception.CSARParsingException;
import alien4cloud.tosca.container.exception.CSARValidationException;
import alien4cloud.tosca.container.model.CloudServiceArchive;
import alien4cloud.tosca.container.validation.CSARValidationResult;

@Component
public class CsarUploadService {

    @Resource
    private ArchiveParser archiveParser;

    @Resource
    private ArchivePostProcessor archivePostProcessor;

    @Resource
    private ArchiveValidator archiveValidator;

    @Resource
    private ArchiveIndexer archiveIndexer;

    @Resource
    private ArchiveImageLoader archiveImageLoader;

    @Resource
    private ICsarRepositry archiveRepositry;

    @Resource
    private CsarService csarService;

    public Csar uploadCsar(Path csarPath) throws CSARParsingException, CSARVersionAlreadyExistsException, CSARValidationException {
        CloudServiceArchive cloudServiceARchive = archiveParser.parseArchive(csarPath);
        archivePostProcessor.postProcessArchive(cloudServiceARchive);
        CSARValidationResult validationResult = archiveValidator.validateArchive(cloudServiceARchive);
        if (validationResult.isValid()) {
            // manage images before archive storage in the repository
            archiveImageLoader.importImages(csarPath, cloudServiceARchive);
            // save the archive in the repository
            archiveRepositry.storeCSAR(cloudServiceARchive.getMeta().getName(), cloudServiceARchive.getMeta().getVersion(), csarPath);
            // index the archive content in elastic-search
            archiveIndexer.indexArchive(cloudServiceARchive);
            // Save in elastic search
            return csarService.saveUploadedCsar(cloudServiceARchive);
        } else {
            throw new CSARValidationException("archive is not valid", validationResult);
        }
    }
}
