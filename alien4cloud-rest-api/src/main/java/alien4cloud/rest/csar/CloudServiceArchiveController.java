package alien4cloud.rest.csar;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Set;

import javax.annotation.Resource;
import javax.validation.Valid;

import org.alien4cloud.tosca.catalog.ArchiveUploadService;
import org.alien4cloud.tosca.catalog.index.ICsarAuthorizationFilter;
import org.alien4cloud.tosca.catalog.index.ICsarService;
import org.alien4cloud.tosca.catalog.repository.CsarFileRepository;
import org.alien4cloud.tosca.model.CSARDependency;
import org.alien4cloud.tosca.model.Csar;
import org.apache.commons.collections4.CollectionUtils;
import org.elasticsearch.common.collect.Sets;
import org.springframework.beans.factory.annotation.Required;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.google.common.collect.Lists;

import alien4cloud.audit.annotation.Audit;
import alien4cloud.common.AlienConstants;
import alien4cloud.component.repository.exception.CSARUsedInActiveDeployment;
import alien4cloud.dao.IGenericSearchDAO;
import alien4cloud.dao.model.FacetedSearchResult;
import alien4cloud.exception.AlreadyExistException;
import alien4cloud.model.common.Usage;
import alien4cloud.model.components.CSARSource;
import alien4cloud.rest.component.SearchRequest;
import alien4cloud.rest.model.RestError;
import alien4cloud.rest.model.RestErrorBuilder;
import alien4cloud.rest.model.RestErrorCode;
import alien4cloud.rest.model.RestResponse;
import alien4cloud.rest.model.RestResponseBuilder;
import alien4cloud.tosca.parser.ParsingError;
import alien4cloud.tosca.parser.ParsingErrorLevel;
import alien4cloud.tosca.parser.ParsingException;
import alien4cloud.tosca.parser.ParsingResult;
import alien4cloud.tosca.parser.impl.ErrorCode;
import alien4cloud.utils.FileUploadUtil;
import alien4cloud.utils.FileUtil;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;

@RestController
@RequestMapping({ "/rest/csars", "/rest/v1/csars", "/rest/latest/csars" })
@Slf4j
public class CloudServiceArchiveController {

    @Resource
    private ArchiveUploadService csarUploadService;
    @Resource(name = "alien-es-dao")
    private IGenericSearchDAO csarDAO;
    private Path tempDirPath;
    @Resource
    private ICsarService csarService;
    @Resource
    private ICsarAuthorizationFilter csarAuthorizationFilter;

    @ApiOperation(value = "Upload a csar zip file.")
    @RequestMapping(method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("isAuthenticated()")
    @Audit
    public RestResponse<CsarUploadResult> uploadCSAR(@RequestParam(required = false) String workspace, @RequestParam("file") MultipartFile csar)
            throws IOException {
        Path csarPath = null;
        try {
            if (workspace == null) {
                workspace = AlienConstants.GLOBAL_WORKSPACE_ID;
            }
            log.info("Serving file upload with name [" + csar.getOriginalFilename() + "]");
            csarPath = Files.createTempFile(tempDirPath, null, '.' + CsarFileRepository.CSAR_EXTENSION);
            // save the archive in the temp directory
            FileUploadUtil.safeTransferTo(csarPath, csar);
            // load, parse the archive definitions and save on disk
            ParsingResult<Csar> result = csarUploadService.upload(csarPath, CSARSource.UPLOAD, workspace);
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
        } catch (AlreadyExistException e) {
            log.error("A CSAR with the same name and the same version already existed in the repository", e);
            CsarUploadResult uploadResult = new CsarUploadResult();
            uploadResult.getErrors().put(csar.getOriginalFilename(), Lists.newArrayList(new ParsingError(ErrorCode.CSAR_ALREADY_EXISTS, "CSAR already exists",
                    null, "Unable to override an existing CSAR if the version is not a SNAPSHOT version.", null, null)));
            return RestResponseBuilder.<CsarUploadResult> builder().error(RestErrorBuilder.builder(RestErrorCode.ALREADY_EXIST_ERROR).build())
                    .data(uploadResult).build();
        } catch (CSARUsedInActiveDeployment e) {
            log.error("This csar is used in an active deployment. It cannot be overrided.", e);
            CsarUploadResult uploadResult = new CsarUploadResult();
            uploadResult.getErrors().put(csar.getOriginalFilename(), Lists.newArrayList(new ParsingError(ErrorCode.CSAR_USED_IN_ACTIVE_DEPLOYMENT,
                    "CSAR used in active deployment", null, "Unable to override a csar used in an active deployment.", null, null)));
            return RestResponseBuilder.<CsarUploadResult> builder().error(RestErrorBuilder.builder(RestErrorCode.RESOURCE_USED_ERROR).build())
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

    @ApiOperation(value = "Add dependency to the csar with given id.")
    @RequestMapping(value = "/{csarId:.+?}/dependencies", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("isAuthenticated()")
    @Audit
    @Deprecated
    public RestResponse<Boolean> addDependency(@PathVariable String csarId, @Valid @RequestBody CSARDependency dependency) {
        Csar csar = csarService.getOrFail(csarId);
        csarAuthorizationFilter.checkWriteAccess(csar);
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
    @PreAuthorize("isAuthenticated()")
    @Audit
    public RestResponse<List<Usage>> delete(@PathVariable String csarId) {
        Csar csar = csarService.getOrFail(csarId);
        csarAuthorizationFilter.checkWriteAccess(csar);
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
    @PreAuthorize("isAuthenticated()")
    public RestResponse<CsarInfoDTO> read(@PathVariable String csarId) {
        Csar csar = csarService.get(csarId);
        csarAuthorizationFilter.checkReadAccess(csar);
        List<Usage> relatedResourceList = csarService.getCsarRelatedResourceList(csar);
        CsarInfoDTO csarInfo = new CsarInfoDTO(csar, relatedResourceList);
        return RestResponseBuilder.<CsarInfoDTO> builder().data(csarInfo).build();
    }

    @ApiOperation(value = "Search for cloud service archives.")
    @RequestMapping(value = "/search", method = RequestMethod.POST, consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("isAuthenticated()")
    public RestResponse<FacetedSearchResult> search(@RequestBody SearchRequest searchRequest) {
        return RestResponseBuilder.<FacetedSearchResult> builder()
                .data(csarService.search(searchRequest.getQuery(), searchRequest.getFrom(), searchRequest.getSize(), searchRequest.getFilters())).build();
    }

    @Required
    @Value("${directories.alien}/${directories.upload_temp}")
    public void setTempDirPath(String tempDirPath) throws IOException {
        this.tempDirPath = FileUtil.createDirectoryIfNotExists(tempDirPath);
        log.info("Temporary folder for upload was set to [" + this.tempDirPath + "]");
    }

}
