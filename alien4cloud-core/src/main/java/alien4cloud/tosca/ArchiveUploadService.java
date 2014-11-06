package alien4cloud.tosca;

import java.nio.file.Path;

import javax.annotation.Resource;

import org.springframework.stereotype.Component;

import alien4cloud.component.repository.ICsarRepositry;
import alien4cloud.component.repository.exception.CSARVersionAlreadyExistsException;
import alien4cloud.csar.services.CsarService;
import alien4cloud.tosca.container.services.csar.ICSARRepositoryIndexerService;
import alien4cloud.tosca.model.ArchiveRoot;
import alien4cloud.tosca.parser.ParsingError;
import alien4cloud.tosca.parser.ParsingErrorLevel;
import alien4cloud.tosca.parser.ParsingException;
import alien4cloud.tosca.parser.ParsingResult;

@Component
public class ArchiveUploadService {
    @Resource
    private ArchiveParser parser;
    @Resource
    private ArchivePostProcessor postProcessor;
    @Resource
    private ArchiveImageLoader imageLoader;
    @Resource
    private ICsarRepositry archiveRepositry;
    @Resource
    private CsarService csarService;
    @Resource
    private ICSARRepositoryIndexerService indexerService;

    /**
     * Upload a TOSCA archive and index it's components.
     * 
     * @param path The archive path.
     * @return The Csar object from the parsing.
     * @throws ParsingException
     * @throws CSARVersionAlreadyExistsException
     */
    public ParsingResult<ArchiveRoot> upload(Path path) throws ParsingException, CSARVersionAlreadyExistsException {
        // TODO issue tolerance should depends of the version (SNAPSHOT).

        // parse the archive.
        ParsingResult<ArchiveRoot> parsingResult = parser.parse(path);
        postProcessor.postProcess(parsingResult);
        // TODO check if there is any specific validation to be done on a bean as CSAR ?

        // check if any blocker error has been found during parsing process.
        if (hasBlockerError(parsingResult)) {
            // save the archive in the repository
            archiveRepositry.storeCSAR(parsingResult.getResult().getArchive().getName(), parsingResult.getResult().getArchive().getVersion(), path);
            // manage images before archive storage in the repository
            imageLoader.importImages(path, parsingResult);
            // index the archive content in elastic-search

            csarService.save(parsingResult.getResult().getArchive());
        }
        return null;
    }

    @SuppressWarnings("unchecked")
    public boolean hasBlockerError(ParsingResult<ArchiveRoot> parsingResult) {
        for (ParsingError error : parsingResult.getContext().getParsingErrors()) {
            if (ParsingErrorLevel.ERROR.equals(error.getErrorLevel())) {
                return true;
            }
        }

        for (ParsingResult<?> childResult : parsingResult.getContext().getSubResults()) {
            if (hasBlockerError((ParsingResult<ArchiveRoot>) childResult)) {
                return true;
            }
        }

        return false;
    }
}