package alien4cloud.rest.application;

import java.util.List;
import java.util.Map;

import javax.annotation.Resource;

import lombok.extern.slf4j.Slf4j;

import org.elasticsearch.index.query.FilterBuilder;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import alien4cloud.application.ApplicationEnvironmentService;
import alien4cloud.application.ApplicationService;
import alien4cloud.application.ApplicationVersionService;
import alien4cloud.application.DeploymentSetupService;
import alien4cloud.cloud.CloudService;
import alien4cloud.cloud.DeploymentService;
import alien4cloud.dao.IGenericSearchDAO;
import alien4cloud.dao.model.FacetedSearchResult;
import alien4cloud.dao.model.GetMultipleDataResult;
import alien4cloud.exception.AlreadyExistException;
import alien4cloud.exception.DeleteDeployedException;
import alien4cloud.exception.DeleteLastApplicationEnvironmentException;
import alien4cloud.exception.InvalidArgumentException;
import alien4cloud.model.application.Application;
import alien4cloud.model.application.ApplicationEnvironment;
import alien4cloud.model.application.ApplicationVersion;
import alien4cloud.model.application.DeploymentSetupMatchInfo;
import alien4cloud.model.cloud.Cloud;
import alien4cloud.paas.exception.CloudDisabledException;
import alien4cloud.paas.model.DeploymentStatus;
import alien4cloud.rest.component.SearchRequest;
import alien4cloud.rest.model.RestErrorBuilder;
import alien4cloud.rest.model.RestErrorCode;
import alien4cloud.rest.model.RestResponse;
import alien4cloud.rest.model.RestResponseBuilder;
import alien4cloud.rest.topology.TopologyService;
import alien4cloud.security.ApplicationEnvironmentRole;
import alien4cloud.security.ApplicationRole;
import alien4cloud.security.AuthorizationUtil;
import alien4cloud.security.CloudRole;
import alien4cloud.security.Role;
import alien4cloud.security.UserService;
import alien4cloud.utils.MapUtil;
import alien4cloud.utils.ReflectionUtil;
import alien4cloud.utils.services.ResourceRoleService;

import com.google.common.collect.Lists;
import com.wordnik.swagger.annotations.Api;
import com.wordnik.swagger.annotations.ApiOperation;

