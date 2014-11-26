package alien4cloud.tosca;

import java.nio.file.Path;

import javax.annotation.Resource;

import org.springframework.stereotype.Component;

import alien4cloud.component.repository.ICsarRepositry;
import alien4cloud.component.repository.exception.CSARVersionAlreadyExistsException;
import alien4cloud.csar.services.CsarService;
import alien4cloud.tosca.model.ArchiveRoot;
import alien4cloud.tosca.model.Csar;
import alien4cloud.tosca.parser.ParsingContext;
import alien4cloud.tosca.parser.ParsingError;
import alien4cloud.tosca.parser.ParsingErrorLevel;
import alien4cloud.tosca.parser.ParsingException;
import alien4cloud.tosca.parser.ParsingResult;
import alien4cloud.utils.VersionUtil;

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
    private ArchiveIndexer archiveIndexer;

    /**
     * Upload a TOSCA archive and index it's components.
     * 
     * @param path The archive path.
     * @return The Csar object from the parsing.
     * @throws ParsingException
     * @throws CSARVersionAlreadyExistsException
     */
    public ParsingResult<Csar> upload(Path path) throws ParsingException, CSARVersionAlreadyExistsException {
        // TODO issue tolerance should depends of the version (SNAPSHOT) ?

        // parse the archive.
        ParsingResult<ArchiveRoot> parsingResult = parser.parse(path);
        postProcessor.postProcess(parsingResult);

        String archiveName = parsingResult.getResult().getArchive().getName();
        String archiveVersion = parsingResult.getResult().getArchive().getVersion();

        // check if the archive already exists
        Csar archive = csarService.getIfExists(archiveName, archiveVersion);
        if (archive != null) {
            if (!VersionUtil.isSnapshot(archive.getVersion())) {
                // Cannot override RELEASED CSAR .
                throw new CSARVersionAlreadyExistsException("CSAR: " + archiveName + ", Version: " + archiveVersion + " already exists in the repository.");
            }
        }

        ParsingResult<Csar> simpleResult = toSimpleResult(parsingResult);

        if (ArchiveUploadService.hasError(parsingResult, null)) {
            // save the parsing results so users can keep track of the warnings or infos from parsing.
            archiveRepositry.storeParsingResults(archiveName, archiveVersion, simpleResult);

            // check if any blocker error has been found during parsing process.
            if (ArchiveUploadService.hasError(parsingResult, ParsingErrorLevel.ERROR)) {
                // do not save anything if any blocker error has been found during import.
                return toSimpleResult(parsingResult);
            }
        }

        // save the archive (before we index and save other data so we can cleanup if anything goes wrong).
        csarService.save(parsingResult.getResult().getArchive());
        // save the archive in the repository
        archiveRepositry.storeCSAR(archiveName, archiveVersion, path);
        // manage images before archive storage in the repository
        imageLoader.importImages(path, parsingResult);
        // index the archive content in elastic-search
        archiveIndexer.indexArchive(archiveName, archiveVersion, parsingResult.getResult(), archive != null);

        return simpleResult;
    }

    /**
     * Create a simple result without all the parsed data but just the {@link Csar} object as well as the eventual errors.
     * 
     * @return The simple result out of the complex parsing result.
     */
    private ParsingResult<Csar> toSimpleResult(ParsingResult<ArchiveRoot> parsingResult) {
        ParsingResult<Csar> simpleResult = this.<Csar> cleanup(parsingResult);
        simpleResult.setResult(parsingResult.getResult().getArchive());
        return simpleResult;
    }

    private <T> ParsingResult<T> cleanup(ParsingResult<?> result) {
        ParsingContext context = new ParsingContext(result.getContext().getFileName());
        context.getParsingErrors().addAll(result.getContext().getParsingErrors());
        for (ParsingResult<?> subResult : result.getContext().getSubResults()) {
            context.getSubResults().add(cleanup(subResult));
        }
        ParsingResult<T> cleaned = new ParsingResult<T>(null, context);
        return cleaned;
    }

    /**
     * Checks if a given parsing result has any error.
     * 
     * @param parsingResult The parsing result to check.
     * @param level The level of error to check. Null means any levels.
     * @return true if the parsing result as at least one error of the requested level.
     */
    public static boolean hasError(ParsingResult<?> parsingResult, ParsingErrorLevel level) {
        for (ParsingError error : parsingResult.getContext().getParsingErrors()) {
            if (level == null || level.equals(error.getErrorLevel())) {
                return true;
            }
        }

        for (ParsingResult<?> childResult : parsingResult.getContext().getSubResults()) {
            if (hasError((ParsingResult<?>) childResult, level)) {
                return true;
            }
        }
        return false;
    }
}