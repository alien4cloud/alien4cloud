package alien4cloud.rest.application;

import java.util.List;
import java.util.Map;

import javax.annotation.Resource;
import javax.validation.Valid;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import alien4cloud.application.ApplicationService;
import alien4cloud.application.ApplicationVersionService;
import alien4cloud.cloud.CloudService;
import alien4cloud.cloud.DeploymentService;
import alien4cloud.dao.IGenericSearchDAO;
import alien4cloud.dao.model.FacetedSearchResult;
import alien4cloud.dao.model.GetMultipleDataResult;
import alien4cloud.model.application.Application;
import alien4cloud.model.application.ApplicationVersion;
import alien4cloud.paas.model.DeploymentStatus;
import alien4cloud.rest.component.SearchRequest;
import alien4cloud.rest.model.RestErrorBuilder;
import alien4cloud.rest.model.RestErrorCode;
import alien4cloud.rest.model.RestResponse;
import alien4cloud.rest.model.RestResponseBuilder;
import alien4cloud.security.ApplicationRole;
import alien4cloud.security.AuthorizationUtil;
import alien4cloud.security.Role;
import alien4cloud.utils.MapUtil;
import alien4cloud.utils.ReflectionUtil;

import com.google.common.collect.Lists;
import com.wordnik.swagger.annotations.Api;
import com.wordnik.swagger.annotations.ApiOperation;

@RestController
@RequestMapping("/rest/applications/{applicationId:.+}/versions")
@Api(value = "", description = "Manages application's versions")
public class ApplicationVersionController {

    @Resource(name = "alien-es-dao")
    private IGenericSearchDAO alienDAO;
    @Resource
    private ApplicationVersionService applicationVersionService;
    @Resource
    private CloudService cloudService;
    @Resource
    private ApplicationService applicationService;
    @Resource
    private DeploymentService deploymentService;