@Slf4j
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
    @Resource
    private ApplicationVersionService applicationVersionService;
    @Resource
    private DeploymentService deploymentService;
    @Resource
    private DeploymentSetupService deploymentSetupService;
    @Resource
    private UserService userService;
    @Resource
    private TopologyService topologyService;

    /**
     * Search for application environment for a given application id
     *
     * @param applicationId the targeted application id
     * @param searchRequest
     * @return A rest response that contains a {@link FacetedSearchResult} containing application environments for an application id
     */
    @ApiOperation(value = "Search for application environments", notes = "Returns a search result with that contains application environments DTO matching the request. A application environment is returned only if the connected user has at least one application role in [ APPLICATION_USER | DEPLOYMENT_MANAGER ]")
    @RequestMapping(value = "/search", method = RequestMethod.POST, consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public RestResponse<GetMultipleDataResult<ApplicationEnvironmentDTO>> search(@PathVariable String applicationId, @RequestBody SearchRequest searchRequest) {
        FilterBuilder authorizationFilter = getEnvrionmentAuthorizationFilters(applicationId);
        Map<String, String[]> applicationEnvironmentFilters = getApplicationEnvironmentFilters(applicationId);
        GetMultipleDataResult<ApplicationEnvironment> searchResult = alienDAO.search(ApplicationEnvironment.class, searchRequest.getQuery(),
                applicationEnvironmentFilters, authorizationFilter, null, searchRequest.getFrom(), searchRequest.getSize());

        GetMultipleDataResult<ApplicationEnvironmentDTO> searchResultDTO = new GetMultipleDataResult<ApplicationEnvironmentDTO>();
        searchResultDTO.setQueryDuration(searchResult.getQueryDuration());
        searchResultDTO.setTypes(searchResult.getTypes());
        searchResultDTO.setData(getApplicationEnvironmentDTO(searchResult.getData()));
        searchResultDTO.setTotalResults(searchResult.getTotalResults());
        return RestResponseBuilder.<GetMultipleDataResult<ApplicationEnvironmentDTO>> builder().data(searchResultDTO).build();
    }

    private FilterBuilder getEnvrionmentAuthorizationFilters(String applicationId) {
        Application application = applicationService.checkAndGetApplication(applicationId);
        if (AuthorizationUtil.hasAuthorizationForApplication(application)) {
            return null;
        }
        return AuthorizationUtil.getResourceAuthorizationFilters();
    }

    /**
     * Get application environment from its id
     *
     * @param applicationId The application id
     */
    @ApiOperation(value = "Get an application environment from its id", notes = "Returns the application environment. Application role required [ APPLICATION_USER | DEPLOYMENT_MANAGER ]")
    @RequestMapping(value = "/{applicationEnvironmentId:.+}", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    public RestResponse<ApplicationEnvironment> getApplicationEnvironment(@PathVariable String applicationId, @PathVariable String applicationEnvironmentId) {
        Application application = applicationService.checkAndGetApplication(applicationId);
        ApplicationEnvironment environment = applicationEnvironmentService.checkAndGetApplicationEnvironment(applicationEnvironmentId,
                ApplicationRole.APPLICATION_MANAGER);
        AuthorizationUtil.checkAuthorizationForEnvironment(environment, ApplicationEnvironmentRole.values());
        if (!AuthorizationUtil.hasAuthorizationForApplication(application, ApplicationRole.APPLICATION_MANAGER)) {
            AuthorizationUtil.checkAuthorizationForEnvironment(environment, ApplicationEnvironmentRole.values());
        }
        return RestResponseBuilder.<ApplicationEnvironment> builder().data(environment).build();
    }

    /**
     * Create the application environment for an application
     *
     * @param request data to create an application environment
     * @return application environment id
     */
    @ApiOperation(value = "Create a new application environment", notes = "If successfull returns a rest response with the id of the created application environment in data. If not successful a rest response with an error content is returned. Role required [ APPLICATIONS_MANAGER ]"
            + "By default the application environment creator will have application roles [ APPLICATION_MANAGER, DEPLOYMENT_MANAGER ]")
    @RequestMapping(method = RequestMethod.POST, consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseStatus(value = HttpStatus.CREATED)
    public RestResponse<String> create(@PathVariable String applicationId, @RequestBody ApplicationEnvironmentRequest request) throws CloudDisabledException {

        // User should be APPLICATIONS_MANAGER to create an application
        AuthorizationUtil.checkHasOneRoleIn(Role.APPLICATIONS_MANAGER);
        Application application = applicationService.getOrFail(applicationId);
        // User should be APPLICATION_MANAGER to create an application
        AuthorizationUtil.hasAuthorizationForApplication(application, ApplicationRole.APPLICATION_MANAGER);

        final Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        ApplicationEnvironment appEnvironment = applicationEnvironmentService.createApplicationEnvironment(auth.getName(), request.getApplicationId(),
                request.getName(), request.getDescription(), request.getEnvironmentType(), request.getVersionId());

        if (request.getCloudId() != null) {
            Cloud cloud = cloudService.getMandatoryCloud(request.getCloudId());
            AuthorizationUtil.checkAuthorizationForCloud(cloud, CloudRole.values());
            appEnvironment.setCloudId(request.getCloudId());
            alienDAO.save(appEnvironment);
            try {
                DeploymentSetupMatchInfo deploymentSetupMatchInfo = deploymentSetupService.getDeploymentSetupMatchInfo(applicationId, appEnvironment.getId());
                deploymentSetupService.generatePropertyDefinition(deploymentSetupMatchInfo, cloud);
                alienDAO.save(deploymentSetupMatchInfo.getDeploymentSetup());
            } catch (AlreadyExistException e) {
                log.error("DeploymentSetup already exists");
            }
        }

        alienDAO.save(appEnvironment);
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
    public RestResponse<Void> update(@PathVariable String applicationId, @PathVariable String applicationEnvironmentId,
            @RequestBody UpdateApplicationEnvironmentRequest request) throws CloudDisabledException {

        ApplicationEnvironment applicationEnvironment = applicationEnvironmentService.getOrFail(applicationEnvironmentId);
        // Deployment manager can update a environment
        if (request.getCloudId() != null
                && !AuthorizationUtil.hasAuthorizationForEnvironment(applicationEnvironment, ApplicationEnvironmentRole.DEPLOYMENT_MANAGER)) {
            // Only APPLICATION_MANAGER on the underlying application can update an application environment
            Application application = applicationService.getOrFail(applicationId);
            AuthorizationUtil.hasAuthorizationForApplication(application, ApplicationRole.APPLICATION_MANAGER);
        }

        if (applicationEnvironment == null) {
            return RestResponseBuilder
                    .<Void> builder()
                    .data(null)
                    .error(RestErrorBuilder.builder(RestErrorCode.APPLICATION_ENVIRONMENT_ERROR)
                            .message("Application environment with id <" + applicationEnvironmentId + "> does not exist").build()).build();
        }

        // prevent cloud id update when the environment is deployed
        if (request.getCloudId() != null && applicationEnvironmentService.isDeployed(applicationEnvironmentId)) {
            return RestResponseBuilder
                    .<Void> builder()
                    .data(null)
                    .error(RestErrorBuilder
                            .builder(RestErrorCode.APPLICATION_ENVIRONMENT_ERROR)
                            .message(
                                    "Application environment with id <" + applicationEnvironmentId + "> is currently deployed on cloud <"
                                            + request.getCloudId() + ">. Cloud update is not possible.").build()).build();

        }

        if (request.getCloudId() != null) {
            // Check Cloud rights
            Cloud cloud = cloudService.getMandatoryCloud(request.getCloudId());
            AuthorizationUtil.checkAuthorizationForCloud(cloud, CloudRole.values());

            // Update the linked deployment setup
            DeploymentSetupMatchInfo deploymentSetupMatchInfo = deploymentSetupService.getDeploymentSetupMatchInfo(applicationId, applicationEnvironmentId);
            deploymentSetupService.generatePropertyDefinition(deploymentSetupMatchInfo, cloud);
            alienDAO.save(deploymentSetupMatchInfo.getDeploymentSetup());
        }

        applicationEnvironmentService.ensureNameUnicity(applicationEnvironment.getApplicationId(), request.getName());
        ReflectionUtil.mergeObject(request, applicationEnvironment);
        if (applicationEnvironment.getName() == null || applicationEnvironment.getName().isEmpty()) {
            throw new InvalidArgumentException("Application environment name cannot be set to null or empty");
        }
        alienDAO.save(applicationEnvironment);
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

        // Only APPLICATION_MANAGER on the underlying application can delete an application environment
        applicationEnvironmentService.checkAndGetApplicationEnvironment(applicationEnvironmentId, ApplicationRole.APPLICATION_MANAGER);

        int countEnvironment = applicationEnvironmentService.getByApplicationId(applicationId).length;
        boolean isDeployed = applicationEnvironmentService.isDeployed(applicationEnvironmentId);
        if (isDeployed) {
            throw new DeleteDeployedException("Application environment with id <" + applicationEnvironmentId + "> cannot be deleted since it is deployed");
        }
        boolean deleted = false;
        if (countEnvironment == 1 || isDeployed) {
            throw new DeleteLastApplicationEnvironmentException("Application environment with id <" + applicationEnvironmentId
                    + "> cannot be deleted as it's the last one for the application id <" + applicationId + ">");
        }
        deleted = applicationEnvironmentService.delete(applicationEnvironmentId);
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
        ApplicationEnvironment applicationEnvironment = applicationEnvironmentService.checkAndGetApplicationEnvironment(applicationEnvironmentId,
                ApplicationRole.APPLICATION_MANAGER);
        resourceRoleService.addUserRole(applicationEnvironment, username, role);
        handleAddUserRoleOnApplication(applicationEnvironment.getApplicationId(), username);
        return RestResponseBuilder.<Void> builder().build();
    }

    /**
     * Add a role to a group on a specific application environment
     *
     * @param applicationEnvironmentId application environment id
     * @param groupId The id of the group to update roles
     * @param role The role to add to the group on the application environment from {@link ApplicationEnvironmentRole}
     * @return A {@link Void} {@link RestResponse}.
     */
    @ApiOperation(value = "Add a role to a group on a specific application environment", notes = "Any user with application role APPLICATION_MANAGER can assign any role to a group of users. Application role required [ APPLICATION_MANAGER ]")
    @RequestMapping(value = "/{applicationEnvironmentId:.+}/groupRoles/{groupId}/{role}", method = RequestMethod.PUT, produces = MediaType.APPLICATION_JSON_VALUE)
    public RestResponse<Void> addGroupRole(@PathVariable String applicationEnvironmentId, @PathVariable String groupId, @PathVariable String role) {
        ApplicationEnvironment applicationEnvironment = applicationEnvironmentService.checkAndGetApplicationEnvironment(applicationEnvironmentId,
                ApplicationRole.APPLICATION_MANAGER);
        resourceRoleService.addGroupRole(applicationEnvironment, groupId, role);
        handleAddGrpRoleOnApplication(applicationEnvironment.getApplicationId(), groupId);
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
        ApplicationEnvironment applicationEnvironment = applicationEnvironmentService.checkAndGetApplicationEnvironment(applicationEnvironmentId,
                ApplicationRole.APPLICATION_MANAGER);
        resourceRoleService.removeUserRole(applicationEnvironment, username, role);
        handleRemoveUserRoleOnApplication(applicationEnvironment.getApplicationId(), username);
        return RestResponseBuilder.<Void> builder().build();
    }

    /**
     * Remove a role from a group on a specific application environment
     *
     * @param applicationEnvironmentId application environment id
     * @param groupId The id of the group to update roles
     * @param role The role to add to the user on the application environment
     * @return A {@link Void} {@link RestResponse}.
     */
    @ApiOperation(value = "Remove a role of a group on a specific application environment", notes = "Any user with application role APPLICATION_MANAGER can un-assign any role to a group. Application role required [ APPLICATION_MANAGER ]")
    @RequestMapping(value = "/{applicationEnvironmentId:.+}/groupRoles/{groupId}/{role}", method = RequestMethod.DELETE, produces = MediaType.APPLICATION_JSON_VALUE)
    public RestResponse<Void> removeGroupRole(@PathVariable String applicationEnvironmentId, @PathVariable String groupId, @PathVariable String role) {
        ApplicationEnvironment applicationEnvironment = applicationEnvironmentService.checkAndGetApplicationEnvironment(applicationEnvironmentId,
                ApplicationRole.APPLICATION_MANAGER);
        resourceRoleService.removeGroupRole(applicationEnvironment, groupId, role);
        handleRemoveGrpRoleOnApplication(applicationEnvironment.getApplicationId(), groupId);
        return RestResponseBuilder.<Void> builder().build();
    }

    /**
     * Handle user roles on the targeted application
     * Any role on an environment implies APPLICATION_USER role on the linked application
     * 
     * @param applicationId
     * @param username
     */
    private void handleAddUserRoleOnApplication(String applicationId, String username) {
        Application application = applicationService.getOrFail(applicationId);
        resourceRoleService.addUserRole(application, username, ApplicationRole.APPLICATION_USER.toString());
    }

    /**
     * Handle remove roles on the targeted application
     * 
     * @param applicationId
     * @param username
     */
    private void handleRemoveUserRoleOnApplication(String applicationId, String username) {
        Application application = applicationService.getOrFail(applicationId);
        boolean isApplicationUserOnly = AuthorizationUtil.hasUniqueUserRoleOnResource(userService.retrieveUser(username), application,
                ApplicationRole.APPLICATION_USER);
        // check this condition > remove the role only if it is not the only role
        if (!isApplicationUserOnly) {
            resourceRoleService.removeUserRole(application, username, ApplicationRole.APPLICATION_USER.toString());
        }
    }

    /**
     * Handle group roles on the targeted application
     * Any role on an environment implies APPLICATION_USER role on the linked application
     * 
     * @param applicationEnvironmentId
     * @param username
     */
    private void handleAddGrpRoleOnApplication(String applicationId, String groupId) {
        Application application = applicationService.getOrFail(applicationId);
        resourceRoleService.addGroupRole(application, groupId, ApplicationRole.APPLICATION_USER.toString());
    }

    /**
     * Handle group roles on the targeted application
     * 
     * @param applicationEnvironmentId
     * @param username
     */
    private void handleRemoveGrpRoleOnApplication(String applicationId, String groupId) {
        Application application = applicationService.getOrFail(applicationId);
        boolean isApplicationGroupOnly = AuthorizationUtil.hasUniqueGroupRoleOnResource(groupId, application, ApplicationRole.APPLICATION_USER);
        if (!isApplicationGroupOnly) {
            resourceRoleService.removeGroupRole(application, groupId, ApplicationRole.APPLICATION_USER.toString());
        }
    }

    /**
     * Get a list a application environment DTO
     *
     * @param applicationEnvironments
     * @return
     */
    private ApplicationEnvironmentDTO[] getApplicationEnvironmentDTO(ApplicationEnvironment[] applicationEnvironments) {
        List<ApplicationEnvironmentDTO> listApplicationEnvironmentsDTO = Lists.newArrayList();
        ApplicationEnvironmentDTO tempEnvDTO = null;
        for (ApplicationEnvironment env : applicationEnvironments) {
            tempEnvDTO = new ApplicationEnvironmentDTO();
            tempEnvDTO.setApplicationId(env.getApplicationId());
            tempEnvDTO.setDescription(env.getDescription());
            tempEnvDTO.setEnvironmentType(env.getEnvironmentType());
            tempEnvDTO.setId(env.getId());
            tempEnvDTO.setName(env.getName());
            tempEnvDTO.setUserRoles(env.getUserRoles());
            tempEnvDTO.setGroupRoles(env.getGroupRoles());
            if (env.getCloudId() != null) {
                tempEnvDTO.setCloudName(cloudService.get(env.getCloudId()).getName());
            } else {
                tempEnvDTO.setCloudName(null);
            }
            tempEnvDTO.setCloudId(env.getCloudId());
            ApplicationVersion applicationVersion = applicationVersionService.get(env.getCurrentVersionId());
            tempEnvDTO.setCurrentVersionName(applicationVersion != null ? applicationVersion.getVersion() : null);
            try {
                tempEnvDTO.setStatus(applicationEnvironmentService.getStatus(env));
            } catch (CloudDisabledException e) {
                log.debug("Getting status for the environment <" + env.getId() + "> failed because the associated cloud <" + env.getCloudId()
                        + "> seems disabled. Returned status is UNKNOWN.", e);
                tempEnvDTO.setStatus(DeploymentStatus.UNKNOWN);
            }
            listApplicationEnvironmentsDTO.add(tempEnvDTO);
        }
        return listApplicationEnvironmentsDTO.toArray(new ApplicationEnvironmentDTO[listApplicationEnvironmentsDTO.size()]);
    }

    @ApiOperation(value = "Get the id of the topology linked to the environment", notes = "Application role required [ APPLICATION_MANAGER | APPLICATION_DEVOPS ]")
    @RequestMapping(value = "/{applicationEnvironmentId:.+}/topology", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    public RestResponse<String> getTopologyId(@PathVariable String applicationId, @PathVariable String applicationEnvironmentId) {
        Application application = applicationService.getOrFail(applicationId);
        AuthorizationUtil.checkAuthorizationForApplication(application, ApplicationRole.values());
        ApplicationEnvironment environment = applicationEnvironmentService
                .checkAndGetApplicationEnvironment(applicationEnvironmentId, ApplicationRole.values());
        if (!AuthorizationUtil.hasAuthorizationForApplication(application, ApplicationRole.values())) {
            AuthorizationUtil.checkAuthorizationForEnvironment(environment, ApplicationEnvironmentRole.DEPLOYMENT_MANAGER);
        }
        String topologyId = applicationEnvironmentService.getTopologyId(applicationEnvironmentId);
        return RestResponseBuilder.<String> builder().data(topologyId).build();
    }

}
