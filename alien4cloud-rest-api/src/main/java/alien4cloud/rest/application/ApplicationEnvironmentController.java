package alien4cloud.rest.application;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;

import javax.annotation.Resource;
import javax.inject.Inject;

import alien4cloud.deployment.DeploymentService;
import alien4cloud.exception.RenameDeployedException;
import com.google.common.collect.Maps;
import org.alien4cloud.alm.deployment.configuration.model.AbstractDeploymentConfig;
import org.alien4cloud.alm.deployment.configuration.model.DeploymentInputs;
import org.alien4cloud.alm.deployment.configuration.model.DeploymentMatchingConfiguration;
import org.alien4cloud.alm.deployment.configuration.services.DeploymentConfigurationDao;
import org.alien4cloud.tosca.model.Csar;
import org.apache.commons.lang.StringUtils;
import org.elasticsearch.index.query.FilterBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

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
import alien4cloud.rest.application.model.*;
import alien4cloud.rest.model.*;
import alien4cloud.security.AuthorizationUtil;
import alien4cloud.security.model.ApplicationEnvironmentRole;
import alien4cloud.security.model.ApplicationRole;
import alien4cloud.utils.MapUtil;
import alien4cloud.utils.ReflectionUtil;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestController
@RequestMapping({ "/rest/applications/{applicationId:.+}/environments", "/rest/v1/applications/{applicationId:.+}/environments",
        "/rest/latest/applications/{applicationId:.+}/environments" })
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
    @Inject
    private ApplicationEnvironmentDTOBuilder dtoBuilder;
    @Inject
    private DeploymentConfigurationDao deploymentConfigurationDao;

    @Resource
    private DeploymentService deploymentService;

    @Value("${features.no_deployed_envs_renaming:#{false}}")
    private boolean deployed_check;

    /**
     * Search for application environment for a given application id
     *
     * @param applicationId the targeted application id
     * @param searchRequest
     * @return A rest response that contains a {@link FacetedSearchResult} containing application environments for an application id
     */
    @ApiOperation(value = "Search for application environments", notes = "Returns a search result with that contains application environments DTO matching the request. A application environment is returned only if the connected user has at least one application environment role in [ APPLICATION_USER | DEPLOYMENT_MANAGER ]")
    @RequestMapping(value = "/search", method = RequestMethod.POST, consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("isAuthenticated()")
    public RestResponse<GetMultipleDataResult<ApplicationEnvironmentDTO>> search(@PathVariable String applicationId,
            @RequestBody FilteredSearchRequest searchRequest) {
        return RestResponseBuilder.<GetMultipleDataResult<ApplicationEnvironmentDTO>> builder()
                .data(transformToDTO(searchAuthorizedEnvironments(applicationId, searchRequest))).build();
    }

    /**
     * Get a list of application environments, which has inputs for deployment, that can be copied when the new application topology version is bound to the
     * environment.
     *
     * @param applicationId application's id
     * @param getInputCandidatesRequest contain parameters for input candidate requests
     * @return list of candidates to copy from
     */
    @ApiOperation(value = "Get a list of application environments, which has inputs for deployment, that can be copied when the new application topology version is bound to the environment")
    @RequestMapping(value = "/input-candidates", method = RequestMethod.POST, consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("isAuthenticated()")
    public RestResponse<List<ApplicationEnvironment>> getEnvironmentCandidatesToCopyInputs(@PathVariable String applicationId,
            @RequestBody GetInputCandidatesRequest getInputCandidatesRequest) {
        FilteredSearchRequest filteredSearchRequest = new FilteredSearchRequest(null, 0, Integer.MAX_VALUE, null);
        GetMultipleDataResult<ApplicationEnvironment> authorizedEnvironments = searchAuthorizedEnvironments(applicationId, filteredSearchRequest);
        // TODO implement something more generic to check if a configuration to copy exist.
        List<ApplicationEnvironment> environmentsWithInputs = Arrays.stream(authorizedEnvironments.getData())
                .filter(environment -> {
                    String id = AbstractDeploymentConfig.generateId(environment.getTopologyVersion(), environment.getId());
                    DeploymentInputs di = deploymentConfigurationDao.findById(DeploymentInputs.class, id);
                    if (di != null) {
                        return true;
                    }
                    DeploymentMatchingConfiguration dmc = deploymentConfigurationDao.findById(DeploymentMatchingConfiguration.class, id);
                    return dmc != null;
                })
                .collect(Collectors.toList());
        if (getInputCandidatesRequest != null && (StringUtils.isNotBlank(getInputCandidatesRequest.getApplicationEnvironmentId())
                || StringUtils.isNotBlank(getInputCandidatesRequest.getApplicationTopologyVersion()))) {
            // If one of this information is given, we can deduct a certain preference
            environmentsWithInputs.sort((left, right) -> {
                // Prefer take input from current environment
                if (Objects.equals(left.getId(), getInputCandidatesRequest.getApplicationEnvironmentId())) {
                    return -1;
                }
                if (Objects.equals(right.getId(), getInputCandidatesRequest.getApplicationEnvironmentId())) {
                    return 1;
                }
                // Prefer take input from environment with the same target topology version than others
                if (Objects.equals(left.getTopologyVersion(), getInputCandidatesRequest.getApplicationTopologyVersion())) {
                    return -1;
                }
                if (Objects.equals(right.getTopologyVersion(), getInputCandidatesRequest.getApplicationTopologyVersion())) {
                    return 1;
                }
                return 0;
            });
        }
        return RestResponseBuilder.<List<ApplicationEnvironment>> builder().data(environmentsWithInputs).build();
    }

    private GetMultipleDataResult<ApplicationEnvironment> searchAuthorizedEnvironments(String applicationId, FilteredSearchRequest searchRequest) {
        FilterBuilder authorizationFilter = getEnvironmentAuthorizationFilters(applicationId);
        Map<String, String[]> applicationEnvironmentFilters = getApplicationEnvironmentFilters(applicationId);
        return alienDAO.search(ApplicationEnvironment.class, searchRequest.getQuery(), applicationEnvironmentFilters, authorizationFilter, null,
                searchRequest.getFrom(), searchRequest.getSize());
    }

    private GetMultipleDataResult<ApplicationEnvironmentDTO> transformToDTO(GetMultipleDataResult<ApplicationEnvironment> searchResult) {
        GetMultipleDataResult<ApplicationEnvironmentDTO> searchResultDTO = new GetMultipleDataResult<ApplicationEnvironmentDTO>();
        searchResultDTO.setQueryDuration(searchResult.getQueryDuration());
        searchResultDTO.setTypes(searchResult.getTypes());
        searchResultDTO.setData(dtoBuilder.getApplicationEnvironmentDTO(searchResult.getData()));
        searchResultDTO.setTotalResults(searchResult.getTotalResults());
        return searchResultDTO;
    }

    private FilterBuilder getEnvironmentAuthorizationFilters(String applicationId) {
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
    @ApiOperation(value = "Get an application environment from its id", notes = "Returns the application environment. Roles required: Application environment [ APPLICATION_USER | DEPLOYMENT_MANAGER ], or application [APPLICATION_MANAGER]")
    @RequestMapping(value = "/{applicationEnvironmentId:.+}", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("isAuthenticated()")
    public RestResponse<ApplicationEnvironmentDTO> getApplicationEnvironment(@PathVariable String applicationId,
            @PathVariable String applicationEnvironmentId) {
        Application application = applicationService.checkAndGetApplication(applicationId);
        ApplicationEnvironment environment = applicationEnvironmentService.getOrFail(applicationEnvironmentId);
        AuthorizationUtil.checkAuthorizationForEnvironment(application, environment, ApplicationEnvironmentRole.values());
        return RestResponseBuilder.<ApplicationEnvironmentDTO> builder().data(dtoBuilder.getApplicationEnvironmentDTO(environment)).build();
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
            throws ExecutionException, InterruptedException {
        Application application = applicationService.checkAndGetApplication(applicationId);
        ApplicationEnvironment environment = applicationEnvironmentService.getOrFail(applicationEnvironmentId);
        AuthorizationUtil.checkAuthorizationForEnvironment(application, environment, ApplicationEnvironmentRole.values());
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
        // User should be APPLICATION_MANAGER to create an environment
        applicationService.checkAndGetApplication(applicationId, ApplicationRole.APPLICATION_MANAGER);

        final Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        ApplicationEnvironment appEnvironment = applicationEnvironmentService.createApplicationEnvironment(auth.getName(), applicationId, request.getName(),
                request.getDescription(), request.getEnvironmentType(), request.getVersionId());

        alienDAO.save(appEnvironment);
        if (StringUtils.isNotBlank(request.getInputCandidate())) {
            // Client ask to copy inputs from other environment
            ApplicationEnvironment environmentToCopyInputs = applicationEnvironmentService.checkAndGetApplicationEnvironment(request.getInputCandidate(),
                    ApplicationRole.APPLICATION_MANAGER);
            applicationEnvironmentService.synchronizeEnvironmentInputs(environmentToCopyInputs, appEnvironment);
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
    @PreAuthorize("isAuthenticated()")
    @Audit
    public RestResponse<Void> update(@PathVariable String applicationId, @PathVariable String applicationEnvironmentId,
            @RequestBody UpdateApplicationEnvironmentRequest request) throws OrchestratorDisabledException {

        // Only APPLICATION_MANAGER on the underlying application can update an application environment
        ApplicationEnvironment applicationEnvironment = applicationEnvironmentService.checkAndGetApplicationEnvironment(applicationEnvironmentId,
                ApplicationRole.APPLICATION_MANAGER);

        if (applicationEnvironment == null) {
            return RestResponseBuilder.<Void> builder().data(null).error(RestErrorBuilder.builder(RestErrorCode.APPLICATION_ENVIRONMENT_ERROR)
                    .message("Application environment with id <" + applicationEnvironmentId + "> does not exist").build()).build();
        }

        if (deployed_check == true && request.getName() != null && (!request.getName().equals(applicationEnvironment.getName()))) {
            if (deploymentService.getActiveDeployment(applicationEnvironmentId) != null) {
                throw new RenameDeployedException("Environment [" + applicationEnvironment.getName() + "] is deployed");
            }
        }

        applicationEnvironmentService.ensureNameUnicity(applicationEnvironment.getApplicationId(), request.getName());
        ReflectionUtil.mergeObject(request, applicationEnvironment);
        if (applicationEnvironment.getName() == null || applicationEnvironment.getName().isEmpty()) {
            throw new UnsupportedOperationException("Application environment name cannot be set to null or empty");
        }
        if (request.getCurrentVersionId() != null) {
            // update the version of the environment
            ApplicationVersion applicationVersion = applicationVersionService
                    .getOrFailByArchiveId(Csar.createId(applicationEnvironment.getApplicationId(), request.getCurrentVersionId()));
            applicationEnvironment.setVersion(applicationVersion.getVersion());
            applicationEnvironment.setTopologyVersion(request.getCurrentVersionId());
        }
        alienDAO.save(applicationEnvironment);
        return RestResponseBuilder.<Void> builder().build();
    }

    /**
     * Use new topology version for environment
     * 
     * @param applicationEnvironmentId environment's id
     * @param applicationId application's id
     * @param request request for new topology's version
     */
    @ApiOperation(value = "Use new topology version for the given application environment", notes = "The logged-in user must have the application manager role for this application. Application role required [ APPLICATION_MANAGER ]")
    @RequestMapping(value = "/{applicationEnvironmentId:.+}/topology-version", method = RequestMethod.PUT, consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("isAuthenticated()")
    @Audit
    public RestResponse<Void> updateTopologyVersion(@PathVariable String applicationId, @PathVariable String applicationEnvironmentId,
            @RequestBody UpdateTopologyVersionForEnvironmentRequest request) throws OrchestratorDisabledException {
        // Only APPLICATION_MANAGER & DEPLOYMENT_MANAGER on the underlying application can update an application environment
        ApplicationEnvironment applicationEnvironment = applicationEnvironmentService.checkAndGetApplicationEnvironment(applicationEnvironmentId,
                ApplicationRole.APPLICATION_MANAGER, ApplicationEnvironmentRole.DEPLOYMENT_MANAGER);

        if (applicationEnvironment == null) {
            return RestResponseBuilder.<Void> builder().data(null).error(RestErrorBuilder.builder(RestErrorCode.APPLICATION_ENVIRONMENT_ERROR)
                    .message("Application environment with id <" + applicationEnvironmentId + "> does not exist").build()).build();
        }
        // update the version of the environment
        ApplicationVersion newApplicationVersion = applicationVersionService
                .getOrFailByArchiveId(Csar.createId(applicationEnvironment.getApplicationId(), request.getNewTopologyVersion()));
        String oldVersion = applicationEnvironment.getVersion();
        String oldTopologyVersion = applicationEnvironment.getTopologyVersion();
        String newVersion = newApplicationVersion.getVersion();
        String newTopologyVersion = request.getNewTopologyVersion();

        if (!Objects.equals(newVersion, oldVersion) || !Objects.equals(newTopologyVersion, oldTopologyVersion)) {
            // Only process if something has changed
            applicationEnvironmentService.updateTopologyVersion(applicationEnvironment, oldTopologyVersion, newVersion, newTopologyVersion,
                    request.getEnvironmentToCopyInput());
        }
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
        ApplicationEnvironment environmentToDelete = applicationEnvironmentService.checkAndGetApplicationEnvironment(applicationEnvironmentId,
                ApplicationRole.APPLICATION_MANAGER);

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

    @Deprecated
    @ApiOperation(value = "Deprecated: Get the id of the topology linked to the environment", notes = "Application role required [ APPLICATION_MANAGER | APPLICATION_DEVOPS ]")
    @RequestMapping(value = "/{applicationEnvironmentId:.+}/topology", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("isAuthenticated()")
    public RestResponse<String> getTopologyId(@PathVariable String applicationId, @PathVariable String applicationEnvironmentId) {
        Application application = applicationService.getOrFail(applicationId);
        ApplicationEnvironment environment = applicationEnvironmentService.getOrFail(applicationEnvironmentId);
        AuthorizationUtil.checkAuthorizationForEnvironment(application, environment, ApplicationEnvironmentRole.values());
        String topologyId = applicationEnvironmentService.getTopologyId(applicationEnvironmentId);
        if (topologyId == null) {
            throw new ApplicationVersionNotFoundException("An application version is required by an application environment.");
        }
        return RestResponseBuilder.<String> builder().data(topologyId).build();
    }
}