package alien4cloud.rest.csar;

import java.io.IOException;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import javax.annotation.Resource;
import javax.validation.Valid;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import org.elasticsearch.common.collect.Maps;
import org.elasticsearch.common.collect.Sets;
import org.elasticsearch.index.query.QueryBuilders;
import org.springframework.beans.factory.annotation.Required;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import alien4cloud.application.ApplicationEnvironmentService;
import alien4cloud.application.ApplicationService;
import alien4cloud.application.DeploymentSetupService;
import alien4cloud.application.InvalidDeploymentSetupException;
import alien4cloud.audit.annotation.Audit;
import alien4cloud.cloud.CloudResourceMatcherService;
import alien4cloud.cloud.CloudService;
import alien4cloud.cloud.DeploymentService;
import alien4cloud.component.ICSARRepositoryIndexerService;
import alien4cloud.component.repository.CsarFileRepository;
import alien4cloud.component.repository.exception.CSARVersionAlreadyExistsException;
import alien4cloud.component.repository.exception.CSARVersionNotFoundException;
import alien4cloud.csar.services.CsarService;
import alien4cloud.dao.IGenericSearchDAO;
import alien4cloud.dao.model.FacetedSearchResult;
import alien4cloud.exception.AlreadyExistException;
import alien4cloud.exception.DeleteReferencedObjectException;
import alien4cloud.exception.NotFoundException;
import alien4cloud.model.application.Application;
import alien4cloud.model.application.DeploymentSetup;
import alien4cloud.model.application.DeploymentSetupMatchInfo;
import alien4cloud.model.cloud.Cloud;
import alien4cloud.model.components.CSARDependency;
import alien4cloud.model.components.Csar;
import alien4cloud.model.components.IndexedNodeType;
import alien4cloud.model.deployment.Deployment;
import alien4cloud.model.templates.TopologyTemplate;
import alien4cloud.model.topology.Topology;
import alien4cloud.paas.exception.CloudDisabledException;
import alien4cloud.rest.component.SearchRequest;
import alien4cloud.rest.model.RestError;
import alien4cloud.rest.model.RestErrorBuilder;
import alien4cloud.rest.model.RestErrorCode;
import alien4cloud.rest.model.RestResponse;
import alien4cloud.rest.model.RestResponseBuilder;
import alien4cloud.rest.topology.CsarRelatedResourceDTO;
import alien4cloud.security.AuthorizationUtil;
import alien4cloud.security.model.CloudRole;
import alien4cloud.topology.TopologyService;
import alien4cloud.tosca.ArchiveUploadService;
import alien4cloud.tosca.parser.ParsingError;
import alien4cloud.tosca.parser.ParsingErrorLevel;
import alien4cloud.tosca.parser.ParsingException;
import alien4cloud.tosca.parser.ParsingResult;
import alien4cloud.tosca.parser.impl.ErrorCode;
import alien4cloud.utils.FileUploadUtil;
import alien4cloud.utils.FileUtil;
import alien4cloud.utils.VersionUtil;
import alien4cloud.utils.YamlParserUtil;

import com.google.common.collect.Lists;
import com.mangofactory.swagger.annotations.ApiIgnore;
import com.wordnik.swagger.annotations.ApiOperation;

@RestController
@RequestMapping("/rest/csars")
@Slf4j
public class CloudServiceArchiveController {
    private static final String DEFAULT_TEST_FOLDER = "test";
    private static final String CSAR_TYPE_NAME = "csar";

    @Resource
    private ArchiveUploadService csarUploadService;
    @Resource(name = "alien-es-dao")
    private IGenericSearchDAO csarDAO;
    @Resource
    private DeploymentService deploymentService;
    @Resource
    private ICSARRepositoryIndexerService indexerService;
    @Resource
    private CsarFileRepository alienRepository;
    private Path tempDirPath;
    @Resource
    private CsarService csarService;
    @Resource
    private TopologyService topologyService;
    @Resource
    private CloudService cloudService;
    @Resource
    private CloudResourceMatcherService cloudResourceMatcherService;
    @Resource
    private DeploymentSetupService deploymentSetupService;
    @Resource
    private ApplicationService applicationService;
    @Resource
    private ApplicationEnvironmentService applicationEnvironmentService;

