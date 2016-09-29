package alien4cloud.rest.application;

import java.util.List;
import java.util.Map;

import javax.annotation.Resource;

import org.elasticsearch.index.query.FilterBuilder;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.google.common.collect.Lists;

import alien4cloud.application.ApplicationEnvironmentService;
import alien4cloud.application.ApplicationService;
import alien4cloud.application.ApplicationVersionService;
import alien4cloud.audit.annotation.Audit;
import alien4cloud.dao.IGenericSearchDAO;
import alien4cloud.dao.model.FacetedSearchResult;
import alien4cloud.dao.model.GetMultipleDataResult;
import alien4cloud.exception.ApplicationVersionNotFoundException;
import alien4cloud.exception.DeleteLastApplicationEnvironmentException;
import alien4cloud.model.application.Application;
import alien4cloud.model.application.ApplicationEnvironment;
import alien4cloud.model.application.ApplicationVersion;
import alien4cloud.paas.exception.OrchestratorDisabledException;
import alien4cloud.paas.model.DeploymentStatus;
import alien4cloud.rest.application.model.ApplicationEnvironmentDTO;
import alien4cloud.rest.application.model.ApplicationEnvironmentRequest;
import alien4cloud.rest.application.model.UpdateApplicationEnvironmentRequest;
import alien4cloud.rest.component.SearchRequest;
import alien4cloud.rest.model.RestErrorBuilder;
import alien4cloud.rest.model.RestErrorCode;
import alien4cloud.rest.model.RestResponse;
import alien4cloud.rest.model.RestResponseBuilder;
import alien4cloud.security.AuthorizationUtil;
import alien4cloud.security.model.ApplicationEnvironmentRole;
import alien4cloud.security.model.ApplicationRole;
import alien4cloud.security.model.Role;
import alien4cloud.utils.MapUtil;
import alien4cloud.utils.ReflectionUtil;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestController
@RequestMapping({"/rest/applications/{applicationId:.+}/environments", "/rest/v1/applications/{applicationId:.+}/environments", "/rest/latest/applications/{applicationId:.+}/environments"})
@Api(value = "", description = "Manages application's environments")
public class ApplicationEnvironmentController {

    @Resource(name = "alien-es-dao")
    private IGenericSearchDAO alienDAO;
    @Resource
    private ApplicationEnvironmentService applicationEnvironmentService;
    @Resource
    private ApplicationService applicationService;
    @Resource
    private ApplicationVersionService applicationVersionService;

