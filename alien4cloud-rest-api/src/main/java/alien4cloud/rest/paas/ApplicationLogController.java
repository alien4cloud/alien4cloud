package alien4cloud.rest.paas;

import alien4cloud.application.ApplicationEnvironmentService;
import alien4cloud.application.ApplicationService;
import alien4cloud.dao.model.FacetedSearchResult;
import alien4cloud.deployment.DeploymentService;
import alien4cloud.model.application.Application;
import alien4cloud.model.application.ApplicationEnvironment;
import alien4cloud.model.deployment.Deployment;
import alien4cloud.paas.model.PaaSDeploymentLog;
import alien4cloud.rest.application.model.SearchLogRequest;
import alien4cloud.rest.paas.services.LogService;
import alien4cloud.rest.model.RestResponse;
import alien4cloud.rest.model.RestResponseBuilder;
import alien4cloud.security.AuthorizationUtil;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.inject.Inject;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping({ "/rest/applications/{applicationId}", "/rest/v1/applications/{applicationId}", "/rest/latest/applications/{applicationId}" })
@Api(value = "", description = "Manages application's deployment logs")
public class ApplicationLogController {
    @Resource
    private ApplicationService applicationService;
    @Resource
    private ApplicationEnvironmentService applicationEnvironmentService;
    @Resource
    private DeploymentService deploymentService;
    @Inject
    private LogService logService;

    /**
     * Search logs for all deployments of a given application.
     *
     * @param searchRequest The element that contains criterias for search operation.
     * @return A rest response that contains a {@link FacetedSearchResult} containing applications.
     */
    @ApiOperation(value = "Search for logs of all deployments for a given application", notes = "Returns a search result with that contains logs matching the request.")
    @RequestMapping(value = "/logs/search", method = RequestMethod.POST, consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("isAuthenticated()")
    public RestResponse<FacetedSearchResult<PaaSDeploymentLog>> search(@PathVariable String applicationId, @RequestBody SearchLogRequest searchRequest) {
        ApplicationEnvironment[] allEnvironments = applicationEnvironmentService.getByApplicationId(applicationId);
        Application application = applicationService.checkAndGetApplication(applicationId);

        ArrayList<String> allDeploymentId = new ArrayList<>();
        for (ApplicationEnvironment environment : allEnvironments) {
            if (AuthorizationUtil.hasAuthorizationForEnvironment(application, environment)) {
                Deployment deployment = deploymentService.getDeployment(environment.getId());
                if (deployment != null) {
                    allDeploymentId.add(deployment.getId());
                }
            }
        }

        if (allDeploymentId.isEmpty()) {
            return RestResponseBuilder.<FacetedSearchResult<PaaSDeploymentLog>> builder().data(new FacetedSearchResult()).build();
        }

        Map<String, String[]> filters = searchRequest.getFilters();
        if (filters == null) {
            filters = new HashMap<>();
            searchRequest.setFilters(filters);
        }

        filters.put("deploymentId", allDeploymentId.toArray(new String[allDeploymentId.size()]));
        return logService.doSearch(searchRequest);
    }

    /**
     * Search logs for a given deployment.
     *
     * @param searchRequest The element that contains criterias for search operation.
     * @return A rest response that contains a {@link FacetedSearchResult} containing applications.
     */
    @ApiOperation(value = "Search for logs of a given deployment", notes = "Returns a search result with that contains logs matching the request. ")
    @RequestMapping(value = "/environments/{applicationEnvironmentId}/logs/search", method = RequestMethod.POST, consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("isAuthenticated()")
    public RestResponse<FacetedSearchResult<PaaSDeploymentLog>> search(@PathVariable String applicationId, @PathVariable String applicationEnvironmentId,
            @RequestBody SearchLogRequest searchRequest) {
        ApplicationEnvironment environment = applicationEnvironmentService.getEnvironmentByIdOrDefault(applicationId, applicationEnvironmentId);
        AuthorizationUtil.checkAuthorizationForEnvironment(applicationService.getOrFail(applicationId), environment);
        Deployment deployment = deploymentService.getDeployment(applicationEnvironmentId);
        if (deployment == null) {
            return RestResponseBuilder.<FacetedSearchResult<PaaSDeploymentLog>> builder().data(new FacetedSearchResult()).build();
        }

        Map<String, String[]> filters = searchRequest.getFilters();
        if (filters == null) {
            filters = new HashMap<>();
            searchRequest.setFilters(filters);
        }
        filters.put("deploymentId", new String[] { deployment.getId() });
        return logService.doSearch(searchRequest);
    }
}