    @ApiOperation(value = "Upload a csar zip file.")
    @RequestMapping(method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
    @Audit
    public RestResponse<CsarUploadResult> uploadCSAR(@RequestParam("file") MultipartFile csar) throws IOException {
        Path csarPath = null;
        try {
            log.info("Serving file upload with name [" + csar.getOriginalFilename() + "]");
            csarPath = Files.createTempFile(tempDirPath, "", '.' + CsarFileRepository.CSAR_EXTENSION);
            // save the archive in the temp directory
            FileUploadUtil.safeTransferTo(csarPath, csar);
            // load, parse the archive definitions and save on disk
            ParsingResult<Csar> result = csarUploadService.upload(csarPath);
            RestError error = null;
            if (ArchiveUploadService.hasError(result, ParsingErrorLevel.ERROR)) {
                error = RestErrorBuilder.builder(RestErrorCode.CSAR_PARSING_ERROR).build();
            }
            return RestResponseBuilder.<CsarUploadResult> builder().error(error).data(toUploadResult(result)).build();
        } catch (ParsingException e) {
            log.error("Error happened while parsing csar file <" + e.getFileName() + ">", e);
            String fileName = e.getFileName() == null ? csar.getOriginalFilename() : e.getFileName();

            CsarUploadResult uploadResult = new CsarUploadResult();
            uploadResult.getErrors().put(fileName, e.getParsingErrors());
            return RestResponseBuilder.<CsarUploadResult> builder().error(RestErrorBuilder.builder(RestErrorCode.CSAR_INVALID_ERROR).build())
                    .data(uploadResult).build();
        } catch (CSARVersionAlreadyExistsException e) {
            log.error("A CSAR with the same name and the same version already existed in the repository", e);
            CsarUploadResult uploadResult = new CsarUploadResult();
            uploadResult.getErrors().put(
                    csar.getOriginalFilename(),
                    Lists.newArrayList(new ParsingError(ErrorCode.CSAR_ALREADY_EXISTS, "CSAR already exists", null,
                            "Unable to override an existing CSAR if the version is not a SNAPSHOT version.", null, null)));
            return RestResponseBuilder.<CsarUploadResult> builder().error(RestErrorBuilder.builder(RestErrorCode.ALREADY_EXIST_ERROR).build())
                    .data(uploadResult).build();
        } finally {
            if (csarPath != null) {
                // Clean up
                try {
                    FileUtil.delete(csarPath);
                } catch (IOException e) {
                    // The repository might just move the file instead of copying to save IO disk access
                }
            }
        }
    }

    private CsarUploadResult toUploadResult(ParsingResult<Csar> result) {
        CsarUploadResult uploadResult = new CsarUploadResult();
        uploadResult.setCsar(result.getResult());
        addAllSubResultErrors(result, uploadResult);
        return uploadResult;
    }

    private void addAllSubResultErrors(ParsingResult<?> result, CsarUploadResult uploadResult) {
        if (result.getContext().getParsingErrors() != null && !result.getContext().getParsingErrors().isEmpty()) {
            uploadResult.getErrors().put(result.getContext().getFileName(), result.getContext().getParsingErrors());
        }
        for (ParsingResult<?> subResult : result.getContext().getSubResults()) {
            addAllSubResultErrors(subResult, uploadResult);
        }
    }

    /**
     * Create a CSAR in SNAPSHOT version
     *
     * @param request
     * @return
     */
    @ApiOperation(value = "Create a CSAR in SNAPSHOT version.")
    @RequestMapping(method = RequestMethod.POST, consumes = MediaType.APPLICATION_JSON_VALUE)
    @ResponseStatus(value = HttpStatus.CREATED)
    @Audit
    public RestResponse<String> createSnapshot(@Valid @RequestBody CreateCsarRequest request) {
        // new csar instance to save
        Csar csar = new Csar();
        csar.setName(request.getName());
        csar.setDescription(request.getDescription());

        String version = request.getVersion().endsWith("-SNAPSHOT") ? request.getVersion() : request.getVersion() + "-SNAPSHOT";
        csar.setVersion(version);

        if (csarDAO.count(Csar.class, QueryBuilders.termQuery("id", csar.getId())) > 0) {
            log.debug("Create csar <{}> impossible (already exists)", csar.getId());
            // an csar already exist with the given name.
            throw new AlreadyExistException("An csar with the given name already exists.");
        } else {
            log.debug("Create csar <{}>", csar.getId());
        }
        csarDAO.save(csar);
        return RestResponseBuilder.<String> builder().data(csar.getId()).build();
    }

    @ApiOperation(value = "Add dependency to the csar with given id.")
    @RequestMapping(value = "/{csarId:.+?}/dependencies", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
    @Audit
    public RestResponse<Boolean> addDependency(@PathVariable String csarId, @Valid @RequestBody CSARDependency dependency) {
        Csar csar = csarDAO.findById(Csar.class, csarId);
        if (csar == null) {
            throw new NotFoundException("Cannot add dependency to csar [" + csarId + "] as it cannot be found");
        }
        Set<CSARDependency> existingDependencies = csar.getDependencies();
        if (existingDependencies == null) {
            existingDependencies = Sets.newHashSet();
            csar.setDependencies(existingDependencies);
        }
        boolean couldBeSaved = existingDependencies.add(dependency);
        csarDAO.save(csar);
        return RestResponseBuilder.<Boolean> builder().data(couldBeSaved).build();
    }

    @ApiOperation(value = "Delete a CSAR given its id.")
    @RequestMapping(value = "/{csarId:.+?}", method = RequestMethod.DELETE, produces = MediaType.APPLICATION_JSON_VALUE)
    @Audit
    public RestResponse<Void> delete(@PathVariable String csarId) {
        boolean isCsarDeletable = true;
        Csar csar = csarService.getMandatoryCsar(csarId);
        List<Object> relatedResourceList = Lists.newArrayList();

        // a csar that is a dependency of another csar can not be deleted
        Csar[] relatedCsars = csarService.getDependantCsars(csar.getName(), csar.getVersion());
        if (relatedCsars != null && relatedCsars.length > 0) {
            isCsarDeletable = false;
            relatedResourceList.addAll(generateCsarsInfo(relatedCsars));
        }

        // check if some of the nodes are used in topologies.
        Topology[] topologies = csarService.getDependantTopologies(csar.getName(), csar.getVersion());
        if (topologies != null && topologies.length > 0) {
            isCsarDeletable = false;
            relatedResourceList.addAll(generateTopologiesInfo(topologies));
        }

        if (!isCsarDeletable) {
            throw new DeleteReferencedObjectException("The csar named <" + csar.getName() + "> in version <" + csar.getVersion()
                    + "> can not be deleted since it is referenced by other resources", relatedResourceList);
        }

        // latest version indicator will be recomputed to match this new reality
        indexerService.deleteElements(csar.getName(), csar.getVersion());

        csarDAO.delete(Csar.class, csarId);

        // physically delete files
        alienRepository.removeCSAR(csar.getName(), csar.getVersion());
        return RestResponseBuilder.<Void> builder().build();
    }

    /**
     * Generate resources related to a csar list
     * 
     * @param csars
     * @return
     */
    private List<CsarRelatedResourceDTO> generateCsarsInfo(Csar[] csars) {
        String resourceName = null;
        String resourceId = null;
        List<CsarRelatedResourceDTO> resourceList = Lists.newArrayList();
        for (Csar csar : csars) {
            resourceName = csar.getName();
            resourceId = csar.getId();
            CsarRelatedResourceDTO temp = new CsarRelatedResourceDTO(resourceName, CSAR_TYPE_NAME, resourceId);
            resourceList.add(temp);
        }
        return resourceList;
    }

    /**
     * Generate resources (application or template) related to a topology list
     * 
     * @param topologies
     * @return
     */
    private List<CsarRelatedResourceDTO> generateTopologiesInfo(Topology[] topologies) {

        String resourceName = null;
        String resourceId = null;
        boolean typeHandled = true;
        List<CsarRelatedResourceDTO> resourceList = Lists.newArrayList();
        for (Topology topology : topologies) {
            String delegateType = topology.getDelegateType();
            if (delegateType.equals("application")) {
                // get the related application
                Application application = applicationService.checkAndGetApplication(topology.getDelegateId());
                resourceName = application.getName();
            } else if (delegateType.equals("topologytemplate")) {
                // get the related template
                TopologyTemplate template = topologyService.getOrFailTopologyTemplate(topology.getDelegateId());
                resourceName = template.getName();
            } else {
                typeHandled = false;
                log.info("The topology <" + topology.getId() + "> of type <" + delegateType + " is not yet handled.");
            }

            if (typeHandled) {
                resourceId = topology.getDelegateId();
                CsarRelatedResourceDTO temp = new CsarRelatedResourceDTO(resourceName, delegateType, resourceId);
                resourceList.add(temp);
            } else {
                typeHandled = true;
            }
        }

        return resourceList;
    }

    @ApiOperation(value = "Get a CSAR given its id.", notes = "Returns a CSAR.")
    @RequestMapping(value = "/{csarId:.+?}", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    public RestResponse<Csar> read(@PathVariable String csarId) {
        Csar data = csarDAO.findById(Csar.class, csarId);
        return RestResponseBuilder.<Csar> builder().data(data).build();
    }

    @ApiOperation(value = "Search for cloud service archives.")
    @RequestMapping(value = "/search", method = RequestMethod.POST, consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public RestResponse<FacetedSearchResult> search(@RequestBody SearchRequest searchRequest) {
        Map<String, String[]> filters = searchRequest.getFilters();
        if (filters == null) {
            filters = Maps.newHashMap();
        }
        FacetedSearchResult searchResult = csarDAO.facetedSearch(Csar.class, searchRequest.getQuery(), filters, null, searchRequest.getFrom(),
                searchRequest.getSize());
        return RestResponseBuilder.<FacetedSearchResult> builder().data(searchResult).build();
    }

    @ApiIgnore
    // @ApiOperation(value = "Create or update a node type in the given cloud service archive.")
    @RequestMapping(value = "/{csarId:.+?}/nodetypes", method = RequestMethod.POST, consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    @Audit
    public RestResponse<Void> saveNodeType(@PathVariable String csarId, @RequestBody IndexedNodeType nodeType) {
        Csar csar = csarService.getMandatoryCsar(csarId);
        // check that the csar version is snapshot.
        if (VersionUtil.isSnapshot(csar.getVersion())) {
            nodeType.setArchiveName(csar.getName());
            nodeType.setArchiveVersion(csar.getVersion());
            indexerService.indexInheritableElement(csar.getName(), csar.getVersion(), nodeType, csar.getDependencies());
            return RestResponseBuilder.<Void> builder().build();
        }
        return RestResponseBuilder.<Void> builder().error(RestErrorBuilder.builder(RestErrorCode.CSAR_RELEASE_IMMUTABLE).build()).build();
    }

    @Required
    @Value("${directories.alien}/${directories.upload_temp}")
    public void setTempDirPath(String tempDirPath) throws IOException {
        this.tempDirPath = FileUtil.createDirectoryIfNotExists(tempDirPath);
        log.info("Temporary folder for upload was set to [" + this.tempDirPath + "]");
    }

    private static class YamlTestFileVisitor extends SimpleFileVisitor<Path> {
        @Getter
        private Path yamlTestFile;

        @Override
        public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
            if (file.getFileName().toString().endsWith(".yaml")) {
                yamlTestFile = file;
            }
            return FileVisitResult.TERMINATE;
        }
    }

    /**
     * Get only the active deployment for the given application on the given cloud
     *
     * @param csarId id of the topology
     * @return the active deployment
     */
    @ApiOperation(value = "Get active deployment for the given csar snapshot's test topology on the given cloud.", notes = "Application role required [ APPLICATION_MANAGER | APPLICATION_DEVOPS ]")
    @RequestMapping(value = "/{csarId:.+?}/active-deployment", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    public RestResponse<Deployment> getActiveDeployment(@PathVariable String csarId) {
        Csar csar = csarService.getMandatoryCsar(csarId);
        if (csar.getTopologyId() == null || csar.getCloudId() == null) {
            return RestResponseBuilder.<Deployment> builder().build();
        }
        Deployment deployment = deploymentService.getActiveDeployment(csar.getCloudId(), csar.getTopologyId());
        return RestResponseBuilder.<Deployment> builder().data(deployment).build();
    }

    @ApiOperation(value = "Deploy snapshot archive on a given cloud.")
    @RequestMapping(value = "/{csarName:.+?}/version/{csarVersion:.+?}/cloudid/{cloudId:.+?}", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    @Audit
    public RestResponse<String> deploySnapshot(@PathVariable String csarName, @PathVariable String csarVersion, @PathVariable String cloudId)
            throws CSARVersionNotFoundException, IOException {
        Cloud cloud = cloudService.getMandatoryCloud(cloudId);
        AuthorizationUtil.checkAuthorizationForCloud(cloud, CloudRole.values());

        String archiveId = csarName + ":" + csarVersion;
        Csar csar = csarDAO.findById(Csar.class, archiveId);
        String deploymentId = null;
        if (csar == null) {
            throw new NotFoundException("Cannot find the CSAR with name [" + csarName + "] and version [" + csarVersion + "]");
        }

        if (deploymentService.getActiveDeployment(csar.getTopologyId(), csar.getCloudId()) != null) {
            throw new AlreadyExistException("You cannot have more than a single deployment for a given CSAR.");
        }

        String version = csar.getVersion();
        if (!version.contains("-SNAPSHOT")) {
            throw new NotFoundException("Csar with id [" + csarName + "] is not in SNAPSHOT");
        } else {
            try {
                // load topology yaml file from "test" folder
                Path myCsar = alienRepository.getCSAR(csarName, csarVersion);
                FileSystem csarFS = FileSystems.newFileSystem(myCsar, null);
                Path definitionsFolderPath = csarFS.getPath(DEFAULT_TEST_FOLDER);
                if (!Files.exists(definitionsFolderPath)) {
                    throw new NotFoundException("yaml template should exist in folder [" + DEFAULT_TEST_FOLDER + "]");
                }
                // read the first yaml file found (only one currently)
                YamlTestFileVisitor testFileExtractor = new YamlTestFileVisitor();
                Files.walkFileTree(definitionsFolderPath, testFileExtractor);
                testFileExtractor.getYamlTestFile();
                Path yamlFilePath = testFileExtractor.getYamlTestFile();
                if (yamlFilePath == null) {
                    throw new NotFoundException("Cannot find a yaml file in subfolder [" + DEFAULT_TEST_FOLDER + "]");
                }
                // define a new topology
                String topologyId = csar.getTopologyId();
                if (topologyId == null) {
                    topologyId = UUID.randomUUID().toString();
                    csar.setTopologyId(topologyId);
                }
                csar.setCloudId(cloudId);
                csarDAO.save(csar);
                // update the topology object for the CSAR.
                Topology topology = YamlParserUtil.parseFromUTF8File(yamlFilePath, Topology.class);
                topology.setId(topologyId);
                csarDAO.save(topology);
                // deploy this topology
                DeploymentSetup deploymentSetup = new DeploymentSetup();
                DeploymentSetupMatchInfo deploymentSetupMatchInfo = deploymentSetupService.generateCloudResourcesMapping(deploymentSetup, topology, cloud,
                        false);
                if (!deploymentSetupMatchInfo.isValid()) {
                    throw new InvalidDeploymentSetupException("Test topology for CSAR [" + csar.getId() + "] is not deployable on the cloud [" + cloud.getId()
                            + "] because it contains unmatchable resources");
                }
                deploymentSetupService.generatePropertyDefinition(deploymentSetup, cloud);
                deploymentId = deploymentService.deployTopology(topology, csar, deploymentSetup, cloudId);
            } catch (CloudDisabledException e) {
                return RestResponseBuilder.<String> builder().data(null).error(new RestError(RestErrorCode.CLOUD_DISABLED_ERROR.getCode(), e.getMessage()))
                        .build();
            }
        }
        return RestResponseBuilder.<String> builder().data(deploymentId).build();
    }
}
