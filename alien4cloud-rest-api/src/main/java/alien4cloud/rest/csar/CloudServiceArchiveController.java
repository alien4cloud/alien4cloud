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

import alien4cloud.application.DeploymentSetupService;
import alien4cloud.cloud.CloudResourceMatcherService;
import alien4cloud.cloud.CloudService;
import alien4cloud.cloud.DeploymentService;
import alien4cloud.component.repository.CsarFileRepository;
import alien4cloud.component.repository.exception.CSARVersionAlreadyExistsException;
import alien4cloud.component.repository.exception.CSARVersionNotFoundException;
import alien4cloud.csar.model.Csar;
import alien4cloud.csar.services.CsarService;
import alien4cloud.dao.IGenericSearchDAO;
import alien4cloud.dao.model.FacetedSearchResult;
import alien4cloud.exception.AlreadyExistException;
import alien4cloud.exception.NotFoundException;
import alien4cloud.model.application.DeploymentSetup;
import alien4cloud.model.cloud.Cloud;
import alien4cloud.model.cloud.ComputeTemplate;
import alien4cloud.model.deployment.Deployment;
import alien4cloud.paas.exception.CloudDisabledException;
import alien4cloud.rest.component.SearchRequest;
import alien4cloud.rest.model.RestError;
import alien4cloud.rest.model.RestErrorBuilder;
import alien4cloud.rest.model.RestErrorCode;
import alien4cloud.rest.model.RestResponse;
import alien4cloud.rest.model.RestResponseBuilder;
import alien4cloud.rest.topology.TopologyService;
import alien4cloud.security.AuthorizationUtil;
import alien4cloud.security.CloudRole;
import alien4cloud.tosca.container.archive.CsarUploadService;
import alien4cloud.tosca.container.exception.CSARIOException;
import alien4cloud.tosca.container.exception.CSARParsingException;
import alien4cloud.tosca.container.exception.CSARValidationException;
import alien4cloud.tosca.container.model.CSARDependency;
import alien4cloud.tosca.container.model.topology.Topology;
import alien4cloud.tosca.container.model.type.NodeType;
import alien4cloud.tosca.container.model.type.PropertyDefinition;
import alien4cloud.tosca.container.services.csar.ICSARRepositoryIndexerService;
import alien4cloud.tosca.container.validation.CSARError;
import alien4cloud.tosca.container.validation.CSARValidationResult;
import alien4cloud.utils.FileUploadUtil;
import alien4cloud.utils.FileUtil;
import alien4cloud.utils.YamlParserUtil;

import com.wordnik.swagger.annotations.ApiOperation;

@RestController
@RequestMapping("/rest/csars")
@Slf4j
public class CloudServiceArchiveController {
    private static final String DEFAULT_TEST_FOLDER = "test";

    @Resource
    private CsarUploadService csarUploadService;
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

