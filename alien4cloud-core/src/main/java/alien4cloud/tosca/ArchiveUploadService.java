package alien4cloud.tosca;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import javax.inject.Inject;

import org.springframework.stereotype.Component;

import alien4cloud.component.repository.ICsarRepositry;
import alien4cloud.component.repository.exception.CSARVersionAlreadyExistsException;
import alien4cloud.model.components.Csar;
import alien4cloud.security.AuthorizationUtil;
import alien4cloud.security.model.CsarDependenciesBean;
import alien4cloud.security.model.Role;
import alien4cloud.tosca.model.ArchiveRoot;
import alien4cloud.tosca.parser.*;

@Component
public class ArchiveUploadService {

    @Inject
    private ArchiveParser parser;
    @Inject
    private ToscaCsarDependenciesParser dependenciesParser;
    @Inject
    private ArchivePostProcessor postProcessor;
    @Inject
    private ArchiveIndexer archiveIndexer;
    @Inject
    private ICsarRepositry archiveRepositry;

    /**
     * Upload a TOSCA archive and index it's components.
     * 
     * @param path The archive path.
     * @return The Csar object from the parsing.
     * @throws ParsingException
     * @throws CSARVersionAlreadyExistsException
     */
    public ParsingResult<Csar> upload(Path path) throws ParsingException, CSARVersionAlreadyExistsException {
        // parse the archive.
        ParsingResult<ArchiveRoot> parsingResult = parser.parse(path);
        postProcessor.postProcess(parsingResult);

        String archiveName = parsingResult.getResult().getArchive().getName();
        String archiveVersion = parsingResult.getResult().getArchive().getVersion();

        ArchiveRoot archiveRoot = parsingResult.getResult();
        if (archiveRoot.hasToscaTopologyTemplate()) {
            AuthorizationUtil.checkHasOneRoleIn(Role.ARCHITECT, Role.ADMIN);
        }
        if (archiveRoot.hasToscaTypes()) {
            AuthorizationUtil.checkHasOneRoleIn(Role.COMPONENTS_MANAGER, Role.ADMIN);
        }

        ParsingResult<Csar> simpleResult = toSimpleResult(parsingResult);

        if (ArchiveUploadService.hasError(parsingResult, null)) {
            // check if any blocker error has been found during parsing process.
            if (ArchiveUploadService.hasError(parsingResult, ParsingErrorLevel.ERROR)) {
                // do not save anything if any blocker error has been found during import.
                return toSimpleResult(parsingResult);
            } else {
                // save the parsing results so users can keep track of the warnings or infos from parsing.
                archiveRepositry.storeParsingResults(archiveName, archiveVersion, simpleResult);
            }
        }

        archiveIndexer.importArchive(archiveRoot, path, parsingResult.getContext().getParsingErrors());

        return simpleResult;
    }

    public List<CsarDependenciesBean> preParsing(Set<Path> paths) throws ParsingException {
        List<CsarDependenciesBean> listCsarDependenciesBean = new ArrayList<CsarDependenciesBean>();
        for (Path path : paths) {
            CsarDependenciesBean csarDepContainer = new CsarDependenciesBean();
            ParsingResult<ArchiveRoot> parsingResult = parser.parse(path);
            postProcessor.postProcess(parsingResult);
            csarDepContainer.setName(parsingResult.getResult().getArchive().getName());
            csarDepContainer.setVersion(parsingResult.getResult().getArchive().getVersion());
            csarDepContainer.setPath(path);
            if (parsingResult.getResult().getArchive().getDependencies() != null) {
                if (!parsingResult.getResult().getArchive().getDependencies().isEmpty() || parsingResult.getResult().getArchive().getDependencies() != null) {
                    csarDepContainer.setDependencies(parsingResult.getResult().getArchive().getDependencies());
                }
            }
            listCsarDependenciesBean.add(csarDepContainer);
        }
        return listCsarDependenciesBean;
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

        return false;
    }
}