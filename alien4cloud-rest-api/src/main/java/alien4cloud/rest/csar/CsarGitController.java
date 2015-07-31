package alien4cloud.rest.csar;

import java.io.IOException;

import javax.annotation.Resource;
import javax.validation.Valid;

import org.hibernate.validator.constraints.NotBlank;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import alien4cloud.audit.annotation.Audit;
import alien4cloud.component.repository.exception.CSARVersionAlreadyExistsException;
import alien4cloud.csar.services.CsarGitService;
import alien4cloud.dao.IGenericSearchDAO;
import alien4cloud.dao.model.GetMultipleDataResult;
import alien4cloud.exception.GitCloneUriException;
import alien4cloud.exception.GitNotAuthorizedException;
import alien4cloud.model.components.Csar;
import alien4cloud.rest.component.SearchRequest;
import alien4cloud.rest.model.RestErrorBuilder;
import alien4cloud.rest.model.RestErrorCode;
import alien4cloud.rest.model.RestResponse;
import alien4cloud.rest.model.RestResponseBuilder;
import alien4cloud.security.model.CsarGitRepository;
import alien4cloud.tosca.parser.ParsingException;
import alien4cloud.tosca.parser.ParsingResult;

import com.wordnik.swagger.annotations.ApiOperation;

@RestController
@RequestMapping("/rest/csarsgit")
public class CsarGitController {
    @Resource(name = "alien-es-dao")
    private IGenericSearchDAO alienDAO;

    @Resource
    private CsarGitService csarGitService;
    

    /**
     * Retrieve a CsarGit from the system
     *
     * @param param The unique id of the CsarGit to retrieve.
     * @return The CsarGit matching the requested id or url.
     */
    @ApiOperation(value = "Get a CSARGit in ALIEN by id.")
    @RequestMapping(value = "/{id}", method = RequestMethod.GET)
    @PreAuthorize("hasAnyAuthority('ADMIN', 'COMPONENTS_MANAGER', 'ARCHITECT')")
    @Audit
    public RestResponse<CsarGitRepository> get(@Valid @NotBlank @PathVariable String id) {
        if (id == null) {
            return RestResponseBuilder.<CsarGitRepository> builder()
                    .error(RestErrorBuilder.builder(RestErrorCode.ILLEGAL_PARAMETER).message("id cannot be null").build()).build();
        }
        CsarGitRepository csargit = alienDAO.findById(CsarGitRepository.class, id);
        return RestResponseBuilder.<CsarGitRepository> builder().data(csargit).build();
    }

