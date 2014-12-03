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

import alien4cloud.application.ApplicationEnvironmentService;
import alien4cloud.application.ApplicationService;
import alien4cloud.cloud.CloudService;
import alien4cloud.dao.IGenericSearchDAO;
import alien4cloud.dao.model.FacetedSearchResult;
import alien4cloud.dao.model.GetMultipleDataResult;
import alien4cloud.exception.InvalidArgumentException;
import alien4cloud.model.application.Application;
import alien4cloud.model.application.ApplicationEnvironment;
import alien4cloud.model.cloud.Cloud;
import alien4cloud.rest.component.SearchRequest;
import alien4cloud.rest.model.RestErrorBuilder;
import alien4cloud.rest.model.RestErrorCode;
import alien4cloud.rest.model.RestResponse;
import alien4cloud.rest.model.RestResponseBuilder;
import alien4cloud.security.ApplicationRole;
import alien4cloud.security.AuthorizationUtil;
import alien4cloud.security.CloudRole;
import alien4cloud.security.Role;
import alien4cloud.utils.MapUtil;
import alien4cloud.utils.ReflectionUtil;
import alien4cloud.utils.services.ResourceRoleService;

import com.google.common.collect.Lists;
import com.wordnik.swagger.annotations.Api;
import com.wordnik.swagger.annotations.ApiOperation;

@RestController
@RequestMapping("/rest/applications/{applicationId:.+}/environments")
@Api(value = "", description = "Manages application's environments")
public class ApplicationEnvironmentController {

    @Resource(name = "alien-es-dao")
    private IGenericSearchDAO alienDAO;
    @Resource
    private ApplicationEnvironmentService applicationEnvironmentService;
    @Resource
    private CloudService cloudService;
    @Resource
    private ApplicationService applicationService;
    @Resource
    private ResourceRoleService resourceRoleService;

