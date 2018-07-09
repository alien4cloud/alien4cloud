package alien4cloud.rest.deployment;

import alien4cloud.dao.IGenericSearchDAO;
import alien4cloud.dao.model.FacetedSearchResult;
import alien4cloud.deployment.ExecutionService;
import alien4cloud.deployment.TaskService;
import alien4cloud.model.runtime.Task;
import alien4cloud.rest.model.RestResponse;
import alien4cloud.rest.model.RestResponseBuilder;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;

@RestController
@RequestMapping({ "/rest/tasks", "/rest/v1/tasks", "/rest/latest/tasks" })
@Api(value = "", description = "Operations on Tasks")
public class TaskController {
    @Resource(name = "alien-es-dao")
    private IGenericSearchDAO alienDAO;
    @Resource
    private TaskService taskService;

    /**
     * Search for {@link Task}s.
     *
     * @return A rest response that contains a {@link FacetedSearchResult} containing {@link Task}s.
     */
    @ApiOperation(value = "Search for tasks", notes = "Returns a search result with that contains tasks matching the request.")
    @RequestMapping(value = "/search", method = RequestMethod.GET)
    @PreAuthorize("isAuthenticated()")
    public RestResponse<FacetedSearchResult<Task>> search(
            @ApiParam(value = "Query text.") @RequestParam(required = false) String query,
            @ApiParam(value = "Query from the given index.") @RequestParam(required = false, defaultValue = "0") int from,
            @ApiParam(value = "Maximum number of results to retrieve.") @RequestParam(required = false, defaultValue = "50") int size,
            @ApiParam(value = "Id of the execution for which to get tasks. If not provided, get tasks for all executions") @RequestParam(required = false) String executionId
            ) {
        FacetedSearchResult<Task> searchResult = taskService.searchTasks(query, executionId, from, size);
        return RestResponseBuilder.<FacetedSearchResult<Task>> builder().data(searchResult).build();
    }

}