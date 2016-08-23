package alien4cloud.rest.csar;

import io.swagger.annotations.ApiOperation;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.annotation.Resource;
import javax.validation.Valid;

import lombok.extern.slf4j.Slf4j;

import org.apache.commons.collections4.CollectionUtils;
import org.elasticsearch.common.collect.Maps;
import org.elasticsearch.common.collect.Sets;
import org.elasticsearch.index.query.QueryBuilders;
import org.springframework.beans.factory.annotation.Required;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import springfox.documentation.annotations.ApiIgnore;
import alien4cloud.audit.annotation.Audit;
import alien4cloud.component.ICSARRepositoryIndexerService;
import alien4cloud.component.repository.CsarFileRepository;
import alien4cloud.component.repository.exception.CSARVersionAlreadyExistsException;
import alien4cloud.csar.services.CsarService;
import alien4cloud.dao.IGenericSearchDAO;
import alien4cloud.dao.model.FacetedSearchResult;
import alien4cloud.exception.AlreadyExistException;
import alien4cloud.exception.NotFoundException;
import alien4cloud.model.common.Usage;
import alien4cloud.model.components.CSARDependency;
import alien4cloud.model.components.CSARSource;
import alien4cloud.model.components.Csar;
import alien4cloud.model.components.IndexedNodeType;
import alien4cloud.rest.component.SearchRequest;
import alien4cloud.rest.model.RestError;
import alien4cloud.rest.model.RestErrorBuilder;
import alien4cloud.rest.model.RestErrorCode;
import alien4cloud.rest.model.RestResponse;
import alien4cloud.rest.model.RestResponseBuilder;
import alien4cloud.tosca.ArchiveUploadService;
import alien4cloud.tosca.parser.ParsingError;
import alien4cloud.tosca.parser.ParsingErrorLevel;
import alien4cloud.tosca.parser.ParsingException;
import alien4cloud.tosca.parser.ParsingResult;
import alien4cloud.tosca.parser.impl.ErrorCode;
import alien4cloud.utils.FileUploadUtil;
import alien4cloud.utils.FileUtil;
import alien4cloud.utils.VersionUtil;

import com.google.common.collect.Lists;

@RestController
@RequestMapping({ "/rest/csars", "/rest/v1/csars", "/rest/latest/csars" })
@Slf4j
public class CloudServiceArchiveController {

    @Resource
    private ArchiveUploadService csarUploadService;
    @Resource(name = "alien-es-dao")
    private IGenericSearchDAO csarDAO;
    @Resource
    private ICSARRepositoryIndexerService indexerService;
    private Path tempDirPath;
    @Resource
    private CsarService csarService;

    @ApiOperation(value = "Upload a csar zip file.")
    @RequestMapping(method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasAnyAuthority('ADMIN', 'COMPONENTS_MANAGER', 'ARCHITECT')")
    @Audit
    public RestResponse<CsarUploadResult> uploadCSAR(@RequestParam("file") MultipartFile csar) throws IOException {
        Path csarPath = null;
        try {

            log.info("Serving file upload with name [" + csar.getOriginalFilename() + "]");
            csarPath = Files.createTempFile(tempDirPath, null, '.' + CsarFileRepository.CSAR_EXTENSION);
            // save the archive in the temp directory
            FileUploadUtil.safeTransferTo(csarPath, csar);
            // load, parse the archive definitions and save on disk
            ParsingResult<Csar> result = csarUploadService.upload(csarPath, CSARSource.UPLOAD);
            RestError error = null;
            if (result.hasError(ParsingErrorLevel.ERROR)) {
                error = RestErrorBuilder.builder(RestErrorCode.CSAR_PARSING_ERROR).build();
            }
            return RestResponseBuilder.<CsarUploadResult> builder().error(error).data(toUploadResult(result)).build();
        } catch (ParsingException e) {
            log.error("Error happened while parsing csar file <" + e.getFileName() + ">", e);
            String fileName = e.getFileName() == null ? csar.getOriginalFilename() : e.getFileName();

            CsarUploadResult uploadResult = new CsarUploadResult();
            uploadResult.getErrors().put(fileName, e.getParsingErrors());
            return RestResponseBuilder.<CsarUploadResult> builder().error(RestErrorBuilder.builder(RestErrorCode.CSAR_INVALID_ERROR).build()).data(uploadResult)
                    .build();
        } catch (CSARVersionAlreadyExistsException e) {
            log.error("A CSAR with the same name and the same version already existed in the repository", e);
            CsarUploadResult uploadResult = new CsarUploadResult();
            uploadResult.getErrors().put(csar.getOriginalFilename(), Lists.newArrayList(new ParsingError(ErrorCode.CSAR_ALREADY_EXISTS, "CSAR already exists",
                    null, "Unable to override an existing CSAR if the version is not a SNAPSHOT version.", null, null)));
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
        if (result.getContext().getParsingErrors() != null && !result.getContext().getParsingErrors().isEmpty()) {
            uploadResult.getErrors().put(result.getContext().getFileName(), result.getContext().getParsingErrors());
        }
        return uploadResult;
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
    @PreAuthorize("hasAnyAuthority('ADMIN', 'COMPONENTS_MANAGER')")
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
    @PreAuthorize("hasAnyAuthority('ADMIN', 'COMPONENTS_MANAGER')")
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
    @PreAuthorize("hasAnyAuthority('ADMIN', 'COMPONENTS_MANAGER')")
    @Audit
    public RestResponse<List<Usage>> delete(@PathVariable String csarId) {
        Csar csar = csarService.getOrFail(csarId);
        List<Usage> relatedResourceList = csarService.deleteCsarWithElements(csar);

        if (CollectionUtils.isNotEmpty(relatedResourceList)) {
            String errorMessage = "The csar named <" + csar.getName() + "> in version <" + csar.getVersion()
                    + "> can not be deleted since it is referenced by other resources";
            return RestResponseBuilder.<List<Usage>> builder().data(relatedResourceList)
                    .error(RestErrorBuilder.builder(RestErrorCode.DELETE_REFERENCED_OBJECT_ERROR).message(errorMessage).build()).build();
        }

        return RestResponseBuilder.<List<Usage>> builder().build();
    }

    @ApiOperation(value = "Get a CSAR given its id.", notes = "Returns a CSAR.")
    @RequestMapping(value = "/{csarId:.+?}", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasAnyAuthority('ADMIN', 'COMPONENTS_MANAGER')")
    public RestResponse<CsarInfoDTO> read(@PathVariable String csarId) {
        Csar csar = csarDAO.findById(Csar.class, csarId);
        List<Usage> relatedResourceList = csarService.getCsarRelatedResourceList(csar);
        CsarInfoDTO csarInfo = new CsarInfoDTO(csar, relatedResourceList);
        return RestResponseBuilder.<CsarInfoDTO> builder().data(csarInfo).build();
    }

    @ApiOperation(value = "Search for cloud service archives.")
    @RequestMapping(value = "/search", method = RequestMethod.POST, consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasAnyAuthority('ADMIN', 'COMPONENTS_MANAGER')")
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
    @RequestMapping(value = "/{csarId:.+?}/nodetypes", method = RequestMethod.POST, consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasAnyAuthority('ADMIN', 'COMPONENTS_MANAGER')")
    @Audit
    public RestResponse<Void> saveNodeType(@PathVariable String csarId, @RequestBody IndexedNodeType nodeType) {
        Csar csar = csarService.getOrFail(csarId);
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

}