    /**
     * Get all application environment for an application
     *
     * @param applicationId The application id.
     */
    @ApiOperation(value = "Get all application environments for an application", notes = "Return all application environments for one application. Application role required [ APPLICATION_MANAGER | APPLICATION_USER | APPLICATION_DEVOPS | DEPLOYMENT_MANAGER ]")
    @RequestMapping(method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    public RestResponse<ApplicationEnvironment> get(@PathVariable String applicationId) {
        Application application = alienDAO.findById(Application.class, applicationId);
        AuthorizationUtil.checkAuthorizationForApplication(application, ApplicationRole.values());
        ApplicationEnvironment[] environments = applicationEnvironmentService.getByApplicationId(applicationId);
        return RestResponseBuilder.<ApplicationEnvironment> builder().data(environments[0]).build();
    }

    /**
     * Search for application environment for a given application id
     * 
     * @param applicationId the targeted application id
     * @param searchRequest
     * @return A rest response that contains a {@link FacetedSearchResult} containing application environments for an application id
     */
    @SuppressWarnings("rawtypes")
    @ApiOperation(value = "Search for application environments", notes = "Returns a search result with that contains application environments matching the request. A application environment is returned only if the connected user has at least one application role in [ APPLICATION_MANAGER | APPLICATION_USER | APPLICATION_DEVOPS | DEPLOYMENT_MANAGER ]")
    @RequestMapping(value = "/search", method = RequestMethod.POST, consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public RestResponse<GetMultipleDataResult> search(@PathVariable String applicationId, @RequestBody SearchRequest searchRequest) {
        GetMultipleDataResult<ApplicationEnvironment> searchResult = alienDAO.search(ApplicationEnvironment.class, searchRequest.getQuery(),
                getApplicationEnvironmentFilters(applicationId), searchRequest.getFrom(), searchRequest.getSize());
        return RestResponseBuilder.<GetMultipleDataResult> builder().data(searchResult).build();
    }

    /**
     * Get application environment from it's id
     *
     * @param applicationId The application id
     */
    @ApiOperation(value = "Get an application based from its id.", notes = "Returns the application details. Application role required [ APPLICATION_MANAGER | APPLICATION_USER | APPLICATION_DEVOPS | DEPLOYMENT_MANAGER ]")
    @RequestMapping(value = "/{applicationEnvironmentId:.+}", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    public RestResponse<ApplicationEnvironment> getApplicationEnvironment(@PathVariable String applicationId, @PathVariable String applicationEnvironmentId) {
        Application application = alienDAO.findById(Application.class, applicationId);
        AuthorizationUtil.checkAuthorizationForApplication(application, ApplicationRole.values());
        ApplicationEnvironment applicationEnvironment = alienDAO.findById(ApplicationEnvironment.class, applicationEnvironmentId);
        return RestResponseBuilder.<ApplicationEnvironment> builder().data(applicationEnvironment).build();
    }

    /**
     * Create the application environment for an application
     * 
     * @param request data to create an application environment
     * @return application environment id
     */
    @ApiOperation(value = "Create a new application environment.", notes = "If successfull returns a rest response with the id of the created application environment in data. If not successful a rest response with an error content is returned. Role required [ APPLICATIONS_MANAGER ]. "
            + "By default the application environment creator will have application roles [APPLICATION_MANAGER, DEPLOYMENT_MANAGER]")
    @RequestMapping(method = RequestMethod.POST, consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseStatus(value = HttpStatus.CREATED)
    public RestResponse<String> create(@Valid @RequestBody ApplicationEnvironmentRequest request) {
        AuthorizationUtil.checkHasOneRoleIn(Role.APPLICATIONS_MANAGER);
        ApplicationEnvironment appEnvironment = null;
        Application application = alienDAO.findById(Application.class, request.getApplicationId());
        if (application != null) {
            AuthorizationUtil.hasAuthorizationForApplication(application, ApplicationRole.APPLICATION_MANAGER, ApplicationRole.DEPLOYMENT_MANAGER);
            appEnvironment = applicationEnvironmentService.createApplicationEnvironment(request.getApplicationId(), request.getName(),
                    request.getDescription(), request.getEnvironmentType());
            if (request.getCloudId() != null) {
                // validate cloud and rights
                Cloud cloud = cloudService.getMandatoryCloud(request.getCloudId());
                AuthorizationUtil.checkAuthorizationForCloud(cloud, CloudRole.values());
                appEnvironment.setCloudId(request.getCloudId());
                alienDAO.save(appEnvironment);
            }
        } else {
            // no application found to create an env
            return RestResponseBuilder
                    .<String> builder()
                    .data(null)
                    .error(RestErrorBuilder.builder(RestErrorCode.APPLICATION_ENVIRONMENT_ERROR)
                            .message("Application with id <" + request.getApplicationId() + "> could not be found to create an environment").build()).build();
        }
        return RestResponseBuilder.<String> builder().data(appEnvironment.getId()).build();
    }

    /**
     * Update application environment
     * 
     * @param applicationEnvironmentId
     * @param request
     * @return
     */
    @ApiOperation(value = "Updates by merging the given request into the given application environment", notes = "The logged-in user must have the application manager role for this application. Application role required [ APPLICATION_MANAGER ]")
    @RequestMapping(value = "/{applicationEnvironmentId:.+}", method = RequestMethod.PUT, consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public RestResponse<Void> update(@PathVariable String applicationEnvironmentId, @RequestBody UpdateApplicationEnvironmentRequest request) {
        // check application env id
        ApplicationEnvironment appEnvironment = alienDAO.findById(ApplicationEnvironment.class, applicationEnvironmentId);
        if (appEnvironment != null) {
            // check application
            Application application = alienDAO.findById(Application.class, appEnvironment.getApplicationId());
            if (application != null) {
                // check : unique app environment name for a given application
                applicationEnvironmentService.ensureNameUnicity(application.getId(), request.getName());
                ReflectionUtil.mergeObject(request, appEnvironment);
                if (appEnvironment.getName() == null || appEnvironment.getName().isEmpty()) {
                    throw new InvalidArgumentException("Application environment name cannot be set to null or empty");
                }
                alienDAO.save(appEnvironment);
            } else {
                // linked application id not found
                return RestResponseBuilder
                        .<Void> builder()
                        .data(null)
                        .error(RestErrorBuilder.builder(RestErrorCode.APPLICATION_ENVIRONMENT_ERROR)
                                .message("Application with id <" + appEnvironment.getApplicationId() + "> could not be found to update an environment").build())
                        .build();
            }
        } else {
            // no application found to create an env
            return RestResponseBuilder
                    .<Void> builder()
                    .data(null)
                    .error(RestErrorBuilder.builder(RestErrorCode.APPLICATION_ENVIRONMENT_ERROR)
                            .message("Application environment with id <" + applicationEnvironmentId + "> does not exist").build()).build();
        }
        return RestResponseBuilder.<Void> builder().build();
    }

    /**
     * Delete an application environment based on it's id
     * 
     * @param applicationEnvironmentId
     * @return
     */
    @ApiOperation(value = "Delete an application environment from its id", notes = "The logged-in user must have the application manager role for this application. Application role required [ APPLICATION_MANAGER ]")
    @RequestMapping(value = "/{applicationEnvironmentId:.+}", method = RequestMethod.DELETE, produces = MediaType.APPLICATION_JSON_VALUE)
    public RestResponse<Boolean> delete(@PathVariable String applicationId, @PathVariable String applicationEnvironmentId) {
        Application application = applicationService.getOrFail(applicationId);
        AuthorizationUtil.checkAuthorizationForApplication(application, ApplicationRole.APPLICATION_MANAGER);
        boolean deleted = applicationEnvironmentService.delete(applicationEnvironmentId);
        return RestResponseBuilder.<Boolean> builder().data(deleted).build();
    }

    /**
     * Filter to search app environments only for an application id
     * 
     * @param applicationId
     * @return
     */
    private Map<String, String[]> getApplicationEnvironmentFilters(String applicationId) {
        List<String> filterKeys = Lists.newArrayList();
        List<String[]> filterValues = Lists.newArrayList();
        if (applicationId != null) {
            filterKeys.add("applicationId");
            filterValues.add(new String[] { applicationId });
        }
        return MapUtil.newHashMap(filterKeys.toArray(new String[filterKeys.size()]), filterValues.toArray(new String[filterValues.size()][]));
    }

    /**
     * Add a role to a user on a specific application environment
     *
     * @param applicationEnvironmentId application environment id
     * @param username user for who to add role
     * @param role the application role to add to this user
     * @return A {@link Void} {@link RestResponse}.
     */
    @ApiOperation(value = "Add a role to a user on a specific application environment", notes = "Any user with application role APPLICATION_MANAGER can assign any role to another user. Application role required [ APPLICATION_MANAGER ]")
    @RequestMapping(value = "/{applicationEnvironmentId:.+}/userRoles/{username}/{role}", method = RequestMethod.PUT, produces = MediaType.APPLICATION_JSON_VALUE)
    public RestResponse<Void> addUserRole(@PathVariable String applicationEnvironmentId, @PathVariable String username, @PathVariable String role) {
        ApplicationEnvironment applicationEnvironment = checkAndGetApplicationEnvironment(applicationEnvironmentId, ApplicationRole.APPLICATION_MANAGER);
        resourceRoleService.addUserRole(applicationEnvironment, username, role);
        return RestResponseBuilder.<Void> builder().build();
    }

    /**
     * Add a role to a group on a specific application environment
     *
     * @param applicationEnvironmentId application environment id
     * @param groupId The id of the group to update roles
     * @param role The role to add to the group on the application environment
     * @return A {@link Void} {@link RestResponse}.
     */
    @ApiOperation(value = "Add a role to a group on a specific application environment", notes = "Any user with application role APPLICATION_MANAGER can assign any role to a group of users. Application role required [ APPLICATION_MANAGER ]")
    @RequestMapping(value = "/{applicationEnvironmentId:.+}/groupRoles/{groupId}/{role}", method = RequestMethod.PUT, produces = MediaType.APPLICATION_JSON_VALUE)
    public RestResponse<Void> addGroupRole(@PathVariable String applicationEnvironmentId, @PathVariable String groupId, @PathVariable String role) {
        ApplicationEnvironment applicationEnvironment = checkAndGetApplicationEnvironment(applicationEnvironmentId, ApplicationRole.APPLICATION_MANAGER);
        resourceRoleService.addGroupRole(applicationEnvironment, groupId, role);
        return RestResponseBuilder.<Void> builder().build();
    }

    /**
     * Remove a role from a user on a specific application environment
     *
     * @param applicationEnvironmentId application environment id
     * @param username The username of the user to update roles
     * @param role The role to add to the user on the application environment
     * @return A {@link Void} {@link RestResponse}
     */
    @ApiOperation(value = "Remove a role to a user on a specific application environment", notes = "Any user with application role APPLICATION_MANAGER can unassign any role to another user. Application role required [ APPLICATION_MANAGER ]")
    @RequestMapping(value = "/{applicationEnvironmentId:.+}/userRoles/{username}/{role}", method = RequestMethod.DELETE, produces = MediaType.APPLICATION_JSON_VALUE)
    public RestResponse<Void> removeUserRole(@PathVariable String applicationEnvironmentId, @PathVariable String username, @PathVariable String role) {
        ApplicationEnvironment applicationEnvironment = checkAndGetApplicationEnvironment(applicationEnvironmentId, ApplicationRole.APPLICATION_MANAGER);
        resourceRoleService.removeUserRole(applicationEnvironment, username, role);
        return RestResponseBuilder.<Void> builder().build();
    }

    /**
     * Remove a role from a user on a specific application environment
     *
     * @param applicationEnvironmentId application environment id
     * @param groupId The id of the group to update roles
     * @param role The role to add to the user on the application environment
     * @return A {@link Void} {@link RestResponse}.
     */
    @ApiOperation(value = "Remove a role of a group on a specific application environment", notes = "Any user with application role APPLICATION_MANAGER can un-assign any role to a group. Application role required [ APPLICATION_MANAGER ]")
    @RequestMapping(value = "/{applicationEnvironmentId:.+}/groupRoles/{groupId}/{role}", method = RequestMethod.DELETE, produces = MediaType.APPLICATION_JSON_VALUE)
    public RestResponse<Void> removeGroupRole(@PathVariable String applicationEnvironmentId, @PathVariable String groupId, @PathVariable String role) {
        ApplicationEnvironment applicationEnvironment = checkAndGetApplicationEnvironment(applicationEnvironmentId, ApplicationRole.APPLICATION_MANAGER);
        resourceRoleService.removeGroupRole(applicationEnvironment, groupId, role);
        return RestResponseBuilder.<Void> builder().build();
    }

    /**
     * Check rights and get application environment
     * 
     * @param applicationEnvironmentId
     * @param roles
     * @return the corresponding application environment
     */
    private ApplicationEnvironment checkAndGetApplicationEnvironment(String applicationEnvironmentId, ApplicationRole... roles) {
        ApplicationEnvironment applicationEnvironment = applicationEnvironmentService.getOrFail(applicationEnvironmentId);
        AuthorizationUtil.checkAuthorizationForApplication(applicationEnvironment, ApplicationRole.APPLICATION_MANAGER);
        return applicationEnvironment;
    }

}
