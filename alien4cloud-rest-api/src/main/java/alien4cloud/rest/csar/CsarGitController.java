package alien4cloud.rest.csar;

import java.io.IOException;
import java.util.List;

import javax.annotation.Resource;
import javax.validation.Valid;

import org.springframework.http.MediaType;
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
import alien4cloud.model.components.Csar;
import alien4cloud.rest.component.SearchRequest;
import alien4cloud.rest.model.RestErrorBuilder;
import alien4cloud.rest.model.RestErrorCode;
import alien4cloud.rest.model.RestResponse;
import alien4cloud.rest.model.RestResponseBuilder;
import alien4cloud.security.model.CsarGitCheckoutLocation;
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
    private CsarGitService csarGithubService;

    /**
     * Retrieve a CsarGit from the system
     *
     * @param param The unique id or url of the CsarGit to retrieve.
     * @return The CsarGit matching the requested id or url.
     */
    @ApiOperation(value = "Get a CSARGit in ALIEN.")
    @RequestMapping(value = "/{id:.+}", method = RequestMethod.GET, consumes = MediaType.APPLICATION_JSON_VALUE)
    @Audit
    public RestResponse<CsarGitRepository> get(@PathVariable String csarId) {
        if (csarId == null) {
            return RestResponseBuilder.<CsarGitRepository> builder()
                    .error(RestErrorBuilder.builder(RestErrorCode.ILLEGAL_PARAMETER).message("id cannot be null").build()).build();
        }
        CsarGitRepository csargit = alienDAO.findById(CsarGitRepository.class, csarId);
        return RestResponseBuilder.<CsarGitRepository> builder().data(csargit).build();
    }

    /**
     * Retrieve a CsarGit from the system by this URL
     *
     * @param param The unique url of the CsarGit to retrieve.
     * @return The CsarGit matching the requested id or url.
     */
    @ApiOperation(value = "Get a CSARGit in ALIEN.")
    @RequestMapping(value = "/get", method = RequestMethod.POST, consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    @Audit
    public RestResponse<CsarGitRepository> getByUrl(@Valid @RequestBody String param) {
        if (param == null || param.isEmpty()) {
            return RestResponseBuilder.<CsarGitRepository> builder()
                    .error(RestErrorBuilder.builder(RestErrorCode.ILLEGAL_PARAMETER).message("url cannot be null or empty").build()).build();
        }
        CsarGitRepository csargit = csarGithubService.getCsargitByUrl(param);
        return RestResponseBuilder.<CsarGitRepository> builder().data(csargit).build();
    }

    @ApiOperation(value = "Search csargits", notes = "Returns a search result with that contains CSARGIT matching the request.")
    @RequestMapping(value = "/search", method = RequestMethod.POST, consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public RestResponse<GetMultipleDataResult<CsarGitRepository>> search(@RequestBody SearchRequest searchRequest) {
        // TODO Suport search request for csarsgit based on id ...
        GetMultipleDataResult<CsarGitRepository> searchResult = alienDAO.search(CsarGitRepository.class, searchRequest.getQuery(), null,
                searchRequest.getFrom(), searchRequest.getSize());
        searchResult.setData(searchResult.getData());
        return RestResponseBuilder.<GetMultipleDataResult<CsarGitRepository>> builder().data(searchResult).build();
    }

    /**
     * Retrieve a CsarGit from github
     * 
     * @param id The unique id of the CsarGit to retrieve.
     * @return The CsarGit matching the requested id.
     * @throws ParsingException
     * @throws CSARVersionAlreadyExistsException
     * @throws IOException
     */
    @ApiOperation(value = "Specify a CSAR from Git and proceed to its import in Alien.")
    @RequestMapping(value = "/import/{id:.+}", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
    @Audit
    public RestResponse<GetMultipleDataResult<ParsingResult<Csar>>> specify(@Valid @RequestBody String param) throws CSARVersionAlreadyExistsException,
            ParsingException, IOException {
        if (param == null || param.isEmpty()) {
            return RestResponseBuilder.<GetMultipleDataResult<ParsingResult<Csar>>> builder()
                    .error(RestErrorBuilder.builder(RestErrorCode.ILLEGAL_PARAMETER).message("Url cannot be null or empty").build()).build();
        }
        ParsingResult<Csar>[] parsingResult = csarGithubService.specifyCsarFromGit(param);
        GetMultipleDataResult<ParsingResult<Csar>> getMultipleDataResult = new GetMultipleDataResult<ParsingResult<Csar>>(
                new String[] { ParsingResult.class.toString() }, parsingResult);
        return RestResponseBuilder.<GetMultipleDataResult<ParsingResult<Csar>>> builder().data(getMultipleDataResult).build();
    }

    /**
     * Create a new CsarGit in the system
     * 
     * @param request The CsarGit to save in the system.
     * @return an empty (void) rest {@link RestResponse}.
     */
    @ApiOperation(value = "Create a new CSARGit from a Git location in ALIEN.")
    @RequestMapping(method = RequestMethod.POST, consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    @Audit
    public RestResponse<String> create(@Valid @RequestBody CreateCsarGithubRequest request) {
        CsarGitRepository csargit = csarGithubService.getCsargitByUrl(request.getRepositoryUrl());
        if (csargit != null) {
            if(request.getRepositoryUrl().equals(csargit.getRepositoryUrl())){
                return RestResponseBuilder.<String> builder()
                        .error(RestErrorBuilder.builder(RestErrorCode.ILLEGAL_PARAMETER).message("An existing CSAR with the same url and repository already exists").build())
                        .build();
            }
        }
        String csarId = csarGithubService.createGithubCsar(request.getRepositoryUrl(), request.getUsername(), request.getPassword(),
                request.getImportLocations());
        return RestResponseBuilder.<String> builder().data(csarId).build();
    }

    /**
     * Delete a CsarGit in the system.
     * 
     * @param id The unique id of the CsarGit to delete.
     * @return an empty (void) rest {@link RestResponse}.
     */
    @ApiOperation(value = "Delete a CSARGit in ALIEN.")
    @RequestMapping(value = "/{id}", method = RequestMethod.DELETE)
    @Audit
    public RestResponse<Void> deleteCsarGit(@PathVariable String id) {
        if (id == null) {
            return RestResponseBuilder.<Void> builder().error(RestErrorBuilder.builder(RestErrorCode.ILLEGAL_PARAMETER).message("id cannot be null").build())
                    .build();
        }
        alienDAO.delete(CsarGitRepository.class, id);
        return RestResponseBuilder.<Void> builder().build();
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
    @Audit
    public RestResponse<Void> addLocation(@Valid @PathVariable String id, @RequestBody AddCsarGitLocation request) {
        csarGithubService.addImportLocation(id, request.getImportLocations());
        return RestResponseBuilder.<Void> builder().build();
    }

    /**
     * Remove an importLocation from an existing CsarGit
     * 
     * @param branchId The unique id of the importLocation
     * @param id The unique id of the CsarGit to reach
     * @return an empty (void) rest {@link RestResponse}.
     */
    @ApiOperation(value = "Delete importLocation of a CSARGit in ALIEN.")
    @RequestMapping(value = "/{id}/importLocations/{branchId}", method = RequestMethod.DELETE, consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    @Audit
    public RestResponse<Void> deleteImportLocation(@Valid @PathVariable String id, @PathVariable String branchId) {
        csarGithubService.removeImportLocation(id, branchId);
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
    @Audit
    public RestResponse<Void> update(@Valid @PathVariable String id, @RequestBody UpdateCsarGithubRequest request) {
        csarGithubService.update(id, request.getRepositoryUrl(), request.getUsername(), request.getPassword());
        return RestResponseBuilder.<Void> builder().build();
    }
}