    /**
     * Retrieve a CsarGit from the system by this URL
     *
     * @param param The unique url of the CsarGit to retrieve.
     * @return The CsarGit matching the requested id or url.
     */
    @ApiOperation(value = "Get a CSARGit in ALIEN by url.")
    @RequestMapping(value = "/get", method = RequestMethod.POST, consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasAnyAuthority('ADMIN', 'COMPONENTS_MANAGER', 'ARCHITECT')")
    @Audit
    public RestResponse<CsarGitRepository> getByUrl(@Valid @RequestBody String url) {
        if (url == null || url.isEmpty()) {
            return RestResponseBuilder.<CsarGitRepository> builder()
                    .error(RestErrorBuilder.builder(RestErrorCode.ILLEGAL_PARAMETER).message("Url cannot be null or empty").build()).build();
        }
        CsarGitRepository csargit = csarGitService.getCsargitByUrl(url);
        return RestResponseBuilder.<CsarGitRepository> builder().data(csargit).build();
    }

    @ApiOperation(value = "Search csargits", notes = "Returns a search result with that contains CSARGIT matching the request.")
    @RequestMapping(value = "/search", method = RequestMethod.POST, consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasAnyAuthority('ADMIN', 'COMPONENTS_MANAGER', 'ARCHITECT')")
    public RestResponse<GetMultipleDataResult<CsarGitRepository>> search(@RequestBody SearchRequest searchRequest) {
        GetMultipleDataResult<CsarGitRepository> searchResult = alienDAO.search(CsarGitRepository.class, searchRequest.getQuery(), null,
                searchRequest.getFrom(), searchRequest.getSize());
        searchResult.setData(searchResult.getData());
        return RestResponseBuilder.<GetMultipleDataResult<CsarGitRepository>> builder().data(searchResult).build();
    }

    /**
     * Retrieve a CsarGit from git
     * 
     * @param id The unique id of the CsarGit to retrieve.
     * @return The CsarGit matching the requested id.
     * @throws ParsingException
     * @throws CSARVersionAlreadyExistsException
     * @throws IOException
     * @throws GitCloneUriException
     * @throws GitNotAuthorizedException 
     */
    @ApiOperation(value = "Specify a CSAR from Git and proceed to its import in Alien.")
    @RequestMapping(value = "/import/{id:.+}", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasAnyAuthority('ADMIN', 'COMPONENTS_MANAGER', 'ARCHITECT')")
    @Audit
    public RestResponse<GetMultipleDataResult<ParsingResult<Csar>>> specify(@Valid @RequestBody String param) throws CSARVersionAlreadyExistsException,
            ParsingException, IOException, GitCloneUriException, GitNotAuthorizedException {
        if (param == null || param.isEmpty()) {
            return RestResponseBuilder.<GetMultipleDataResult<ParsingResult<Csar>>> builder()
                    .error(RestErrorBuilder.builder(RestErrorCode.ILLEGAL_PARAMETER).message("Url cannot be null or empty").build()).build();
        }
        ParsingResult<Csar>[] parsingResult = csarGitService.specifyCsarFromGit(param);
        GetMultipleDataResult<ParsingResult<Csar>> getMultipleDataResult = new GetMultipleDataResult<ParsingResult<Csar>>(
                new String[] { ParsingResult.class.toString() }, parsingResult);
        return RestResponseBuilder.<GetMultipleDataResult<ParsingResult<Csar>>> builder().data(getMultipleDataResult).build();
    }

    /**
     * Create a new CsarGit in the system
     * 
     * @param request The CsarGit to save in the system.
     * @return an the id of the created CsarGit {@link RestResponse}.
     */
    @ApiOperation(value = "Create a new CSARGit from a Git location in ALIEN.")
    @RequestMapping(method = RequestMethod.POST, consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasAnyAuthority('ADMIN', 'COMPONENTS_MANAGER', 'ARCHITECT')")
    @Audit
    public RestResponse<String> create(@Valid @RequestBody CreateCsarGitRequest request) {
        CsarGitRepository csargit = csarGitService.getCsargitByUrl(request.getRepositoryUrl());
        if (csargit != null) {
            if (request.getRepositoryUrl().equals(csargit.getRepositoryUrl())) {
                return RestResponseBuilder
                        .<String> builder()
                        .error(RestErrorBuilder.builder(RestErrorCode.ILLEGAL_PARAMETER)
                                .message("An existing CSAR with the same url and repository already exists").build()).build();
            }
        }
        if (!csarGitService.paramIsUrl(request.getRepositoryUrl()) || request.getRepositoryUrl().isEmpty() || request.getImportLocations().isEmpty()) {
            return RestResponseBuilder.<String> builder()
                    .error(RestErrorBuilder.builder(RestErrorCode.ILLEGAL_PARAMETER).message("CSAR's data are not valid").build()).build();
        }
        String csarId = csarGitService.createGitCsar(request.getRepositoryUrl(), request.getUsername(), request.getPassword(), request.getImportLocations());
        return RestResponseBuilder.<String> builder().data(csarId).build();
    }

    /**
     * Delete a CsarGit in the system.
     * 
     * @param id The unique id of the CsarGit to delete.
     * @return the id of the CsarGit deleted {@link RestResponse}.
     */
    @ApiOperation(value = "Delete a CSARGit in ALIEN by id.")
    @RequestMapping(value = "/{id}", method = RequestMethod.DELETE)
    @PreAuthorize("hasAnyAuthority('ADMIN', 'COMPONENTS_MANAGER', 'ARCHITECT')")
    @Audit
    public RestResponse<String> deleteCsarGit(@PathVariable String id) {
        if (id == null) {
            return RestResponseBuilder.<String> builder().error(RestErrorBuilder.builder(RestErrorCode.ILLEGAL_PARAMETER).message("id cannot be null").build())
                    .build();
        }
        if (csarGitService.checkIfCsarExist(id) != null) {
            alienDAO.delete(CsarGitRepository.class, id);
            return RestResponseBuilder.<String> builder().data(id).build();
        }
        return RestResponseBuilder.<String> builder().data(id)
                .error(RestErrorBuilder.builder(RestErrorCode.NOT_FOUND_ERROR).message("No csargit exists with this id").build()).build();
    }

    /**
     * Retrieve a CsarGit from the system by this URL
     *
     * @param param The unique url of the CsarGit to retrieve.
     * @return The CsarGit matching the requested id or url.
     */
    @ApiOperation(value = "Delete a CSARGit in ALIEN by url.")
    @RequestMapping(value = "/delete/{url}", method = RequestMethod.POST)
    @PreAuthorize("hasAnyAuthority('ADMIN', 'COMPONENTS_MANAGER', 'ARCHITECT')")
    @Audit
    public RestResponse<String> deleteCsargitByUrl(@Valid @RequestBody String url) {
        if (url == null || url.isEmpty()) {
            return RestResponseBuilder.<String> builder()
                    .error(RestErrorBuilder.builder(RestErrorCode.ILLEGAL_PARAMETER).message("url cannot be null or empty").build()).build();
        }
        String result = csarGitService.deleteCsargitByUrl(url);
        if (result.equals("not found")) {
            return RestResponseBuilder.<String> builder().data(result)
                    .error(RestErrorBuilder.builder(RestErrorCode.NOT_FOUND_ERROR).message("No csargit exists with this url").build()).build();
        } else {
            return RestResponseBuilder.<String> builder().data(result).build();
        }
    }

    /**
     * Add importLocation to a CsarGit
     * 
     * @param request The list of the importLocation to add
     * @param id The unique id of the CsarGit to update
     * @return an empty(void) rest {@link RestResponse}
     */
    @ApiOperation(value = "Add importLocation in a CSARGit.")
    @RequestMapping(value = "/{id}/importLocations/{importLocation}", method = RequestMethod.PUT, consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasAnyAuthority('ADMIN', 'COMPONENTS_MANAGER', 'ARCHITECT')")
    @Audit
    public RestResponse<Void> addLocation(@Valid @PathVariable String id, @RequestBody AddCsarGitLocation request) {
        csarGitService.addImportLocation(id, request.getImportLocations());
        return RestResponseBuilder.<Void> builder().build();
    }

    /**
     * Remove an importLocation from an existing CsarGit
     * 
     * @param branchId The unique id of the importLocation
     * @param id The unique id of the CsarGit to reach
     * @return an empty (void) rest {@link RestResponse}.
     */
    @ApiOperation(value = "Delete importLocation of a CSARGit in ALIEN by id.")
    @RequestMapping(value = "/{id}/importLocations/{branchId}", method = RequestMethod.DELETE, consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasAnyAuthority('ADMIN', 'COMPONENTS_MANAGER', 'ARCHITECT')")
    @Audit
    public RestResponse<Void> deleteImportLocationById(@Valid @PathVariable String id, @PathVariable String branchId) {
        csarGitService.removeImportLocationById(id, branchId);
        return RestResponseBuilder.<Void> builder().build();
    }

    /**
     * Remove an importLocation from an existing CsarGit
     * 
     * @param branchId The unique id of the importLocation
     * @param url The unique url of the CsarGit to reach
     * @return an empty (void) rest {@link RestResponse}.
     */
    @ApiOperation(value = "Delete importLocation of a CSARGit in ALIEN by url.")
    @RequestMapping(value = "/{url}/importLocations/{branchId}", method = RequestMethod.POST, consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasAnyAuthority('ADMIN', 'COMPONENTS_MANAGER', 'ARCHITECT')")
    @Audit
    public RestResponse<Void> deleteImportLocationbyUrl(@Valid @PathVariable String url, @PathVariable String branchId) {
        csarGitService.removeImportLocationByUrl(url, branchId);
        return RestResponseBuilder.<Void> builder().build();
    }

    /**
     * Update an existing CsarGit
     * 
     * @param request The CsarGit data to update
     * @return an empty (void) rest {@link RestResponse}.
     */
    @ApiOperation(value = "Update a CSARGit by id.")
    @RequestMapping(value = "/{id}", method = RequestMethod.PUT, consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasAnyAuthority('ADMIN', 'COMPONENTS_MANAGER', 'ARCHITECT')")
    @Audit
    public RestResponse<Void> update(@PathVariable String id, @RequestBody UpdateCsarGitRequest request) {
        csarGitService.update(id, request.getRepositoryUrl(), request.getUsername(), request.getPassword());
        return RestResponseBuilder.<Void> builder().build();
    }
}