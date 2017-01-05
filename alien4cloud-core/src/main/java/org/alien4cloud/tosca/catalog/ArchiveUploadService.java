package org.alien4cloud.tosca.catalog;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.inject.Inject;

import org.alien4cloud.tosca.catalog.index.ArchiveIndexer;
import org.alien4cloud.tosca.model.CSARDependency;
import org.alien4cloud.tosca.model.Csar;
import org.alien4cloud.tosca.model.CsarDependenciesBean;
import org.springframework.stereotype.Component;

import com.google.common.collect.Maps;

import alien4cloud.component.repository.exception.CSARUsedInActiveDeployment;
import alien4cloud.component.repository.exception.ToscaTypeAlreadyDefinedInOtherCSAR;
import alien4cloud.model.components.CSARSource;
import alien4cloud.suggestions.services.SuggestionService;
import alien4cloud.tosca.context.ToscaContextual;
import alien4cloud.tosca.model.ArchiveRoot;
import alien4cloud.tosca.parser.ParsingContext;
import alien4cloud.tosca.parser.ParsingErrorLevel;
import alien4cloud.tosca.parser.ParsingException;
import alien4cloud.tosca.parser.ParsingResult;
import lombok.extern.slf4j.Slf4j;

@Component
@Slf4j
public class ArchiveUploadService {
    @Inject
    private ArchiveParser parser;
    @Inject
    private ArchiveIndexer archiveIndexer;
    @Inject
    private SuggestionService suggestionService;

    /**
     * Upload a TOSCA archive and index its components.
     * 
     * @param path The archive path.
     * @param csarSource The source of the upload.
     * @return The Csar object from the parsing.
     * @throws ParsingException
     * @throws CSARUsedInActiveDeployment
     */
    @ToscaContextual
    public ParsingResult<Csar> upload(Path path, CSARSource csarSource, String workspace)
            throws ParsingException, CSARUsedInActiveDeployment, ToscaTypeAlreadyDefinedInOtherCSAR {
        // parse the archive.
        ParsingResult<ArchiveRoot> parsingResult = parser.parseWithExistingContext(path, workspace);

        final ArchiveRoot archiveRoot = parsingResult.getResult();

        // check if any blocker error has been found during parsing process.
        if (parsingResult.hasError(ParsingErrorLevel.ERROR)) {
            // do not save anything if any blocker error has been found during import.
            return ArchiveParserUtil.toSimpleResult(parsingResult);
        }

        archiveIndexer.importArchive(archiveRoot, csarSource, path, parsingResult.getContext().getParsingErrors());
        try {
            suggestionService.postProcessSuggestionFromArchive(parsingResult);
            suggestionService.setAllSuggestionIdOnPropertyDefinition();
        } catch (Exception e) {
            log.error("Could not post process suggestion for the archive <" + archiveRoot.getArchive().getName() + "/" + archiveRoot.getArchive().getVersion()
                    + ">", e);
        }
        return ArchiveParserUtil.toSimpleResult(parsingResult);
    }

    @ToscaContextual
    public Map<CSARDependency, CsarDependenciesBean> preParsing(Set<Path> paths, List<ParsingResult<Csar>> parsingResults) {
        Map<CSARDependency, CsarDependenciesBean> csarDependenciesBeans = Maps.newHashMap();
        for (Path path : paths) {
            try {
                // FIXME cleanup git import archives
                ParsingResult<CsarDependenciesBean> parsingResult = parser.parseImports(path);
                parsingResult.getResult().setPath(path);
                csarDependenciesBeans.put(parsingResult.getResult().getSelf(), parsingResult.getResult());
            } catch (ParsingException e) {
                ParsingResult<Csar> failedResult = new ParsingResult<>();
                failedResult.setContext(new ParsingContext(path.getFileName().toString()));
                failedResult.getContext().setParsingErrors(e.getParsingErrors());
                parsingResults.add(failedResult);
                log.debug("Not able to parse archive, ignoring it", e);
            }
        }
        return csarDependenciesBeans;
    }
}