package alien4cloud.rest.deployment;

import alien4cloud.application.ApplicationService;
import alien4cloud.audit.annotation.Audit;
import alien4cloud.dao.IGenericSearchDAO;
import alien4cloud.dao.ResponseUtil;
import alien4cloud.dao.model.FacetedSearchResult;
import alien4cloud.dao.model.FetchContext;
import alien4cloud.dao.model.GetMultipleDataResult;
import alien4cloud.deployment.DeploymentLockService;
import alien4cloud.deployment.DeploymentRuntimeStateService;
import alien4cloud.deployment.DeploymentService;
import alien4cloud.deployment.UndeployService;
import alien4cloud.model.deployment.Deployment;
import alien4cloud.model.deployment.DeploymentSourceType;
import alien4cloud.model.deployment.IDeploymentSource;
import alien4cloud.model.orchestrators.locations.Location;
import alien4cloud.orchestrators.locations.services.LocationService;
import alien4cloud.paas.IPaaSCallback;
import alien4cloud.paas.exception.OrchestratorDisabledException;
import alien4cloud.paas.exception.PaaSTechnicalException;
import alien4cloud.paas.model.DeploymentStatus;
import alien4cloud.rest.model.JsonRawRestResponse;
import alien4cloud.rest.model.RestError;
import alien4cloud.rest.model.RestErrorCode;
import alien4cloud.rest.model.RestResponse;
import alien4cloud.rest.model.RestResponseBuilder;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.google.common.util.concurrent.SettableFuture;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import io.swagger.annotations.Authorization;
import org.apache.commons.collections.MapUtils;
import org.apache.commons.lang3.ArrayUtils;
import org.elasticsearch.action.get.MultiGetResponse;
import org.elasticsearch.common.collect.Lists;
import org.elasticsearch.mapping.MappingBuilder;
import org.hibernate.validator.constraints.NotBlank;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import javax.inject.Inject;
import javax.validation.Valid;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

@RestController
@RequestMapping({ "/rest/deployments", "/rest/v1/deployments", "/rest/latest/deployments" })
@Api(value = "", description = "Operations on Deployments")
public class DeploymentController {
    @Resource(name = "alien-es-dao")
    private IGenericSearchDAO alienDAO;
    @Resource
    private DeploymentService deploymentService;
    @Resource
    private ApplicationService applicationService;
    @Inject
    private DeploymentRuntimeStateService deploymentRuntimeStateService;
    @Inject
    private UndeployService undeployService;
    @Inject
    private LocationService locationService;
    @Inject
    private DeploymentLockService deploymentLockService;

