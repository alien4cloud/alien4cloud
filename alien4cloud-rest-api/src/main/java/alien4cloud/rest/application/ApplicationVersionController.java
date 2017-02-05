package alien4cloud.rest.application;

import javax.annotation.Resource;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import alien4cloud.application.ApplicationService;
import alien4cloud.application.ApplicationVersionService;
import alien4cloud.audit.annotation.Audit;
import alien4cloud.dao.IGenericSearchDAO;
import alien4cloud.dao.model.FacetedSearchResult;
import alien4cloud.dao.model.GetMultipleDataResult;
import alien4cloud.model.application.Application;
import alien4cloud.model.application.ApplicationVersion;
import alien4cloud.rest.application.model.CreateApplicationVersionRequest;
import alien4cloud.rest.application.model.UpdateApplicationVersionRequest;
import alien4cloud.rest.model.FilteredSearchRequest;
import alien4cloud.rest.model.RestResponse;
import alien4cloud.rest.model.RestResponseBuilder;
import alien4cloud.security.AuthorizationUtil;
import alien4cloud.security.model.ApplicationRole;
import alien4cloud.topology.TopologyServiceCore;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;

@RestController
@RequestMapping({ "/rest/applications/{applicationId:.+}/versions", "/rest/v1/applications/{applicationId:.+}/versions",
        "/rest/latest/applications/{applicationId:.+}/versions" })
@Api(value = "", description = "Manages application's versions")
public class ApplicationVersionController {
    @Resource(name = "alien-es-dao")
    private IGenericSearchDAO alienDAO;
    @Resource
    private ApplicationVersionService appVersionService;
    @Resource
    private ApplicationService applicationService;
    @Resource
    private TopologyServiceCore topologyServiceCore;

