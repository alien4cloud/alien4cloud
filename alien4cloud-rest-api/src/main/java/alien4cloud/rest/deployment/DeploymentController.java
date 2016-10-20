package alien4cloud.rest.deployment;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.annotation.Resource;
import javax.inject.Inject;
import javax.validation.Valid;

import org.alien4cloud.tosca.catalog.index.ICsarService;
import org.apache.commons.collections.MapUtils;
import org.apache.commons.lang3.ArrayUtils;
import org.elasticsearch.common.collect.Lists;
import org.hibernate.validator.constraints.NotBlank;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.context.request.async.DeferredResult;

import com.google.common.collect.Maps;
import com.google.common.collect.Sets;

import alien4cloud.application.ApplicationService;
import alien4cloud.audit.annotation.Audit;
import alien4cloud.dao.IGenericSearchDAO;
import alien4cloud.dao.model.FetchContext;
import alien4cloud.dao.model.GetMultipleDataResult;
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
import alien4cloud.paas.model.DeploymentStatus;
import alien4cloud.rest.model.RestError;
import alien4cloud.rest.model.RestErrorCode;
import alien4cloud.rest.model.RestResponse;
import alien4cloud.rest.model.RestResponseBuilder;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import io.swagger.annotations.Authorization;

@RestController
@RequestMapping({ "/rest/deployments", "/rest/v1/deployments", "/rest/latest/deployments" })
public class DeploymentController {
    @Resource(name = "alien-es-dao")
    private IGenericSearchDAO alienDAO;
    @Resource
    private DeploymentService deploymentService;
    @Resource
    private ApplicationService applicationService;
    @Resource
    private ICsarService csarService;
    @Inject
    private DeploymentRuntimeStateService deploymentRuntimeStateService;
    @Inject
    private UndeployService undeployService;
    @Inject
    private LocationService locationService;

    /**
     * Get all deployments for a cloud, including if asked some details of the related applications.
     *
     * @param orchestratorId Id of the orchestrator for which to get deployments (can be null to get deployments for all orchestrators).
     * @param sourceId Id of the application for which to get deployments (can be null to get deployments for all applications).
     * @param includeSourceSummary include or not the sources (application or csar) summary in the results.
     * @return A {@link RestResponse} with as data a list of {@link DeploymentDTO} that contains deployments and applications info.
     */
    @ApiOperation(value = "Get deployments for an orchestrator.", authorizations = { @Authorization("ADMIN") })
    @RequestMapping(method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("isAuthenticated()")
    public RestResponse<List<DeploymentDTO>> get(
            @ApiParam(value = "Id of the orchestrator for which to get deployments. If not provided, get deployments for all orchestrators") @RequestParam(required = false) String orchestratorId,
            @ApiParam(value = "Id of the application for which to get deployments. if not provided, get deployments for all applications") @RequestParam(required = false) String sourceId,
            @ApiParam(value = "include or not the source (application or csar) summary in the results") @RequestParam(required = false, defaultValue = "false") boolean includeSourceSummary) {
        return RestResponseBuilder.<List<DeploymentDTO>> builder().data(buildDeploymentsDTO(orchestratorId, sourceId, includeSourceSummary)).build();
    }

    /**
     * Get deployments for a given orchestrator, and some info about the related applications and locations
     *
     * @param orchestratorId Id of the orchestrator for which to get deployments (can be null to get deployments for all orchestrator).
     * @param sourceId Id of the application for which to get deployments (can be null to get deployments for all applications).
     * @param includeSourceSummary include or not the applications summary in the results.
     * @return A list of {@link DeploymentDTO} that contains deployments and applications info.
     */
    private List<DeploymentDTO> buildDeploymentsDTO(String orchestratorId, String sourceId, boolean includeSourceSummary) {
        List<Deployment> deployments = deploymentService.getDeployments(orchestratorId, sourceId);
        List<DeploymentDTO> dtos = Lists.newArrayList();
        if (deployments != null && deployments.size() > 0) {
            Map<String, ? extends IDeploymentSource> sources = Maps.newHashMap();
            // get the app summaries if true
            if (includeSourceSummary) {
                DeploymentSourceType sourceType = deployments.get(0).getSourceType();
                String[] sourceIds = getSourceIdsFromDeployments(deployments);
                if (sourceIds[0] != null) { // can have no application deployed
                    switch (sourceType) {
                    case APPLICATION:
                        Map<String, ? extends IDeploymentSource> appSources = applicationService.findByIdsIfAuthorized(FetchContext.SUMMARY, sourceIds);
                        if (appSources != null) {
                            sources = appSources;
                        }
                        // FIXME Allow CSAR deployment again for testing and validation purpose.
                        // break;
                        // case CSAR:
                        // Map<String, ? extends IDeploymentSource> csarSources = csarService.findByIds(FetchContext.SUMMARY, sourceIds);
                        // if (csarSources != null) {
                        // sources = csarSources;
                        // }
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
        }
        return dtos;
    }

    private Map<String, Location> getRelatedLocationsSummaries(List<Deployment> deployments) {
        Set<String> locationIds = Sets.newHashSet();
        for (Deployment deployment : deployments) {
            if (ArrayUtils.isNotEmpty(deployment.getLocationIds())) {
                locationIds.addAll(Sets.newHashSet(deployment.getLocationIds()));
            }
        }
        Map<String, Location> locations = null;
        if (!locationIds.isEmpty()) {
            locations = locationService.findByIdsIfAuthorized(FetchContext.SUMMARY, locationIds.toArray(new String[0]));
        }

        return locations != null ? locations : Maps.<String, Location> newHashMap();
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

    private String[] getSourceIdsFromDeployments(List<Deployment> deployments) {
        List<String> sourceIds = new ArrayList<>(deployments.size());
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
    public DeferredResult<RestResponse<DeploymentStatus>> getDeploymentStatus(
            @ApiParam(value = "Deployment id.", required = true) @Valid @NotBlank @PathVariable String deploymentId) {

        Deployment deployment = alienDAO.findById(Deployment.class, deploymentId);
        final DeferredResult<RestResponse<DeploymentStatus>> statusResult = new DeferredResult<>(5L * 60L * 1000L);
        if (deployment != null) {
            try {
                deploymentRuntimeStateService.getDeploymentStatus(deployment, new IPaaSCallback<DeploymentStatus>() {
                    @Override
                    public void onSuccess(DeploymentStatus result) {
                        statusResult.setResult(RestResponseBuilder.<DeploymentStatus> builder().data(result).build());
                    }

                    @Override
                    public void onFailure(Throwable t) {
                        statusResult.setErrorResult(t);
                    }
                });
                return statusResult;
            } catch (OrchestratorDisabledException e) {
                statusResult.setResult(RestResponseBuilder.<DeploymentStatus> builder().data(null)
                        .error(new RestError(RestErrorCode.CLOUD_DISABLED_ERROR.getCode(), e.getMessage())).build());
            }
        } else {
            statusResult.setResult(RestResponseBuilder.<DeploymentStatus> builder().data(null)
                    .error(new RestError(RestErrorCode.NOT_FOUND_ERROR.getCode(), "Deployment with id <" + deploymentId + "> was not found.")).build());
        }

        return statusResult;
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
                undeployService.undeploy(deploymentId);
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

}