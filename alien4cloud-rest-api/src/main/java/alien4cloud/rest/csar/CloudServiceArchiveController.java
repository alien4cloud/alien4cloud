package alien4cloud.rest.csar;

import java.io.InputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Set;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import javax.validation.Valid;

import org.alien4cloud.tosca.catalog.ArchiveUploadService;
import org.alien4cloud.tosca.catalog.exception.UploadExceptionUtil;
import org.alien4cloud.tosca.catalog.index.CsarService;
import org.alien4cloud.tosca.catalog.index.IArchiveIndexerAuthorizationFilter;
import org.alien4cloud.tosca.catalog.index.ICsarAuthorizationFilter;
import org.alien4cloud.tosca.catalog.index.ICsarSearchService;
import org.alien4cloud.tosca.catalog.repository.CsarFileRepository;
import org.alien4cloud.tosca.catalog.repository.ICsarRepositry;
import org.alien4cloud.tosca.model.CSARDependency;
import org.alien4cloud.tosca.model.Csar;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.fileupload.FileItemIterator;
import org.apache.commons.fileupload.FileItemStream;
import org.apache.commons.fileupload.FileUploadException;
import org.apache.commons.fileupload.servlet.ServletFileUpload;
import org.apache.commons.fileupload.util.Streams;
import com.google.common.collect.Sets;
import org.springframework.beans.factory.annotation.Required;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
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
import alien4cloud.component.repository.exception.CSARUsedInActiveDeployment;
import alien4cloud.component.repository.exception.ToscaTypeAlreadyDefinedInOtherCSAR;
import alien4cloud.dao.IGenericSearchDAO;
import alien4cloud.dao.model.FacetedSearchResult;
import alien4cloud.exception.AlreadyExistException;
import alien4cloud.model.common.Usage;
import alien4cloud.model.components.CSARSource;
import alien4cloud.rest.model.FilteredSearchRequest;
import alien4cloud.rest.model.RestError;
import alien4cloud.rest.model.RestErrorBuilder;
import alien4cloud.rest.model.RestErrorCode;
import alien4cloud.rest.model.RestResponse;
import alien4cloud.rest.model.RestResponseBuilder;
import alien4cloud.tosca.parser.ParsingErrorLevel;
import alien4cloud.tosca.parser.ParsingException;
import alien4cloud.tosca.parser.ParsingResult;
import alien4cloud.utils.AlienConstants;
import alien4cloud.utils.FileUploadUtil;
import alien4cloud.utils.FileUtil;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;

@RestController
@RequestMapping({ "/rest/csars", "/rest/v1/csars", "/rest/latest/csars" })
@Api(value = "", description = "Operations on CSARs")
@Slf4j
public class CloudServiceArchiveController {
    @Resource
    private ArchiveUploadService csarUploadService;
    @Resource(name = "alien-es-dao")
    private IGenericSearchDAO csarDAO;
    @Resource
    private ICsarSearchService csarSearchService;
    @Resource
    private CsarService csarService;
    @Resource
    private ICsarAuthorizationFilter csarAuthorizationFilter;
    @Resource
    private IArchiveIndexerAuthorizationFilter archiveIndexerAuthorizationFilter;
    @Resource
    private ICsarRepositry archiveRepositry;

    private Path tempDirPath;