    /**
     * Get most recent snapshot application version for an application
     *
     * @param applicationId The application id.
     */
    @ApiOperation(value = "Get the first snapshot application version for an application.", notes = "Return the first snapshot application version for an application. Application role required [ APPLICATION_MANAGER | APPLICATION_USER | APPLICATION_DEVOPS | DEPLOYMENT_MANAGER ]")
    @RequestMapping(method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("isAuthenticated()")
    public RestResponse<ApplicationVersion> get(@PathVariable String applicationId) {
        Application application = alienDAO.findById(Application.class, applicationId);
        AuthorizationUtil.checkAuthorizationForApplication(application, ApplicationRole.values());
        return RestResponseBuilder.<ApplicationVersion> builder().data(appVersionService.getLatestSnapshot(applicationId)).build();
    }

    /**
     * Search application versions for a given application id
     *
     * @param applicationId the targeted application id
     * @param searchRequest
     * @return A rest response that contains a {@link FacetedSearchResult} containing application versions for an application id sorted by version
     */
    @ApiOperation(value = "Search application versions", notes = "Returns a search result with that contains application versions matching the request. A application version is returned only if the connected user has at least one application role in [ APPLICATION_MANAGER | APPLICATION_USER | APPLICATION_DEVOPS | DEPLOYMENT_MANAGER ]")
    @RequestMapping(value = "/search", method = RequestMethod.POST, consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("isAuthenticated()")
    public RestResponse<GetMultipleDataResult<ApplicationVersion>> search(@PathVariable String applicationId,
            @RequestBody FilteredSearchRequest searchRequest) {
        Application application = applicationService.getOrFail(applicationId);
        AuthorizationUtil.checkAuthorizationForApplication(application, ApplicationRole.values());
        GetMultipleDataResult<ApplicationVersion> searchResult = appVersionService.search(applicationId, searchRequest.getQuery(), searchRequest.getFrom(),
                searchRequest.getSize());
        return RestResponseBuilder.<GetMultipleDataResult<ApplicationVersion>> builder().data(searchResult).build();
    }

    /**
     * Get application version from it's id
     *
     * @param applicationId The application id
     */
    @ApiOperation(value = "Get an application version based from its id.", notes = "Returns the application version details. Application role required [ APPLICATION_MANAGER | APPLICATION_DEVOPS ]")
    @RequestMapping(value = "/{applicationVersionId:.+}", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("isAuthenticated()")
    public RestResponse<ApplicationVersion> get(@PathVariable String applicationId, @PathVariable String applicationVersionId) {
        Application application = applicationService.getOrFail(applicationId);
        AuthorizationUtil.checkAuthorizationForApplication(application, ApplicationRole.values());
        ApplicationVersion applicationVersion = appVersionService.getOrFail(applicationVersionId);
        return RestResponseBuilder.<ApplicationVersion> builder().data(applicationVersion).build();
    }

    /**
     * Create a new application version for an application.
     *
     * @param request data to create an application environment
     * @return application environment id
     */
    @ApiOperation(value = "Create a new application version.", notes = "If successfull returns a rest response with the id of the created application version in data. If not successful a rest response with an error content is returned. Application role required [ APPLICATIONS_MANAGER ]. "
            + "By default the application version creator will have application roles [APPLICATION_MANAGER]")
    @RequestMapping(method = RequestMethod.POST, consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseStatus(value = HttpStatus.CREATED)
    @PreAuthorize("isAuthenticated()")
    @Audit
    public RestResponse<String> create(@PathVariable String applicationId, @RequestBody CreateApplicationVersionRequest request) {
        Application application = applicationService.getOrFail(applicationId);
        AuthorizationUtil.checkAuthorizationForApplication(application, ApplicationRole.APPLICATION_MANAGER);
        String originalId = request.getTopologyTemplateId();
        boolean originalIsAppVersion = false;
        if (originalId == null) {
            originalId = request.getFromVersionId();
            originalIsAppVersion = true;
        } else if (request.getFromVersionId() != null) {
            throw new IllegalArgumentException("topologyTemplateId and fromVersionId are mutually exclusive.");
        }
        ApplicationVersion appVersion = appVersionService.createApplicationVersion(applicationId, request.getVersion(), request.getDescription(), originalId,
                originalIsAppVersion);
        return RestResponseBuilder.<String> builder().data(appVersion.getId()).build();
    }

    /**
     * Update application version
     *
     * @param applicationId The id of the application for which to update a version.
     * @param applicationVersionId The id of the application version.
     * @param request The update request that eventually contains a new name and description.
     * @return A void rest response with no error.
     */
    @ApiOperation(value = "Updates by merging the given request into the given application version", notes = "Updates by merging the given request into the given application version. Application role required [ APPLICATION_MANAGER ]")
    @RequestMapping(value = "/{applicationVersionId:.+}", method = RequestMethod.PUT, consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("isAuthenticated()")
    @Audit
    public RestResponse<Void> update(@PathVariable String applicationId, @PathVariable String applicationVersionId,
            @RequestBody UpdateApplicationVersionRequest request) {
        Application application = applicationService.getOrFail(applicationId);
        AuthorizationUtil.checkAuthorizationForApplication(application, ApplicationRole.APPLICATION_MANAGER);

        appVersionService.update(applicationId, applicationVersionId, request.getVersion(), request.getDescription());

        return RestResponseBuilder.<Void> builder().build();
    }

    /**
     * Delete an application environment based on it's id. Should not be able to delete a deployed version.
     *
     * @param applicationId
     * @param applicationVersionId
     * @return boolean is delete
     */
    @ApiOperation(value = "Delete an application version from its id", notes = "The logged-in user must have the application manager role for this application. Application role required [ APPLICATION_MANAGER ]")
    @RequestMapping(value = "/{applicationVersionId:.+}", method = RequestMethod.DELETE, produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("isAuthenticated()")
    @Audit
    public RestResponse<Boolean> delete(@PathVariable String applicationId, @PathVariable String applicationVersionId) {
        Application application = applicationService.getOrFail(applicationId);
        AuthorizationUtil.checkAuthorizationForApplication(application, ApplicationRole.APPLICATION_MANAGER);
        appVersionService.delete(applicationVersionId);
        return RestResponseBuilder.<Boolean> builder().data(true).build();
    }
}
