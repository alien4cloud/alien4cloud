package alien4cloud.rest.deployment;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.annotation.Resource;
import javax.validation.Valid;

import org.elasticsearch.common.collect.Lists;
import org.hibernate.validator.constraints.NotBlank;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import alien4cloud.application.ApplicationService;
import alien4cloud.cloud.DeploymentService;
import alien4cloud.csar.services.CsarService;
import alien4cloud.dao.IGenericSearchDAO;
import alien4cloud.dao.model.FetchContext;
import alien4cloud.dao.model.GetMultipleDataResult;
import alien4cloud.model.deployment.Deployment;
import alien4cloud.model.deployment.DeploymentSourceType;
import alien4cloud.model.deployment.IDeploymentSource;
import alien4cloud.paas.exception.CloudDisabledException;
import alien4cloud.paas.model.DeploymentStatus;
import alien4cloud.rest.model.RestError;
import alien4cloud.rest.model.RestErrorCode;
import alien4cloud.rest.model.RestResponse;
import alien4cloud.rest.model.RestResponseBuilder;

import com.google.common.collect.Maps;
import com.wordnik.swagger.annotations.ApiOperation;
import com.wordnik.swagger.annotations.ApiParam;
import com.wordnik.swagger.annotations.Authorization;

@RestController
@RequestMapping("/rest/deployments")
public class DeploymentController {
    @Resource
    private DeploymentService deploymentService;
    @Resource
    private ApplicationService applicationService;
    @Resource
    private CsarService csarService;
    @Resource(name = "alien-es-dao")
    private IGenericSearchDAO alienDAO;