    @ApiOperation(value = "Get a deployment from its id.", authorizations = { @Authorization("ADMIN"), @Authorization("APPLICATION_MANAGER") })
    @RequestMapping(value = "/{deploymentId}", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("isAuthenticated()")
    @Audit
    public RestResponse<DeploymentDTO> get(@ApiParam(value = "Deployment id.", required = true) @Valid @NotBlank @PathVariable String deploymentId) {
        return RestResponseBuilder.<DeploymentDTO> builder()
                .data(buildDeploymentsDTOS(false, deploymentService.getOrfail(deploymentId)).stream().findFirst().get()).build();
    }

    /**
     * Search for deployments
     *
     * @return A rest response that contains a {@link FacetedSearchResult} containing deployments, including if asked some details of the related applications..
     */
    @ApiOperation(value = "Search for deployments", notes = "Returns a search result with that contains deployments matching the request.")
    @RequestMapping(value = "/search", method = RequestMethod.GET)
    @PreAuthorize("isAuthenticated()")
    public RestResponse<FacetedSearchResult> search(
            @ApiParam(value = "Query text.") @RequestParam(required = false) String query,
            @ApiParam(value = "Query from the given index.") @RequestParam(required = false, defaultValue = "0") int from,
            @ApiParam(value = "Maximum number of results to retrieve.") @RequestParam(required = false, defaultValue = "50") int size,
            @ApiParam(value = "Id of the orchestrator for which to get deployments. If not provided, get deployments for all orchestrators") @RequestParam(required = false) String orchestratorId,
            @ApiParam(value = "Id of the application for which to get deployments. if not provided, get deployments for all applications") @RequestParam(required = false) String sourceId,
            @ApiParam(value = "Id of the environment for which to get deployments. if not provided, get deployments without filtering by environment") @RequestParam(required = false) String environmentId,
            @ApiParam(value = "include or not the source (application or csar) summary in the results") @RequestParam(required = false, defaultValue = "false") boolean includeSourceSummary
            ) {
        FacetedSearchResult searchResult = deploymentService.searchDeployments(query, orchestratorId, environmentId, sourceId, from, size);
        List<DeploymentDTO> deploymentsDTOS = buildDeploymentsDTOS(includeSourceSummary, (Deployment[]) searchResult.getData());
        searchResult.setData(deploymentsDTOS.toArray());
        return RestResponseBuilder.<FacetedSearchResult> builder().data(searchResult).build();
    }

    /**
     * Get the first 100 deployments for a cloud, including if asked some details of the related applications.
     *
     * @param orchestratorId Id of the orchestrator for which to get deployments (can be null to get deployments for all orchestrators).
     * @param sourceId Id of the application for which to get deployments (can be null to get deployments for all applications).
     * @param environmentId Id of the environment for which to get deployments (can be null to get deployments for all environments).
     * @param includeSourceSummary include or not the sources (application or csar) summary in the results.
     * @return A {@link RestResponse} with as data a list of {@link DeploymentDTO} that contains deployments and applications info.
     */
    @ApiOperation(value = "Get 100 last deployments for an orchestrator.", authorizations = { @Authorization("ADMIN") })
    @RequestMapping(method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasAuthority('ADMIN')")
    public RestResponse<List<DeploymentDTO>> get(
            @ApiParam(value = "Id of the orchestrator for which to get deployments. If not provided, get deployments for all orchestrators") @RequestParam(required = false) String orchestratorId,
            @ApiParam(value = "Id of the application for which to get deployments. if not provided, get deployments for all applications") @RequestParam(required = false) String sourceId,
            @ApiParam(value = "Id of the environment for which to get deployments. if not provided, get deployments without filtering by environment") @RequestParam(required = false) String environmentId,
            @ApiParam(value = "include or not the source (application or csar) summary in the results") @RequestParam(required = false, defaultValue = "false") boolean includeSourceSummary) {
        return RestResponseBuilder.<List<DeploymentDTO>> builder().data(getDeploymentsDTO(orchestratorId, sourceId, environmentId, includeSourceSummary))
                .build();
    }

    /**
     * Get deployments for a given orchestrator, and some info about the related applications and locations
     *
     * @param orchestratorId Id of the orchestrator for which to get deployments (can be null to get deployments for all orchestrator).
     * @param sourceId Id of the application for which to get deployments (can be null to get deployments for all applications).
     * @param environmentId Id of the environment for which to get deployments (can be null to get deployments for all environments).
     * @param includeSourceSummary include or not the applications summary in the results.
     * @return A list of {@link DeploymentDTO} that contains deployments and applications info.
     */
    private List<DeploymentDTO> getDeploymentsDTO(String orchestratorId, String sourceId, String environmentId, boolean includeSourceSummary) {
        Deployment[] deployments = deploymentService.getDeployments(orchestratorId, sourceId, environmentId, 0, 100);
        return buildDeploymentsDTOS(includeSourceSummary, deployments);
    }

    private List<DeploymentDTO> buildDeploymentsDTOS(boolean includeSourceSummary, Deployment... deployments) {
        List<DeploymentDTO> dtos = Lists.newArrayList();
        if (ArrayUtils.isEmpty(deployments)) {
            return dtos;
        }
        Map<String, ? extends IDeploymentSource> sources = Maps.newHashMap();
        // get the app summaries if true
        if (includeSourceSummary) {
            DeploymentSourceType sourceType = deployments[0].getSourceType();
            String[] sourceIds = getSourceIdsFromDeployments(deployments);
            if (sourceIds[0] != null) { // can have no application deployed
                switch (sourceType) {
                case APPLICATION:
                    Map<String, ? extends IDeploymentSource> appSources = applicationService.findByIdsIfAuthorized(FetchContext.SUMMARY, sourceIds);
                    if (appSources != null) {
                        sources = appSources;
                    }
                }
            }
        }

        Map<String, Location> locationsSummariesMap = getRelatedLocationsSummaries(deployments);
        for (Object object : deployments) {
            Deployment deployment = (Deployment) object;
            IDeploymentSource source = sources.get(deployment.getSourceId());
            if (source == null) {
                source = new DeploymentSourceDTO(deployment.getSourceId(), deployment.getSourceName());
            }
            List<Location> locationsSummaries = getLocations(deployment.getLocationIds(), locationsSummariesMap);
            DeploymentDTO dto = new DeploymentDTO(deployment, source, locationsSummaries);
            dtos.add(dto);
        }
        return dtos;
    }

    private Map<String, Location> getRelatedLocationsSummaries(Deployment[] deployments) {
        Set<String> locationIds = Sets.newHashSet();
        for (Deployment deployment : deployments) {
            if (ArrayUtils.isNotEmpty(deployment.getLocationIds())) {
                locationIds.addAll(Sets.newHashSet(deployment.getLocationIds()));
            }
        }
        Map<String, Location> locations = null;
        if (!locationIds.isEmpty()) {
            locations = locationService.findByIds(FetchContext.SUMMARY, locationIds.toArray(new String[locationIds.size()]));
        }

        return locations != null ? locations : Maps.newHashMap();
    }

    private List<Location> getLocations(String[] locationIds, Map<String, Location> locationSummaries) {
        if (MapUtils.isEmpty(locationSummaries) || ArrayUtils.isEmpty(locationIds)) {
            return null;
        }
        List<Location> locations = Lists.newArrayList();
        for (String id : locationIds) {
            if (locationSummaries.containsKey(id)) {
                locations.add(locationSummaries.get(id));
            }
        }

        return locations;
    }

    private String[] getSourceIdsFromDeployments(Deployment[] deployments) {
        List<String> sourceIds = new ArrayList<>(deployments.length);
        for (Deployment deployment : deployments) {
            if (deployment.getSourceId() != null) {// applicationId nul in "deployment" test case
                sourceIds.add(deployment.getSourceId());
            }
        }
        return sourceIds.toArray(new String[sourceIds.size()]);
    }

    @RequestMapping(value = "/{applicationEnvironmentId}/events", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("isAuthenticated()")
    public RestResponse<GetMultipleDataResult> getEvents(
            @ApiParam(value = "Id of the environment for which to get events.", required = true) @Valid @NotBlank @PathVariable String applicationEnvironmentId,
            @ApiParam(value = "Query from the given index.") @RequestParam(required = false, defaultValue = "0") int from,
            @ApiParam(value = "Maximum number of results to retrieve.") @RequestParam(required = false, defaultValue = "50") int size) {
        return RestResponseBuilder.<GetMultipleDataResult> builder()
                .data(deploymentRuntimeStateService.getDeploymentEvents(applicationEnvironmentId, from, size)).build();
    }

    @ApiOperation(value = "Get deployment status from its id.", authorizations = { @Authorization("ADMIN"), @Authorization("APPLICATION_MANAGER") })
    @RequestMapping(value = "/{deploymentId}/status", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("isAuthenticated()")
    public RestResponse<DeploymentStatus> getDeploymentStatus(
            @ApiParam(value = "Deployment id.", required = true) @Valid @NotBlank @PathVariable String deploymentId) {

        Deployment deployment = alienDAO.findById(Deployment.class, deploymentId);
        if (deployment != null) {
            try {
                return deploymentLockService.doWithDeploymentReadLock(deployment.getOrchestratorDeploymentId(), () -> {
                    final SettableFuture<DeploymentStatus> statusSettableFuture = SettableFuture.create();
                    deploymentRuntimeStateService.getDeploymentStatus(deployment, new IPaaSCallback<DeploymentStatus>() {
                        @Override
                        public void onSuccess(DeploymentStatus result) {
                            statusSettableFuture.set(result);
                        }

                        @Override
                        public void onFailure(Throwable t) {
                            statusSettableFuture.setException(t);
                        }
                    });
                    try {
                        DeploymentStatus currentStatus = statusSettableFuture.get();
                        if (DeploymentStatus.UNDEPLOYED.equals(currentStatus)) {
                            deploymentService.markUndeployed(deployment);
                        }
                        return RestResponseBuilder.<DeploymentStatus> builder().data(currentStatus).build();
                    } catch (Exception e) {
                        throw new PaaSTechnicalException("Could not retrieve status from PaaS", e);
                    }
                });
            } catch (OrchestratorDisabledException e) {
                return RestResponseBuilder.<DeploymentStatus> builder().data(null)
                        .error(new RestError(RestErrorCode.CLOUD_DISABLED_ERROR.getCode(), e.getMessage())).build();
            }
        } else {
            return RestResponseBuilder.<DeploymentStatus> builder().data(null)
                    .error(new RestError(RestErrorCode.NOT_FOUND_ERROR.getCode(), "Deployment with id <" + deploymentId + "> was not found.")).build();
        }
    }

    @ApiOperation(value = "Undeploy deployment from its id.", authorizations = { @Authorization("ADMIN"), @Authorization("APPLICATION_MANAGER") })
    @RequestMapping(value = "/{deploymentId}/undeploy", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("isAuthenticated()")
    @Audit
    public RestResponse<Void> undeploy(@ApiParam(value = "Deployment id.", required = true) @Valid @NotBlank @PathVariable String deploymentId) {
        // Check topology status for this deployment object
        Deployment deployment = alienDAO.findById(Deployment.class, deploymentId);

        if (deployment != null) {
            try {
                // Undeploy the topology linked to this deployment
                undeployService.undeploy(null, deploymentId);
            } catch (OrchestratorDisabledException e) {
                return RestResponseBuilder.<Void> builder().data(null).error(new RestError(RestErrorCode.CLOUD_DISABLED_ERROR.getCode(), e.getMessage()))
                        .build();
            }
        } else {
            return RestResponseBuilder.<Void> builder().data(null)
                    .error(new RestError(RestErrorCode.NOT_FOUND_ERROR.getCode(), "Deployment with id <" + deploymentId + "> was not found.")).build();
        }
        return RestResponseBuilder.<Void> builder().data(null).build();
    }

    /**
     * Bulk id request API.
     */
    @ApiOperation(value = "Get a list of deployments from their ids.", authorizations = { @Authorization("ADMIN") })
    @RequestMapping(value = "/bulk/ids", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasAuthority('ADMIN')")
    public JsonRawRestResponse getByIds(@RequestBody String[] deploymentIds) {
        // Check topology status for this deployment object
        MultiGetResponse response = alienDAO.getClient().prepareMultiGet()
                .add(alienDAO.getIndexForType(Deployment.class), MappingBuilder.indexTypeFromClass(Deployment.class), deploymentIds).get();
        JsonRawRestResponse restResponse = new JsonRawRestResponse();
        restResponse.setData(ResponseUtil.rawMultipleData(response));
        return restResponse;
    }
}