    @ApiOperation(value = "Upload a csar zip file.")
    @RequestMapping(method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
    public RestResponse<CSARValidationResult> uploadCSAR(@RequestParam("file") MultipartFile csar) {
        Path csarPath = null;
        try {
            log.info("Serving file upload with name [" + csar.getOriginalFilename() + "]");
            csarPath = Files.createTempFile(tempDirPath, "", '.' + CsarFileRepository.CSAR_EXTENSION);
            // save the archive in the temp directory
            FileUploadUtil.safeTransferTo(csarPath, csar);
            // load, parse the archive definitions and save on disk
            Csar uploadedCsar = csarUploadService.uploadCsar(csarPath);
            return RestResponseBuilder.<CSARValidationResult> builder().data(new CSARValidationResult(uploadedCsar, null)).build();
        } catch (IOException e) {
            throw new CSARIOException("Exception happened why trying to store uploaded file to temporary folder", e);
        } catch (CSARValidationException e) {
            return RestResponseBuilder.<CSARValidationResult> builder().data(e.getCsarValidationResult())
                    .error(RestErrorBuilder.builder(RestErrorCode.CSAR_INVALID_ERROR).build()).build();
        } catch (CSARParsingException e) {
            log.error("Error happened while parsing csar file", e);
            CSARError error = e.createCSARError();
            Map<String, Set<CSARError>> errors = Maps.newHashMap();
            errors.put(e.getFileName(), Sets.newHashSet(error));
            return RestResponseBuilder.<CSARValidationResult> builder().data(new CSARValidationResult(errors))
                    .error(RestErrorBuilder.builder(RestErrorCode.CSAR_INVALID_ERROR).message(error.getMessage()).build()).build();
        } catch (CSARVersionAlreadyExistsException e) {
            log.error("A CSAR with the same name and the same version already existed in the repository", e);
            return RestResponseBuilder
                    .<CSARValidationResult> builder()
                    .error(RestErrorBuilder.builder(RestErrorCode.REPOSITORY_CSAR_ALREADY_EXISTED_ERROR)
                            .message("A CSAR with the same name and the same version already existed in the repository : " + e.getMessage()).build()).build();
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

    /**
     * Create a CSAR in SNAPSHOT version
     *
     * @param request
     * @return
     */
    @ApiOperation(value = "Create a CSAR in SNAPSHOT version.")
    @RequestMapping(method = RequestMethod.POST, consumes = MediaType.APPLICATION_JSON_VALUE)
    @ResponseStatus(value = HttpStatus.CREATED)
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
    public RestResponse<Void> delete(@PathVariable String csarId) {
        Csar csar = csarService.getMandatoryCsar(csarId);
        Map<String, NodeType> nodeTypes = csar.getNodeTypes();
        if (nodeTypes != null) {
            for (NodeType nodeType : nodeTypes.values()) {
                indexerService.deleteElement(csar.getName(), csar.getVersion(), nodeType);
            }
        }
        // check rights to delete ?
        csarDAO.delete(Csar.class, csarId);
        return RestResponseBuilder.<Void> builder().build();
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
        filters.put("version", new String[] { "SNAPSHOT" });
        FacetedSearchResult searchResult = csarDAO.facetedSearch(Csar.class, searchRequest.getQuery(), filters, null, searchRequest.getFrom(),
                searchRequest.getSize());
        return RestResponseBuilder.<FacetedSearchResult> builder().data(searchResult).build();
    }

    @ApiOperation(value = "Create or update a node type in the given cloud service archive.")
    @RequestMapping(value = "/{csarId:.+?}/nodetypes", method = RequestMethod.POST, consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public RestResponse<Void> saveNodeType(@PathVariable String csarId, @RequestBody NodeType nodeType) {
        Csar csar = csarService.getMandatoryCsar(csarId);
        Map<String, NodeType> nodeTypes = csar.getNodeTypes();
        if (nodeTypes == null) {
            nodeTypes = Maps.newHashMap();
            csar.setNodeTypes(nodeTypes);
        }
        nodeTypes.put(nodeType.getId(), nodeType);
        indexerService.indexInheritableElement(csar.getName(), csar.getVersion(), nodeType);
        csarDAO.save(csar);
        return RestResponseBuilder.<Void> builder().build();
    }

    @ApiOperation(value = "Get a node type defined in a cloud service archive.")
    @RequestMapping(value = "/{csarId:.+?}/nodetypes/{nodeTypeId}", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    public RestResponse<NodeType> getNodeType(@PathVariable String csarId, @PathVariable String nodeTypeId) {
        Csar csar = csarService.getMandatoryCsar(csarId);
        return RestResponseBuilder.<NodeType> builder().data(getNodeType(csar, nodeTypeId)).build();
    }

    @ApiOperation(value = "Removes a node type from a cloud service archive.")
    @RequestMapping(value = "/{csarId:.+?}/nodetypes/{nodeTypeId}", method = RequestMethod.DELETE, produces = MediaType.APPLICATION_JSON_VALUE)
    public RestResponse<Void> deleteNodeType(@PathVariable String csarId, @PathVariable String nodeTypeId) {
        Csar csar = csarService.getMandatoryCsar(csarId);
        if (csar.getNodeTypes() != null) {
            NodeType nodeType = csar.getNodeTypes().remove(nodeTypeId);
            if (nodeType != null) {
                indexerService.deleteElement(csar.getName(), csar.getVersion(), nodeType);
                csarDAO.save(csar);
            }
        }
        return RestResponseBuilder.<Void> builder().build();
    }

    private NodeType getNodeType(Csar csar, String nodeTypeId) {
        Map<String, NodeType> nodeTypes = csar.getNodeTypes();
        if (nodeTypes == null || !nodeTypes.containsKey(nodeTypeId)) {
            throw new NotFoundException("Node type with id [" + nodeTypeId + "] cannot be found");
        } else {
            return nodeTypes.get(nodeTypeId);
        }
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
    @ApiOperation(value = "Get active deployment for the given csar snapshot's test topology on the given cloud.", notes = "Application role required [ APPLICATION_MANAGER | APPLICATION_USER | APPLICATION_DEVOPS | DEPLOYMENT_MANAGER ]")
    @RequestMapping(value = "/{csarId:.+?}/active-deployment", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    public RestResponse<Deployment> getActiveDeployment(@PathVariable String csarId) {
        Csar csar = csarService.getMandatoryCsar(csarId);
        if (csar.getTopologyId() == null || csar.getCloudId() == null) {
            return RestResponseBuilder.<Deployment> builder().build();
        }
        Deployment deployment = deploymentService.getActiveDeployment(csar.getTopologyId(), csar.getCloudId());
        return RestResponseBuilder.<Deployment> builder().data(deployment).build();
    }

    @ApiOperation(value = "Deploy snapshot archive on a given cloud.")
    @RequestMapping(value = "/{csarName:.+?}/version/{csarVersion:.+?}/cloudid/{cloudId:.+?}", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
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
                Map<String, List<ComputeTemplate>> matchResult = cloudResourceMatcherService.matchTopology(topology, cloud).getMatchResult();
                Map<String, PropertyDefinition> propertyDefinitionMap = cloudService.getDeploymentPropertyDefinitions(cloudId);
                deploymentSetupService.fillWithDefaultValues(deploymentSetup, matchResult, propertyDefinitionMap);
                deploymentId = deploymentService.deployTopology(topology, cloudId, csar, deploymentSetup);
            } catch (CloudDisabledException e) {
                return RestResponseBuilder.<String> builder().data(null).error(new RestError(RestErrorCode.CLOUD_DISABLED_ERROR.getCode(), e.getMessage()))
                        .build();
            }
        }
        return RestResponseBuilder.<String> builder().data(deploymentId).build();
    }
}
