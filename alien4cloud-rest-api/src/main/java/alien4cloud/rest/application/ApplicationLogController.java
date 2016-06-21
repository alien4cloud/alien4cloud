package alien4cloud.rest.application;

import javax.annotation.Resource;

import org.elasticsearch.index.query.FilterBuilders;
import org.elasticsearch.index.query.RangeFilterBuilder;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import alien4cloud.application.ApplicationEnvironmentService;
import alien4cloud.application.ApplicationService;
import alien4cloud.dao.IGenericSearchDAO;
import alien4cloud.dao.model.FacetedSearchResult;
import alien4cloud.model.application.Application;
import alien4cloud.model.application.ApplicationEnvironment;
import alien4cloud.paas.model.PaaSDeploymentLog;
import alien4cloud.rest.application.model.SearchLogRequest;
import alien4cloud.rest.model.RestResponse;
import alien4cloud.rest.model.RestResponseBuilder;
import alien4cloud.security.AuthorizationUtil;
import alien4cloud.security.model.ApplicationEnvironmentRole;
import alien4cloud.security.model.ApplicationRole;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;

@RestController
@RequestMapping({ "/rest/applications/{applicationId}/environments/{applicationEnvironmentId}/logs",
        "/rest/v1/applications/{applicationId}/environments/{applicationEnvironmentId}/logs",
        "/rest/latest/applications/{applicationId}/environments/{applicationEnvironmentId}/logs" })
@Api(value = "", description = "Manages application's deploymeng logs")
public class ApplicationLogController {

    @Resource(name = "alien-monitor-es-dao")
    private IGenericSearchDAO alienMonitorDao;
    @Resource
    private ApplicationService applicationService;
    @Resource
    private ApplicationEnvironmentService applicationEnvironmentService;

    /**
     * Search application.
     *
     * @param searchRequest The element that contains criterias for search operation.
     * @return A rest response that contains a {@link FacetedSearchResult} containing applications.
     */
    @ApiOperation(value = "Search for applications", notes = "Returns a search result with that contains applications matching the request. A application is returned only if the connected user has at least one application role in [ APPLICATION_MANAGER | APPLICATION_USER | APPLICATION_DEVOPS | DEPLOYMENT_MANAGER ]")
    @RequestMapping(value = "/search", method = RequestMethod.POST, consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("isAuthenticated()")
    public RestResponse<FacetedSearchResult> search(@PathVariable String applicationId, @PathVariable String applicationEnvironmentId,
            @RequestBody SearchLogRequest searchRequest) {
        ApplicationEnvironment environment = applicationEnvironmentService.getEnvironmentByIdOrDefault(applicationId, applicationEnvironmentId);
        Application application = applicationService.checkAndGetApplication(applicationId);
        if (!AuthorizationUtil.hasAuthorizationForApplication(application, ApplicationRole.APPLICATION_MANAGER)) {
            AuthorizationUtil.checkAuthorizationForEnvironment(environment, ApplicationEnvironmentRole.DEPLOYMENT_MANAGER);
        }

        RangeFilterBuilder dateRangeBuilder = null;
        if (searchRequest.getFromDate() != null || searchRequest.getToDate() != null) {
            dateRangeBuilder = FilterBuilders.rangeFilter("timestamp");
            if (searchRequest.getFromDate() != null) {
                dateRangeBuilder.from(searchRequest.getFromDate());
            }
            if (searchRequest.getToDate() != null) {
                dateRangeBuilder.to(searchRequest.getToDate());
            }
        }

        String sortBy = "timestamp";
        boolean ascending = true;
        if (searchRequest.getSortConfiguration() != null) {
            sortBy = searchRequest.getSortConfiguration().getSortBy();
            ascending = searchRequest.getSortConfiguration().isAscending();
        }
        FacetedSearchResult searchResult = alienMonitorDao.facetedSearch(PaaSDeploymentLog.class, searchRequest.getQuery(), searchRequest.getFilters(),
                dateRangeBuilder, null, searchRequest.getFrom(), searchRequest.getSize(), sortBy, !ascending);
        return RestResponseBuilder.<FacetedSearchResult> builder().data(searchResult).build();
    }
}
