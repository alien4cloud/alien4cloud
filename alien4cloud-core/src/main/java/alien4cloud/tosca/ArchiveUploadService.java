package alien4cloud.tosca;

import java.nio.file.Path;
import java.util.Map;
import java.util.Set;

import javax.inject.Inject;

import lombok.extern.slf4j.Slf4j;

import org.springframework.stereotype.Component;

import alien4cloud.component.repository.exception.CSARVersionAlreadyExistsException;
import alien4cloud.model.components.CSARDependency;
import alien4cloud.model.components.Csar;
import alien4cloud.model.git.CsarDependenciesBean;
import alien4cloud.security.AuthorizationUtil;
import alien4cloud.security.model.Role;
import alien4cloud.suggestions.services.SuggestionService;
import alien4cloud.topology.TopologyServiceCore;
import alien4cloud.topology.TopologyTemplateVersionService;
import alien4cloud.tosca.model.ArchiveRoot;
import alien4cloud.tosca.parser.ParsingContext;
import alien4cloud.tosca.parser.ParsingError;
import alien4cloud.tosca.parser.ParsingErrorLevel;
import alien4cloud.tosca.parser.ParsingException;
import alien4cloud.tosca.parser.ParsingResult;

import com.google.common.collect.Maps;

@Component
@Slf4j
public class ArchiveUploadService {

    @Inject
    private ArchiveParser parser;
    @Inject
    private ArchiveIndexer archiveIndexer;
    @Inject
    TopologyServiceCore topologyServiceCore;
    @Inject
    TopologyTemplateVersionService topologyTemplateVersionService;
    @Inject
    private SuggestionService suggestionService;

    /**
     * Upload a TOSCA archive and index its components.
     * 
     * @param path The archive path.
     * @return The Csar object from the parsing.
     * @throws ParsingException
     * @throws CSARVersionAlreadyExistsException
     */
    public ParsingResult<Csar> upload(Path path) throws ParsingException, CSARVersionAlreadyExistsException {
        // parse the archive.
        ParsingResult<ArchiveRoot> parsingResult = parser.parse(path);

        final ArchiveRoot archiveRoot = parsingResult.getResult();
        if (archiveRoot.hasToscaTopologyTemplate()) {
            AuthorizationUtil.checkHasOneRoleIn(Role.ARCHITECT, Role.ADMIN);
        }
        if (archiveRoot.hasToscaTypes()) {
            AuthorizationUtil.checkHasOneRoleIn(Role.COMPONENTS_MANAGER, Role.ADMIN);
        }

        if (ArchiveUploadService.hasError(parsingResult, null)) {
            // check if any blocker error has been found during parsing process.
            if (ArchiveUploadService.hasError(parsingResult, ParsingErrorLevel.ERROR)) {
                // do not save anything if any blocker error has been found during import.
                return toSimpleResult(parsingResult);
            }
        }
        archiveIndexer.importArchive(archiveRoot, path, parsingResult.getContext().getParsingErrors());
        try {
            suggestionService.postProcessSuggestionFromArchive(parsingResult);
            suggestionService.setAllSuggestionIdOnPropertyDefinition();
        } catch (Exception e) {
            log.error("Could not post process suggestion for the archive", e);
        }
        return toSimpleResult(parsingResult);
    }

    public Map<CSARDependency, CsarDependenciesBean> preParsing(Set<Path> paths) throws ParsingException {
        Map<CSARDependency, CsarDependenciesBean> csarDependenciesBeans = Maps.newHashMap();
        for (Path path : paths) {
            try {
                ParsingResult<ArchiveRoot> parsingResult = parser.parse(path);
                CsarDependenciesBean csarDepContainer = new CsarDependenciesBean();
                csarDepContainer.setPath(path);
                csarDepContainer.setSelf(new CSARDependency(parsingResult.getResult().getArchive().getName(), parsingResult.getResult().getArchive()
                        .getVersion()));
                csarDepContainer.setDependencies(parsingResult.getResult().getArchive().getDependencies());
                csarDependenciesBeans.put(csarDepContainer.getSelf(), csarDepContainer);
            } catch (Exception e) {
                // TODO: error should be returned in a way or another
                log.debug("Not able to parse archive, ignoring it", e);
            }
        }
        return csarDependenciesBeans;
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