    /**
     * Search for application environment for a given application id
     *
     * @param applicationId the targeted application id
     * @param searchRequest
     * @return A rest response that contains a {@link FacetedSearchResult} containing application environments for an application id
     */
    @ApiOperation(value = "Search for application environments", notes = "Returns a search result with that contains application environments DTO matching the request. A application environment is returned only if the connected user has at least one application role in [ APPLICATION_USER | DEPLOYMENT_MANAGER ]")
    @RequestMapping(value = "/search", method = RequestMethod.POST, consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("isAuthenticated()")
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
     * @param applicationEnvironmentId the environment for which to get the status
     * @return A {@link RestResponse} that contains the application environment {@link ApplicationEnvironment}.
     */
    @ApiOperation(value = "Get an application environment from its id", notes = "Returns the application environment. Application role required [ APPLICATION_USER | DEPLOYMENT_MANAGER ]")
    @RequestMapping(value = "/{applicationEnvironmentId:.+}", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("isAuthenticated()")
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
     * Get the current status of the environment for the given application.
     *
     * @param applicationId the id of the application to be deployed.
     * @param applicationEnvironmentId the environment for which to get the status
     * @return A {@link RestResponse} that contains the application's current {@link DeploymentStatus}.
     * @throws Exception
     */
    @ApiOperation(value = "Get an application environment from its id", notes = "Returns the application environment. Application role required [ APPLICATION_USER | DEPLOYMENT_MANAGER ]")
    @RequestMapping(value = "/{applicationEnvironmentId:.+}/status", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("isAuthenticated()")
    public RestResponse<DeploymentStatus> getApplicationEnvironmentStatus(@PathVariable String applicationId, @PathVariable String applicationEnvironmentId)
            throws Exception {
        Application application = applicationService.checkAndGetApplication(applicationId);
        ApplicationEnvironment environment = applicationEnvironmentService.getOrFail(applicationEnvironmentId);
        AuthorizationUtil.checkAuthorizationForEnvironment(environment, ApplicationEnvironmentRole.values());
        if (!AuthorizationUtil.hasAuthorizationForApplication(application, ApplicationRole.APPLICATION_MANAGER)) {
            AuthorizationUtil.checkAuthorizationForEnvironment(environment, ApplicationEnvironmentRole.values());
        }
        DeploymentStatus status = applicationEnvironmentService.getStatus(environment);
        return RestResponseBuilder.<DeploymentStatus> builder().data(status).build();
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
    @PreAuthorize("isAuthenticated()")
    @Audit
    public RestResponse<String> create(@PathVariable String applicationId, @RequestBody ApplicationEnvironmentRequest request)
            throws OrchestratorDisabledException {
        // User should be APPLICATIONS_MANAGER to create an application
        AuthorizationUtil.checkHasOneRoleIn(Role.APPLICATIONS_MANAGER);
        Application application = applicationService.getOrFail(applicationId);
        // User should be APPLICATION_MANAGER to create an application
        AuthorizationUtil.hasAuthorizationForApplication(application, ApplicationRole.APPLICATION_MANAGER);

        final Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        ApplicationEnvironment appEnvironment = applicationEnvironmentService.createApplicationEnvironment(auth.getName(), applicationId, request.getName(),
                request.getDescription(), request.getEnvironmentType(), request.getVersionId());

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
    @PreAuthorize("isAuthenticated()")
    @Audit
    public RestResponse<Void> update(@PathVariable String applicationId, @PathVariable String applicationEnvironmentId,
            @RequestBody UpdateApplicationEnvironmentRequest request) throws OrchestratorDisabledException {

        ApplicationEnvironment applicationEnvironment = applicationEnvironmentService.getOrFail(applicationEnvironmentId);
        // Only APPLICATION_MANAGER on the underlying application can update an application environment
        Application application = applicationService.getOrFail(applicationId);
        AuthorizationUtil.hasAuthorizationForApplication(application, ApplicationRole.APPLICATION_MANAGER);

        if (applicationEnvironment == null) {
            return RestResponseBuilder.<Void> builder().data(null).error(RestErrorBuilder.builder(RestErrorCode.APPLICATION_ENVIRONMENT_ERROR)
                    .message("Application environment with id <" + applicationEnvironmentId + "> does not exist").build()).build();
        }

        applicationEnvironmentService.ensureNameUnicity(applicationEnvironment.getApplicationId(), request.getName());
        ReflectionUtil.mergeObject(request, applicationEnvironment);
        if (applicationEnvironment.getName() == null || applicationEnvironment.getName().isEmpty()) {
            throw new UnsupportedOperationException("Application environment name cannot be set to null or empty");
        }
        alienDAO.save(applicationEnvironment);
        return RestResponseBuilder.<Void> builder().build();
    }

    /**
     * Delete an application environment based on it's id.
     *
     * @param applicationEnvironmentId
     * @return
     */
    @ApiOperation(value = "Delete an application environment from its id", notes = "The logged-in user must have the application manager role for this application. Application role required [ APPLICATION_MANAGER ]")
    @RequestMapping(value = "/{applicationEnvironmentId:.+}", method = RequestMethod.DELETE, produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("isAuthenticated()")
    @Audit
    public RestResponse<Boolean> delete(@PathVariable String applicationId, @PathVariable String applicationEnvironmentId) {
        // Only APPLICATION_MANAGER on the underlying application can delete an application environment
        ApplicationEnvironment environmentToDelete = applicationEnvironmentService.getOrFail(applicationEnvironmentId);
        applicationEnvironmentService.checkAndGetApplicationEnvironment(applicationEnvironmentId, ApplicationRole.APPLICATION_MANAGER);

        int countEnvironment = applicationEnvironmentService.getByApplicationId(environmentToDelete.getApplicationId()).length;
        if (countEnvironment == 1) {
            throw new DeleteLastApplicationEnvironmentException("Application environment with id <" + applicationEnvironmentId
                    + "> cannot be deleted as it's the last one for the application id <" + environmentToDelete.getApplicationId() + ">");
        }

        applicationEnvironmentService.delete(applicationEnvironmentId);
        return RestResponseBuilder.<Boolean> builder().data(true).build();
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
            ApplicationVersion applicationVersion = applicationVersionService.get(env.getCurrentVersionId());
            tempEnvDTO.setCurrentVersionName(applicationVersion != null ? applicationVersion.getVersion() : null);
            try {
                tempEnvDTO.setStatus(applicationEnvironmentService.getStatus(env));
            } catch (Exception e) {
                log.debug("Getting status for the environment <" + env.getId()
                        + "> failed because the associated orchestrator cannot be reached. Returned status is UNKNOWN.", e);
                tempEnvDTO.setStatus(DeploymentStatus.UNKNOWN);
            }
            listApplicationEnvironmentsDTO.add(tempEnvDTO);
        }
        return listApplicationEnvironmentsDTO.toArray(new ApplicationEnvironmentDTO[listApplicationEnvironmentsDTO.size()]);
    }

    @ApiOperation(value = "Get the id of the topology linked to the environment", notes = "Application role required [ APPLICATION_MANAGER | APPLICATION_DEVOPS ]")
    @RequestMapping(value = "/{applicationEnvironmentId:.+}/topology", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("isAuthenticated()")
    public RestResponse<String> getTopologyId(@PathVariable String applicationId, @PathVariable String applicationEnvironmentId) {
        Application application = applicationService.getOrFail(applicationId);
        AuthorizationUtil.checkAuthorizationForApplication(application, ApplicationRole.values());
        ApplicationEnvironment environment = applicationEnvironmentService.checkAndGetApplicationEnvironment(applicationEnvironmentId,
                ApplicationRole.values());
        if (!AuthorizationUtil.hasAuthorizationForApplication(application, ApplicationRole.values())) {
            AuthorizationUtil.checkAuthorizationForEnvironment(environment, ApplicationEnvironmentRole.DEPLOYMENT_MANAGER);
        }
        String topologyId = applicationEnvironmentService.getTopologyId(applicationEnvironmentId);
        if (topologyId == null) {
            throw new ApplicationVersionNotFoundException("An application version is required by an application environment.");
        }
        return RestResponseBuilder.<String> builder().data(topologyId).build();
    }
}