    /**
     * Get all application versions for an application
     *
     * @param applicationId The application id.
     */
    @ApiOperation(value = "Get all application vesions for an application", notes = "Return all application vesions for one application. Application role required [ APPLICATION_MANAGER | APPLICATION_USER | APPLICATION_DEVOPS | DEPLOYMENT_MANAGER ]")
    @RequestMapping(method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    public RestResponse<ApplicationVersion> get(@PathVariable String applicationId) {
        Application application = alienDAO.findById(Application.class, applicationId);
        AuthorizationUtil.checkAuthorizationForApplication(application, ApplicationRole.values());
        ApplicationVersion[] versions = applicationVersionService.getByApplicationId(applicationId);
        return RestResponseBuilder.<ApplicationVersion> builder().data(versions[0]).build();
    }

    /**
     * Search for application version for a given application id
     * 
     * @param applicationId the targeted application id
     * @param searchRequest
     * @return A rest response that contains a {@link FacetedSearchResult} containing application versions for an application id
     */
    @ApiOperation(value = "Search for application versions", notes = "Returns a search result with that contains application versions matching the request. A application version is returned only if the connected user has at least one application role in [ APPLICATION_MANAGER | APPLICATION_USER | APPLICATION_DEVOPS | DEPLOYMENT_MANAGER ]")
    @RequestMapping(value = "/search", method = RequestMethod.POST, consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public RestResponse<GetMultipleDataResult> search(@PathVariable String applicationId, @RequestBody SearchRequest searchRequest) {
        GetMultipleDataResult<ApplicationVersion> searchResult = alienDAO.search(ApplicationVersion.class, searchRequest.getQuery(),
                getApplicationVersionsFilters(applicationId), searchRequest.getFrom(), searchRequest.getSize());
        return RestResponseBuilder.<GetMultipleDataResult> builder().data(searchResult).build();
    }

    /**
     * Get application version from it's id
     *
     * @param applicationId The application id
     */
    @ApiOperation(value = "Get an application based from its id.", notes = "Returns the application details. Application role required [ APPLICATION_MANAGER | APPLICATION_USER | APPLICATION_DEVOPS | DEPLOYMENT_MANAGER ]")
    @RequestMapping(value = "/{applicationEnvironmentId:.+}", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    public RestResponse<ApplicationVersion> getApplicationEnvironment(@PathVariable String applicationId, @PathVariable String applicationEnvironmentId) {
        Application application = alienDAO.findById(Application.class, applicationId);
        AuthorizationUtil.checkAuthorizationForApplication(application, ApplicationRole.values());
        ApplicationVersion applicationEnvironment = alienDAO.findById(ApplicationVersion.class, applicationEnvironmentId);
        return RestResponseBuilder.<ApplicationVersion> builder().data(applicationEnvironment).build();
    }

    /**
     * Create a new application version for an application
     * 
     * @param request data to create an application environment
     * @return application environment id
     */
    @ApiOperation(value = "Create a new application version.", notes = "If successfull returns a rest response with the id of the created application version in data. If not successful a rest response with an error content is returned. Role required [ APPLICATIONS_MANAGER ]. "
            + "By default the application version creator will have application roles [APPLICATION_MANAGER, DEPLOYMENT_MANAGER]")
    @RequestMapping(method = RequestMethod.POST, consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseStatus(value = HttpStatus.CREATED)
    public RestResponse<String> create(@Valid @RequestBody UpdateApplicationVersionRequest request) {
        AuthorizationUtil.checkHasOneRoleIn(Role.APPLICATIONS_MANAGER);
        ApplicationVersion appVersion = null;
        Application application = alienDAO.findById(Application.class, request.getApplicationId());
        if (application != null) {
            AuthorizationUtil.hasAuthorizationForApplication(application, ApplicationRole.APPLICATION_MANAGER);
            appVersion = applicationVersionService.createApplicationVersion(request.getApplicationId(), null);
        } else {
            // no application found to create a version
            return RestResponseBuilder
                    .<String> builder()
                    .data(null)
                    .error(RestErrorBuilder.builder(RestErrorCode.APPLICATION_ENVIRONMENT_ERROR)
                            .message("Application with id <" + request.getApplicationId() + "> could not be found to create a new application version").build())
                    .build();
        }
        return RestResponseBuilder.<String> builder().data(appVersion.getId()).build();
    }

    /**
     * Update application version
     * 
     * @param applicationVersionId
     * @param request
     * @return
     */
    @ApiOperation(value = "Updates by merging the given request into the given application environment", notes = "The logged-in user must have the application manager role for this application. Application role required [ APPLICATION_MANAGER ]")
    @RequestMapping(value = "/{applicationEnvironmentId:.+}", method = RequestMethod.PUT, consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public RestResponse<Void> update(@PathVariable String applicationVersionId, @RequestBody UpdateApplicationVersionRequest request) {
        // check application version id
        ApplicationVersion appVersion = alienDAO.findById(ApplicationVersion.class, applicationVersionId);
        if (appVersion != null) {
            // check application
            Application application = alienDAO.findById(Application.class, appVersion.getApplicationId());
            if (application != null) {
                ReflectionUtil.mergeObject(request, appVersion);
                alienDAO.save(appVersion);
            } else {
                // linked application id not found
                return RestResponseBuilder
                        .<Void> builder()
                        .data(null)
                        .error(RestErrorBuilder.builder(RestErrorCode.APPLICATION_ENVIRONMENT_ERROR)
                                .message("Application with id <" + appVersion.getApplicationId() + "> could not be found to update an environment").build())
                        .build();
            }
        } else {
            // no application found to create a version
            return RestResponseBuilder
                    .<Void> builder()
                    .data(null)
                    .error(RestErrorBuilder.builder(RestErrorCode.APPLICATION_ENVIRONMENT_ERROR)
                            .message("Application version with id <" + applicationVersionId + "> does not exist").build()).build();
        }
        return RestResponseBuilder.<Void> builder().build();
    }

    /**
     * Delete an application environment based on it's id. Should not be able to delete a deployed version.
     * 
     * @param applicationVersionId
     * @return
     */
    @ApiOperation(value = "Delete an application version from its id", notes = "The logged-in user must have the application manager role for this application. Application role required [ APPLICATION_MANAGER ]")
    @RequestMapping(value = "/{applicationVersionId:.+}", method = RequestMethod.DELETE, produces = MediaType.APPLICATION_JSON_VALUE)
    public RestResponse<Boolean> delete(@PathVariable String applicationId, @PathVariable String applicationVersionId) {
        Application application = applicationService.getOrFail(applicationId);
        AuthorizationUtil.checkAuthorizationForApplication(application, ApplicationRole.APPLICATION_MANAGER);
        if (deploymentService.getDeployment(applicationId).equals(DeploymentStatus.UNDEPLOYED)) {
            applicationVersionService.delete(applicationVersionId);
            return RestResponseBuilder.<Boolean> builder().data(true).build();
        } else {
            // we can't delete an application version if this version is deployed
            return RestResponseBuilder
                    .<Boolean> builder()
                    .data(false)
                    .error(RestErrorBuilder.builder(RestErrorCode.APPLICATION_ENVIRONMENT_ERROR)
                            .message("Application version with id <" + applicationVersionId + "> could not be found deleted beacause it's used").build())
                    .build();
        }
    }

    /**
     * Filter to search app versions only for an application id
     * 
     * @param applicationId
     * @return
     */
    private Map<String, String[]> getApplicationVersionsFilters(String applicationId) {
        List<String> filterKeys = Lists.newArrayList();
        List<String[]> filterValues = Lists.newArrayList();
        if (applicationId != null) {
            filterKeys.add("applicationId");
            filterValues.add(new String[] { applicationId });
        }
        return MapUtil.newHashMap(filterKeys.toArray(new String[filterKeys.size()]), filterValues.toArray(new String[filterValues.size()][]));
    }

}
