package alien4cloud.rest.repository;

import javax.annotation.Resource;
import javax.validation.Valid;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import alien4cloud.audit.annotation.Audit;
import alien4cloud.dao.model.FacetedSearchResult;
import alien4cloud.model.repository.Repository;
import alien4cloud.repository.services.RepositoryService;
import alien4cloud.rest.model.FilteredSearchRequest;
import alien4cloud.rest.model.RestResponse;
import alien4cloud.rest.model.RestResponseBuilder;
import alien4cloud.rest.repository.model.CreateRepositoryRequest;
import alien4cloud.rest.repository.model.UpdateRepositoryRequest;
import alien4cloud.utils.ReflectionUtil;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import io.swagger.annotations.Authorization;

@RestController
@RequestMapping(value = { "/rest/repositories", "/rest/v1/repositories", "/rest/latest/repositories" }, produces = MediaType.APPLICATION_JSON_VALUE)
@Api(value = "Repositories", description = "Allow to create/list/delete a repository.", authorizations = { @Authorization("COMPONENTS_MANAGER") })
public class RepositoryController {

    @Resource
    private RepositoryService repositoryService;

    @ApiOperation(value = "Search for repositories", authorizations = { @Authorization("COMPONENTS_MANAGER") })
    @RequestMapping(value = "/search", method = RequestMethod.POST, consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasAnyAuthority('ADMIN', 'COMPONENTS_MANAGER')")
    public RestResponse<FacetedSearchResult> search(@RequestBody FilteredSearchRequest searchRequest) {
        return RestResponseBuilder.<FacetedSearchResult> builder().data(repositoryService.search(searchRequest)).build();
    }

    @RequestMapping(method = RequestMethod.POST, consumes = MediaType.APPLICATION_JSON_VALUE)
    @ApiOperation(value = "Create a new repository.", authorizations = { @Authorization("COMPONENTS_MANAGER") })
    @ResponseStatus(value = HttpStatus.CREATED)
    @PreAuthorize("hasAnyAuthority('ADMIN', 'COMPONENTS_MANAGER')")
    @Audit
    public RestResponse<String> create(@ApiParam(value = "Create repository", required = true) @Valid @RequestBody CreateRepositoryRequest createRequest) {
        String createdRepoId = repositoryService.createRepositoryConfiguration(createRequest.getName(), createRequest.getPluginId(),
                createRequest.getConfiguration());
        return RestResponseBuilder.<String> builder().data(createdRepoId).build();
    }

    @RequestMapping(value = "/{id}", method = RequestMethod.PUT, consumes = MediaType.APPLICATION_JSON_VALUE)
    @ApiOperation(value = "Update the repository.", authorizations = { @Authorization("COMPONENTS_MANAGER") })
    @PreAuthorize("hasAnyAuthority('ADMIN', 'COMPONENTS_MANAGER')")
    @Audit
    public RestResponse<Void> update(@ApiParam(value = "Id of the repository to update", required = true) @PathVariable String id,
            @ApiParam(value = "Request for repository update", required = true) @Valid @RequestBody UpdateRepositoryRequest updateRequest) {
        Repository repository = repositoryService.getOrFail(id);
        String oldName = repository.getName();
        ReflectionUtil.mergeObject(updateRequest, repository);
        repositoryService.updateRepository(repository, oldName, updateRequest.getConfiguration() != null);
        return RestResponseBuilder.<Void> builder().build();
    }

    @RequestMapping(value = "/{id}", method = RequestMethod.DELETE)
    @ApiOperation(value = "Delete a repository.", authorizations = { @Authorization("COMPONENTS_MANAGER") })
    @PreAuthorize("hasAnyAuthority('ADMIN', 'COMPONENTS_MANAGER')")
    @Audit
    public RestResponse<Void> delete(@ApiParam(value = "Id of the repository to update", required = true) @PathVariable String id) {
        repositoryService.getOrFail(id);
        repositoryService.delete(id);
        return RestResponseBuilder.<Void> builder().build();
    }
}
