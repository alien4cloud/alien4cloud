package alien4cloud.tosca;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import javax.annotation.Resource;

import org.springframework.stereotype.Component;

import alien4cloud.component.ICSARRepositorySearchService;
import alien4cloud.component.repository.ICsarRepositry;
import alien4cloud.component.repository.exception.CSARVersionAlreadyExistsException;
import alien4cloud.csar.services.CsarService;
import alien4cloud.model.components.CSARDependency;
import alien4cloud.model.components.Csar;
import alien4cloud.model.components.IndexedToscaElement;
import alien4cloud.model.templates.TopologyTemplate;
import alien4cloud.model.templates.TopologyTemplateVersion;
import alien4cloud.model.topology.Topology;
import alien4cloud.paas.wf.WorkflowsBuilderService;
import alien4cloud.paas.wf.WorkflowsBuilderService.TopologyContext;
import alien4cloud.security.AuthorizationUtil;
import alien4cloud.security.model.CsarDependenciesBean;
import alien4cloud.security.model.Role;
import alien4cloud.topology.TopologyServiceCore;
import alien4cloud.topology.TopologyTemplateVersionService;
import alien4cloud.tosca.model.ArchiveRoot;
import alien4cloud.tosca.parser.ParsingContext;
import alien4cloud.tosca.parser.ParsingError;
import alien4cloud.tosca.parser.ParsingErrorLevel;
import alien4cloud.tosca.parser.ParsingException;
import alien4cloud.tosca.parser.ParsingResult;
import alien4cloud.tosca.parser.ToscaCsarDependenciesParser;
import alien4cloud.tosca.parser.ToscaParsingUtil;
import alien4cloud.tosca.parser.impl.ErrorCode;
import alien4cloud.utils.VersionUtil;

@Component
public class ArchiveUploadService {

    @Resource
    private ArchiveParser parser;
    @Resource
    private ToscaCsarDependenciesParser dependenciesParser;
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
    @Resource
    private TopologyServiceCore topologyServiceCore;
    @Resource
    private TopologyTemplateVersionService topologyTemplateVersionService;
    @Resource
    private WorkflowsBuilderService workflowBuilderService;
    @Resource
    private ICSARRepositorySearchService searchService;

    /**
     * Upload a TOSCA archive and index its components.
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

        final ArchiveRoot archiveRoot = parsingResult.getResult();
        if (archiveRoot.hasToscaTopologyTemplate()) {
            AuthorizationUtil.checkHasOneRoleIn(Role.ARCHITECT, Role.ADMIN);
        }
        if (archiveRoot.hasToscaTypes()) {
            AuthorizationUtil.checkHasOneRoleIn(Role.COMPONENTS_MANAGER, Role.ADMIN);
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

        // if a topology has been added we want to notify the user
        if (parsingResult.getResult().getTopology() != null && !parsingResult.getResult().getTopology().isEmpty()) {
            final Topology topology = parsingResult.getResult().getTopology();
            if (archiveRoot.hasToscaTypes()) {
                // the archive contains types
                // we assume those types are used in the embedded topology
                // so we add the dependency to this CSAR
                CSARDependency selfDependency = new CSARDependency(archiveRoot.getArchive().getName(), archiveRoot.getArchive().getVersion());
                topology.getDependencies().add(selfDependency);
            }

            // init the workflows
            TopologyContext topologyContext = workflowBuilderService.buildCachedTopologyContext(new TopologyContext() {
                @Override
                public Topology getTopology() {
                    return topology;
                }

                @Override
                public <T extends IndexedToscaElement> T findElement(Class<T> clazz, String id) {
                    return ToscaParsingUtil.getElementFromArchiveOrDependencies(clazz, id, archiveRoot, searchService);
                }
            });
            workflowBuilderService.initWorkflows(topologyContext);

            // TODO: here we should update the topology if it already exists
            // TODO: the name should only contains the archiveName
            TopologyTemplate existingTemplate = topologyServiceCore.searchTopologyTemplateByName(archiveRoot.getArchive().getName());
            if (existingTemplate != null) {
                // the topology template already exists
                topology.setDelegateId(existingTemplate.getId());
                topology.setDelegateType(TopologyTemplate.class.getSimpleName().toLowerCase());
                String topologyId = topologyServiceCore.saveTopology(topology);
                // now search the version
                TopologyTemplateVersion ttv = topologyTemplateVersionService.searchByDelegateAndVersion(existingTemplate.getId(), archiveVersion);
                if (ttv != null) {
                    // the version exists, we will update it's topology id and delete the old topology
                    topologyTemplateVersionService.changeTopology(ttv, topologyId);
                } else {
                    // we just create a new version
                    topologyTemplateVersionService.createVersion(existingTemplate.getId(), null, archiveVersion, null, topology);
                }
                simpleResult
                        .getContext()
                        .getParsingErrors()
                        .add(new ParsingError(ParsingErrorLevel.INFO, ErrorCode.TOPOLOGY_UPDATED, "", null, "A topology template has been detected", null,
                                archiveName));

            } else {
                simpleResult
                        .getContext()
                        .getParsingErrors()
                        .add(new ParsingError(ParsingErrorLevel.INFO, ErrorCode.TOPOLOGY_DETECTED, "", null, "A topology template has been detected", null,
                                archiveName));
                topologyServiceCore.createTopologyTemplate(topology, archiveName, parsingResult.getResult().getTopologyTemplateDescription(), archiveVersion);
            }
            topologyServiceCore.updateSubstitutionType(topology);
        }

        return simpleResult;
    }

    public ArrayList<CsarDependenciesBean> preParsing(List<Path> paths) throws ParsingException {
       
        ArrayList<CsarDependenciesBean> listCsarDependenciesBean = new ArrayList<CsarDependenciesBean>();
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