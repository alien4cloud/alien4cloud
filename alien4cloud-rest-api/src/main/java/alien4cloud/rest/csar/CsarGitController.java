package alien4cloud.rest.csar;

import java.util.List;

import javax.inject.Inject;
import javax.validation.Valid;

import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import alien4cloud.audit.annotation.Audit;
import alien4cloud.csar.services.CsarGitRepositoryService;
import alien4cloud.csar.services.CsarGitService;
import alien4cloud.dao.model.GetMultipleDataResult;
import org.alien4cloud.tosca.model.Csar;
import alien4cloud.model.git.CsarGitRepository;
import alien4cloud.rest.model.*;
import alien4cloud.tosca.parser.ParsingErrorLevel;
import alien4cloud.tosca.parser.ParsingResult;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;

@RestController
@RequestMapping(value = { "/rest/csarsgit", "/rest/v1/csarsgit", "/rest/latest/csarsgit" })
public class CsarGitController {
    @Inject
    private CsarGitService csarGitService;
    @Inject
    private CsarGitRepositoryService csarGitRepositoryService;

    /**
     * Create a new CsarGit in the system
     *
     * @param request The CsarGit to save in the system.
     * @return an the id of the created CsarGit {@link RestResponse}.
     */
    @ApiOperation(value = "Create a new CSARGit from a Git location in ALIEN.")
    @RequestMapping(method = RequestMethod.POST, consumes = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasAnyAuthority('ADMIN', 'COMPONENTS_MANAGER', 'ARCHITECT')")
    @Audit
    public RestResponse<String> create(@Valid @RequestBody CreateCsarGitRequest request) {
        String csarId = csarGitRepositoryService.create(request.getRepositoryUrl(), request.getUsername(), request.getPassword(), request.getImportLocations(),
                request.isStoredLocally());
        return RestResponseBuilder.<String> builder().data(csarId).build();
    }

    /**
     * Retrieve information on a repository from the system
     *
     * @param id The unique id of the CsarGit to retrieve.
     * @return The CsarGit matching the requested id.
     */
    @ApiOperation(value = "Retrieve information on a registered TOSCA CSAR git repository.")
    @RequestMapping(value = "/{id}", method = RequestMethod.GET)
    @PreAuthorize("hasAnyAuthority('ADMIN', 'COMPONENTS_MANAGER', 'ARCHITECT')")
    public RestResponse<CsarGitRepository> get(@ApiParam(value = "Id of the csar git repository to get", required = true) @PathVariable String id) {
        CsarGitRepository csargit = csarGitRepositoryService.getOrFail(id);
        return RestResponseBuilder.<CsarGitRepository> builder().data(csargit).build();
    }

    /**
     * Search for tosca csar git repositories.
     *
     * @param query The search query.
     * @param from The index from which to get data.
     * @param size The size of the query.
     * @return A rest response that contains the query result.
     */
    @ApiOperation(value = "Search for TOSCA CSAR git repositories.")
    @RequestMapping(method = RequestMethod.GET)
    @PreAuthorize("hasAnyAuthority('ADMIN', 'COMPONENTS_MANAGER', 'ARCHITECT')")
    public RestResponse<GetMultipleDataResult<CsarGitRepository>> search(@ApiParam(value = "Query text.") @RequestParam(required = false) String query,
            @ApiParam(value = "Query from the given index.") @RequestParam(required = false, defaultValue = "0") int from,
            @ApiParam(value = "Maximum number of results to retrieve.") @RequestParam(required = false, defaultValue = "20") int size) {
        GetMultipleDataResult<CsarGitRepository> result = csarGitRepositoryService.search(query, from, size);
        return RestResponseBuilder.<GetMultipleDataResult<CsarGitRepository>> builder().data(result).build();
    }

    /**
     * Delete information on a repository from the system
     *
     * @param id The unique id of the CsarGit to retrieve.
     * @return The CsarGit matching the requested id.
     */
    @ApiOperation(value = "Delete a registered TOSCA CSAR git repository.")
    @RequestMapping(value = "/{id}", method = RequestMethod.DELETE)
    @PreAuthorize("hasAnyAuthority('ADMIN', 'COMPONENTS_MANAGER', 'ARCHITECT')")
    @Audit
    public RestResponse<Void> delete(@ApiParam(value = "Id of the csar git repository to delete", required = true) @PathVariable String id) {
        csarGitService.delete(id);
        return RestResponseBuilder.<Void> builder().build();
    }

    /**
     * Update an existing CsarGit by id
     *
     * @param request The CsarGit data to update
     * @return an empty (void) rest {@link RestResponse}.
     */
    @ApiOperation(value = "Update a CSARGit by id.")
    @RequestMapping(value = "/{id}", method = RequestMethod.PUT, consumes = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasAnyAuthority('ADMIN', 'COMPONENTS_MANAGER', 'ARCHITECT')")
    @Audit
    public RestResponse<Void> update(@ApiParam(value = "Id of the csar git repository to delete", required = true) @PathVariable String id,
            @RequestBody CreateCsarGitRequest request) {
        csarGitRepositoryService.update(id, request.getRepositoryUrl(), request.getUsername(), request.getPassword(), request.getImportLocations(),
                request.isStoredLocally());
        return RestResponseBuilder.<Void> builder().build();
    }

    /**
     * Trigger the import the content of a repository into Alien 4 Cloud catalog.
     *
     * @param id The unique id of the CsarGit to retrieve.
     * @return The CsarGit matching the requested id.
     */
    @ApiOperation(value = "Specify a CSAR from Git and proceed to its import in Alien.")
    @RequestMapping(value = "/{id:.+}", method = RequestMethod.POST)
    @PreAuthorize("hasAnyAuthority('ADMIN', 'COMPONENTS_MANAGER', 'ARCHITECT')")
    @Audit
    public RestResponse<List<ParsingResult<Csar>>> importCsar(@Valid @PathVariable String id) {
        List<ParsingResult<Csar>> parsingResult = csarGitService.importFromGitRepository(id);
        RestError error = null;
        for (ParsingResult<Csar> result : parsingResult) {
            // check if there is any critical failure in the import
            if (result.hasError(ParsingErrorLevel.ERROR)) {
                error = RestErrorBuilder.builder(RestErrorCode.CSAR_PARSING_ERROR).build();
            }
        }
        return RestResponseBuilder.<List<ParsingResult<Csar>>> builder().error(error).data(parsingResult).build();
    }
}
