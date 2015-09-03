package alien4cloud.rest.csar;

import alien4cloud.audit.annotation.Audit;
import alien4cloud.component.repository.exception.CSARVersionAlreadyExistsException;
import alien4cloud.csar.services.CsarGitRepositoryService;
import alien4cloud.csar.services.CsarGitService;
import alien4cloud.dao.model.GetMultipleDataResult;
import alien4cloud.exception.NotFoundException;
import alien4cloud.model.components.Csar;
import alien4cloud.rest.model.RestResponse;
import alien4cloud.rest.model.RestResponseBuilder;
import alien4cloud.security.model.CsarGitRepository;
import alien4cloud.tosca.parser.ParsingException;
import alien4cloud.tosca.parser.ParsingResult;
import com.wordnik.swagger.annotations.ApiOperation;
import com.wordnik.swagger.annotations.ApiParam;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.api.errors.NoHeadException;
import org.eclipse.jgit.errors.RevisionSyntaxException;
import org.hibernate.validator.constraints.NotBlank;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import javax.inject.Inject;
import javax.validation.Valid;
import java.io.IOException;
import java.util.List;

@RestController
@RequestMapping(value = "/rest/csarsgit")
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
     * @return The CsarGit matching the requested id or url.
     */
    @ApiOperation(value = "Retrieve information on a registered TOSCA CSAR git repository.")
    @RequestMapping(value = "/{id}", method = RequestMethod.GET)
    @PreAuthorize("hasAnyAuthority('ADMIN', 'COMPONENTS_MANAGER', 'ARCHITECT')")
    public RestResponse<CsarGitRepository> get(@ApiParam(value = "Id of the csar git repository to get", required = true) @PathVariable String id) {
        CsarGitRepository csargit = csarGitRepositoryService.getOrFail(id);
        return RestResponseBuilder.<CsarGitRepository> builder().data(csargit).build();
    }

    /**
     * Retrieve a CsarGit from the system by it's url.
     *
     * @param url The unique url of the CsarGit to retrieve.
     * @return The CsarGit matching the requested id or url.
     */
    @ApiOperation(value = "Retrieve information on a registered TOSCA CSAR git repository using the csar url as id.")
    @RequestMapping(method = RequestMethod.GET, consumes = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasAnyAuthority('ADMIN', 'COMPONENTS_MANAGER', 'ARCHITECT')")
    public RestResponse<CsarGitRepository> getByUrl(
            @ApiParam(value = "Url of the csar git repository to get", required = true) @Valid @NotBlank @RequestBody String url) {
        CsarGitRepository csargit = csarGitRepositoryService.getCsargitByUrl(url);
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
     * @return The CsarGit matching the requested id or url.
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
     * Delete a CsarGit from the system by it's url.
     *
     * @param url The unique url of the CsarGit to retrieve.
     * @return The CsarGit matching the requested id or url.
     */
    @ApiOperation(value = "Delete a registered TOSCA CSAR git repository using the csar url as id.")
    @RequestMapping(method = RequestMethod.DELETE, consumes = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasAnyAuthority('ADMIN', 'COMPONENTS_MANAGER', 'ARCHITECT')")
    @Audit
    public RestResponse<Void> deleteByUrl(
            @ApiParam(value = "Url of the csar git repository to delete", required = true) @Valid @NotBlank @RequestBody String url) {
        csarGitService.delete(url);
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
            @RequestBody UpdateCsarGitRequest request) {
        csarGitRepositoryService.update(id, request.getRepositoryUrl(), request.getUsername(), request.getPassword());
        return RestResponseBuilder.<Void> builder().build();
    }

    /**
     * Update an existing CsarGit by url
     *
     * @param request The CsarGit data to update
     * @return an empty (void) rest {@link RestResponse}.
     */
    @ApiOperation(value = "Update a CSARGit by url.")
    @RequestMapping(value = "/update/{url}", method = RequestMethod.POST)
    @PreAuthorize("hasAnyAuthority('ADMIN', 'COMPONENTS_MANAGER', 'ARCHITECT')")
    @Audit
    public RestResponse<Void> updateByUrl(@Valid @RequestBody UpdateCsarGitWithUrlRequest request) {
        csarGitRepositoryService.update(request.getRepositoryUrl(), request.getRepositoryUrl(), request.getUsername(), request.getPassword());
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
    public RestResponse<List<ParsingResult<Csar>>> importCsar(@Valid @RequestBody String id) {
        List<ParsingResult<Csar>> parsingResult = csarGitService.importFromGitRepository(id);
        return RestResponseBuilder.<List<ParsingResult<Csar>>> builder().data(parsingResult).build();
    }
}