    @ApiOperation(value = "Check validity of a csar zip file.")
    @RequestMapping(value = "/check",method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("isAuthenticated()")
    @Audit
    public RestResponse<CsarUploadResult> checkCSAR(@RequestParam(required = false) String workspace,@RequestParam("file") MultipartFile csar)  throws IOException{
        Path csarPath = null;

        try {
            log.info("Checking casr file with name [" + csar.getOriginalFilename() + "]");

            csarPath = Files.createTempFile(tempDirPath, null, '.' + CsarFileRepository.CSAR_EXTENSION);

            // save the archive in the temp directory
            FileUploadUtil.safeTransferTo(csarPath, csar);

            // load, parse the archive definitions and save on disk
            ParsingResult<Csar> result = csarUploadService.check(csarPath, CSARSource.UPLOAD, workspace);
            RestError error = null;
            if (result.hasError(ParsingErrorLevel.ERROR)) {
                error = RestErrorBuilder.builder(RestErrorCode.CSAR_PARSING_ERROR).build();
            }
            return RestResponseBuilder.<CsarUploadResult> builder().error(error).data(CsarUploadUtil.toUploadResult(result)).build();
        } catch (ParsingException e) {
            log.error("Error happened while parsing csar file <" + e.getFileName() + ">", e);
            String fileName = e.getFileName() == null ? csar.getOriginalFilename() : e.getFileName();

            CsarUploadResult uploadResult = new CsarUploadResult();
            uploadResult.getErrors().put(fileName, e.getParsingErrors());
            return RestResponseBuilder.<CsarUploadResult> builder().error(RestErrorBuilder.builder(RestErrorCode.CSAR_INVALID_ERROR).build()).data(uploadResult)
                    .build();
        } finally {
            if (csarPath != null) {
                // Clean up
                try {
                    FileUtil.delete(csarPath);
                } catch (IOException e) {
                    log.error("Can't delete csar file",e);
                }
            }
        }
    }

    @ApiOperation(value = "Upload a csar zip file.")
    @RequestMapping(method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("isAuthenticated()")
    @Audit
    public RestResponse<CsarUploadResult> uploadCSAR(HttpServletRequest request)
            throws IOException {
        Path csarPath = null;
        String fileName = "<csar>";
        try {
            String workspace = AlienConstants.GLOBAL_WORKSPACE_ID;
            ServletFileUpload upload = new ServletFileUpload();
            FileItemIterator iter = upload.getItemIterator(request);
            while (iter.hasNext()) {
               FileItemStream item = iter.next();
               InputStream stream = item.openStream();
               if (item.isFormField() && item.getFieldName().equals("workspace")) {
                  workspace = Streams.asString(stream);
                  archiveIndexerAuthorizationFilter.preCheckAuthorization(workspace);
                  log.info("Serving file upload with workspace [" + workspace + "]");
               } else {
                  log.info("Serving file upload with name [" + item.getName() + "]");
                  fileName = item.getName();
                  csarPath = Files.createTempFile(tempDirPath, null, '.' + CsarFileRepository.CSAR_EXTENSION);
                  // save the archive in the temp directory
                  FileUploadUtil.safeTransferTo(csarPath, stream);
               }
               stream.close();
            }

            // load, parse the archive definitions and save on disk
            ParsingResult<Csar> result = csarUploadService.upload(csarPath, CSARSource.UPLOAD, workspace);
            RestError error = null;
            if (result.hasError(ParsingErrorLevel.ERROR)) {
                error = RestErrorBuilder.builder(RestErrorCode.CSAR_PARSING_ERROR).build();
            }
            return RestResponseBuilder.<CsarUploadResult> builder().error(error).data(CsarUploadUtil.toUploadResult(result)).build();
        } catch (IOException | FileUploadException e) {
            log.error("Error happened while uploading CSAR", e);
            CsarUploadResult uploadResult = new CsarUploadResult();
            uploadResult.getErrors().put(fileName, Lists.newArrayList(UploadExceptionUtil.parsingErrorFromException(e)));
            return RestResponseBuilder.<CsarUploadResult> builder().error(RestErrorBuilder.builder(RestErrorCode.CSAR_INVALID_ERROR).build()).data(uploadResult)
                    .build();
        } catch (ParsingException e) {
            log.error("Error happened while parsing csar file <" + fileName + ">", e);
            CsarUploadResult uploadResult = new CsarUploadResult();
            uploadResult.getErrors().put(fileName, e.getParsingErrors());
            return RestResponseBuilder.<CsarUploadResult> builder().error(RestErrorBuilder.builder(RestErrorCode.CSAR_INVALID_ERROR).build()).data(uploadResult)
                    .build();
        } catch (AlreadyExistException | CSARUsedInActiveDeployment | ToscaTypeAlreadyDefinedInOtherCSAR e) {
            CsarUploadResult uploadResult = new CsarUploadResult();
            uploadResult.getErrors().put(fileName, Lists.newArrayList(UploadExceptionUtil.parsingErrorFromException(e)));
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
        Csar csar = csarService.getOrFail(csarId);
        csarAuthorizationFilter.checkReadAccess(csar);
        List<Usage> relatedResourceList = csarService.getCsarRelatedResourceList(csar);
        CsarInfoDTO csarInfo = new CsarInfoDTO(csar, relatedResourceList);
        return RestResponseBuilder.<CsarInfoDTO> builder().data(csarInfo).build();
    }

    @ApiOperation(value = "Download a CSAR given its id.", notes = "Returns zipped content of a CSAR.")
    @RequestMapping(value = "/{csarId:.+?}/download", method = RequestMethod.GET, produces = MediaType.APPLICATION_OCTET_STREAM_VALUE)
    @PreAuthorize("isAuthenticated()")
    @SneakyThrows
    public ResponseEntity<InputStreamResource> download(@PathVariable String csarId) {
        Csar csar = csarService.getOrFail(csarId);
        csarAuthorizationFilter.checkReadAccess(csar);
        Path csarToDownload = archiveRepositry.getCSAR(csar.getName(), csar.getVersion());
        return ResponseEntity.ok().contentLength(csarToDownload.toFile().length()).contentType(MediaType.APPLICATION_OCTET_STREAM)
                .body(new InputStreamResource(Files.newInputStream(csarToDownload)));
    }

    @ApiOperation(value = "Search for cloud service archives.")
    @RequestMapping(value = "/search", method = RequestMethod.POST, consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("isAuthenticated()")
    public RestResponse<FacetedSearchResult> search(@RequestBody FilteredSearchRequest searchRequest) {
        return RestResponseBuilder.<FacetedSearchResult> builder()
                .data(csarSearchService.search(searchRequest.getQuery(), searchRequest.getFrom(), searchRequest.getSize(), searchRequest.getFilters())).build();
    }

    @Required
    @Value("${directories.alien}/${directories.upload_temp}")
    public void setTempDirPath(String tempDirPath) throws IOException {
        this.tempDirPath = FileUtil.createDirectoryIfNotExists(tempDirPath);
        log.info("Temporary folder for upload was set to [" + this.tempDirPath + "]");
    }

}