    /**
     * Get all deployments for a cloud, including if asked some details of the related applications.
     *
     * @param cloudId Id of the cloud for which to get deployments (can be null to get deployments for all clouds).
     * @param sourceId Id of the application for which to get deployments (can be null to get deployments for all applications).
     * @param includeAppSummary include or not the applications summary in the results.
     * @param from The start index of the query.
     * @param size The maximum number of elements to return.
     * @return A {@link RestResponse} with as data a list of {@link DeploymentDTO} that contains deployments and applications info.
     */
    @ApiOperation(value = "Get deployments for a cloud.", authorizations = { @Authorization("ADMIN") })
    @RequestMapping(method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    public RestResponse<List<DeploymentDTO>> get(
            @ApiParam(value = "Id of the cloud for which to get deployments.") @RequestParam(required = false) String cloudId,
            @ApiParam(value = "Id of the application for which to get deployments.") @RequestParam(required = false) String sourceId,
            @ApiParam(value = "include or not the applications summary in the results") @RequestParam(required = false, defaultValue = "false") boolean includeAppSummary,
            @ApiParam(value = "Query from the given index.") @RequestParam(required = false, defaultValue = "0") int from,
            @ApiParam(value = "Maximum number of results to retrieve.") @RequestParam(required = false, defaultValue = "20") int size) {
        return RestResponseBuilder.<List<DeploymentDTO>> builder().data(getDeploymentsAndSources(cloudId, sourceId, includeAppSummary, from, size))
                .build();
    }

    /**
     * Get deployments for a given cloud, and some info about the related applications
     *
     * @param cloudId Id of the cloud for which to get deployments (can be null to get deployments for all clouds).
     * @param sourceId Id of the application for which to get deployments (can be null to get deployments for all applications).
     * @param includeSourceSummary include or not the applications summary in the results.
     * @param from The start index of the query.
     * @param size The maximum number of elements to return.
     * @return A list of {@link DeploymentDTO} that contains deployments and applications info.
     */
    private List<DeploymentDTO> getDeploymentsAndSources(String cloudId, String sourceId, boolean includeSourceSummary, int from, int size) {
        GetMultipleDataResult results = deploymentService.getDeployments(cloudId, sourceId, from, size);
        List<DeploymentDTO> dtos = Lists.newArrayList();
        if (results.getData().length > 0) {
            Object[] deployments = results.getData();
            Map<String, ? extends IDeploymentSource> sources = Maps.newHashMap();
            // get the app summaries if true
            if (includeSourceSummary) {
                DeploymentSourceType sourceType = ((Deployment) deployments[0]).getSourceType();
                String[] sourceIds = getSourceIdsFromDeployments(deployments);
                if (sourceIds != null) {// can have no application deployed
                    switch (sourceType) {
                    case APPLICATION:
                        sources = applicationService.findByIdsIfAuthorized(FetchContext.DEPLOYMENT, sourceIds);
                        break;
                    case CSAR:
                        sources = csarService.findByIds(FetchContext.DEPLOYMENT, sourceIds);
                    }
                }
            }
            for (Object object : deployments) {
                Deployment deployment = (Deployment) object;
                DeploymentDTO dto = new DeploymentDTO(deployment, sources.get(deployment.getSourceId()));
                dtos.add(dto);
            }
        }
        return dtos;
    }

    private String[] getSourceIdsFromDeployments(Object[] deployments) {
        List<String> sourceIds = new ArrayList<>(deployments.length);
        for (Object object : deployments) {
            Deployment deployment = (Deployment) object;
            if (deployment.getSourceId() != null) {// applicationId nul in "deployment" test case
                sourceIds.add(deployment.getSourceId());
            }
        }
        return sourceIds.toArray(new String[sourceIds.size()]);
    }

    @RequestMapping(value = "/{topologyId}/events", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    public RestResponse<GetMultipleDataResult> getEvents(
            @ApiParam(value = "Id of the topology for which to get events.", required = true) @Valid @NotBlank @PathVariable String topologyId,
            @ApiParam(value = "Id of the cloud on which the topology is deployed.") @RequestParam(required = true) String cloudId,
            @ApiParam(value = "Query from the given index.") @RequestParam(required = false, defaultValue = "0") int from,
            @ApiParam(value = "Maximum number of results to retrieve.") @RequestParam(required = false, defaultValue = "50") int size) {
        return RestResponseBuilder.<GetMultipleDataResult> builder().data(deploymentService.getDeploymentEvents(topologyId, cloudId, from, size)).build();
    }

    @ApiOperation(value = "Get deployment status from its id.", authorizations = { @Authorization("ADMIN"), @Authorization("APPLICATION_MANAGER") })
    @RequestMapping(value = "/{deploymentId}/status", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    public RestResponse<DeploymentStatus> getDeploymentStatus(
            @ApiParam(value = "Deployment id.", required = true) @Valid @NotBlank @PathVariable String deploymentId) {

        Deployment deployment = alienDAO.findById(Deployment.class, deploymentId);
        DeploymentStatus status = null;
        if (deployment != null) {
            try {
                status = deploymentService.getDeploymentStatus(deployment.getTopologyId(), deployment.getCloudId());
            } catch (CloudDisabledException e) {
                return RestResponseBuilder.<DeploymentStatus> builder().data(null)
                        .error(new RestError(RestErrorCode.CLOUD_DISABLED_ERROR.getCode(), e.getMessage())).build();
            }
        } else {
            return RestResponseBuilder.<DeploymentStatus> builder().data(null)
                    .error(new RestError(RestErrorCode.NOT_FOUND_ERROR.getCode(), "Deployment with id <" + deploymentId + "> was not found.")).build();
        }

        return RestResponseBuilder.<DeploymentStatus> builder().data(status).build();
    }

    @ApiOperation(value = "Undeploy deployment from its id.", authorizations = { @Authorization("ADMIN"), @Authorization("APPLICATION_MANAGER") })
    @RequestMapping(value = "/{deploymentId}/undeploy", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    public RestResponse<Void> undeploy(@ApiParam(value = "Deployment id.", required = true) @Valid @NotBlank @PathVariable String deploymentId) {

        // Check topology status for this deployment object
        Deployment deployment = alienDAO.findById(Deployment.class, deploymentId);

        if (deployment != null) {
            try {
                // Undeploy the topology linked to this deployment
                deploymentService.undeploy(deploymentId, deployment.getCloudId());
            } catch (CloudDisabledException e) {
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
