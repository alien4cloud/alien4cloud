package alien4cloud.rest.deployment;

import alien4cloud.application.ApplicationService;
import alien4cloud.dao.IGenericSearchDAO;
import alien4cloud.dao.model.FacetedSearchResult;
import alien4cloud.deployment.*;
import alien4cloud.orchestrators.locations.services.LocationService;
import alien4cloud.rest.model.*;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.inject.Inject;

@RestController
@RequestMapping({ "/rest/executions", "/rest/v1/executions", "/rest/latest/executions" })
@Api(value = "", description = "Operations on Executions")
public class ExecutionController {
    @Resource(name = "alien-es-dao")
    private IGenericSearchDAO alienDAO;
    @Resource
    private ExecutionService executionService;

    /**
     * Search for executions
     *
     * @return A rest response that contains a {@link FacetedSearchResult} containing executions.
     */
    @ApiOperation(value = "Search for executions", notes = "Returns a search result with that contains executions matching the request.")
    @RequestMapping(value = "/search", method = RequestMethod.GET)
    @PreAuthorize("isAuthenticated()")
    public RestResponse<FacetedSearchResult> search(
            @ApiParam(value = "Query text.") @RequestParam(required = false) String query,
            @ApiParam(value = "Query from the given index.") @RequestParam(required = false, defaultValue = "0") int from,
            @ApiParam(value = "Maximum number of results to retrieve.") @RequestParam(required = false, defaultValue = "50") int size,
            @ApiParam(value = "Id of the deployment for which to get executions. If not provided, get executions for all deployments") @RequestParam(required = false) String deploymentId
            ) {
        FacetedSearchResult searchResult = executionService.searchExecutions(query, deploymentId, from, size);
        return RestResponseBuilder.<FacetedSearchResult> builder().data(searchResult).build();
    }

}