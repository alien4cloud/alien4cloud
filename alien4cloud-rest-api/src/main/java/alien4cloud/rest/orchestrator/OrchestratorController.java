package alien4cloud.rest.orchestrator;

import javax.inject.Inject;
import javax.validation.Valid;

import lombok.extern.slf4j.Slf4j;

import org.elasticsearch.index.query.FilterBuilder;
import org.hibernate.validator.constraints.NotEmpty;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import alien4cloud.audit.annotation.Audit;
import alien4cloud.dao.model.GetMultipleDataResult;
import alien4cloud.model.orchestrators.Orchestrator;
import alien4cloud.model.orchestrators.OrchestratorState;
import alien4cloud.model.orchestrators.locations.LocationSupport;
import alien4cloud.orchestrators.services.OrchestratorService;
import alien4cloud.orchestrators.services.OrchestratorStateService;
import alien4cloud.paas.exception.PluginConfigurationException;
import alien4cloud.rest.model.RestErrorBuilder;
import alien4cloud.rest.model.RestErrorCode;
import alien4cloud.rest.model.RestResponse;
import alien4cloud.rest.model.RestResponseBuilder;
import alien4cloud.rest.orchestrator.model.CreateOrchestratorRequest;
import alien4cloud.security.AuthorizationUtil;

import com.wordnik.swagger.annotations.Api;
import com.wordnik.swagger.annotations.ApiOperation;
import com.wordnik.swagger.annotations.ApiParam;
import com.wordnik.swagger.annotations.Authorization;

/**
 * Controller to manage orchestrators.
 */
@Slf4j
@RestController
@RequestMapping(value = "/rest/orchestrators", produces = MediaType.APPLICATION_JSON_VALUE)
@Api(value = "Orchestrators", description = "Manages orchestrators.", authorizations = { @Authorization("ADMIN") }, position = 4300)
public class OrchestratorController {
    @Inject
    private OrchestratorService orchestratorService;
    @Inject
    private OrchestratorStateService orchestratorStateService;

    @RequestMapping(method = RequestMethod.POST, consumes = MediaType.APPLICATION_JSON_VALUE)
    @ApiOperation(value = "Create a new orchestrators.", authorizations = { @Authorization("ADMIN") })
    @ResponseStatus(value = HttpStatus.CREATED)
    @PreAuthorize("hasAuthority('ADMIN')")
    @Audit
    public RestResponse<String> create(
            @ApiParam(value = "Request for orchestrators creation", required = true) @Valid @RequestBody CreateOrchestratorRequest orchestratorRequest) {
        String id = orchestratorService.create(orchestratorRequest.getName(), orchestratorRequest.getPluginId(), orchestratorRequest.getPluginBean());
        return RestResponseBuilder.<String> builder().data(id).build();
    }

    @ApiOperation(value = "Update the name of an existing orchestrators.", authorizations = { @Authorization("ADMIN") })
    @RequestMapping(value = "/{id}", method = RequestMethod.PUT, consumes = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasAuthority('ADMIN')")
    @Audit
    public RestResponse<Void> update(@ApiParam(value = "Id of the orchestrators to update.", required = true) @PathVariable @Valid @NotEmpty String id,
            @ApiParam(value = "Orchestrator's new name.", required = true) @Valid @NotEmpty @RequestBody String name) {
        orchestratorService.updateName(id, name);
        return RestResponseBuilder.<Void> builder().build();
    }

    @ApiOperation(value = "Delete an existing orchestrators.", authorizations = { @Authorization("ADMIN") })
    @RequestMapping(value = "/{id}", method = RequestMethod.DELETE)
    @PreAuthorize("hasAuthority('ADMIN')")
    @Audit
    public RestResponse<Void> delete(@ApiParam(value = "Id of the orchestrators to delete.", required = true) @PathVariable @Valid @NotEmpty String id) {
        Orchestrator orchestrator = orchestratorService.getOrFail(id);
        if (!orchestrator.getState().equals(OrchestratorState.DISABLED)) {
            return RestResponseBuilder
                    .<Void> builder()
                    .error(RestErrorBuilder.builder(RestErrorCode.ILLEGAL_STATE_OPERATION).message("An activated orchestrator can not be deleted").build())
                    .build();
        }
        orchestratorService.delete(id);
        return RestResponseBuilder.<Void> builder().build();
    }

    @ApiOperation(value = "Search for orchestrators.")
    @RequestMapping(method = RequestMethod.GET)
    @PreAuthorize("isAuthenticated()")
    public RestResponse<GetMultipleDataResult<Orchestrator>> search(
            @ApiParam(value = "Query text.") @RequestParam(required = false) String query,
            @ApiParam(value = "If true only connected orchestrators will be retrieved.") @RequestParam(required = false, defaultValue = "false") boolean connectedOnly,
            @ApiParam(value = "Query from the given index.") @RequestParam(required = false, defaultValue = "0") int from,
            @ApiParam(value = "Maximum number of results to retrieve.") @RequestParam(required = false, defaultValue = "20") int size) {
        FilterBuilder authorizationFilter = AuthorizationUtil.getResourceAuthorizationFilters();
        OrchestratorState filterStatus = connectedOnly ? OrchestratorState.CONNECTED : null;
        GetMultipleDataResult<Orchestrator> result = orchestratorService.search(query, filterStatus, from, size, authorizationFilter);
        return RestResponseBuilder.<GetMultipleDataResult<Orchestrator>> builder().data(result).build();
    }

    @ApiOperation(value = "Get an orchestrators from it's id.")
    @RequestMapping(value = "/{id}", method = RequestMethod.GET)
    @PreAuthorize("isAuthenticated()")
    public RestResponse<Orchestrator> get(@ApiParam(value = "Id of the orchestrator to get", required = true) @PathVariable String id) {
        // check roles on the requested cloud
        Orchestrator orchestrator = orchestratorService.getOrFail(id);
        return RestResponseBuilder.<Orchestrator> builder().data(orchestrator).build();
    }

    @ApiOperation(value = "Enable an orchestrator. Creates the instance of orchestrator if not already created.", authorizations = { @Authorization("ADMIN") })
    @RequestMapping(value = "/{id}/instance", method = RequestMethod.POST)
    @PreAuthorize("hasAuthority('ADMIN')")
    public RestResponse<Void> enable(@ApiParam(value = "Id of the orchestrator to enable", required = true) @PathVariable String id) {
        Orchestrator orchestrator = orchestratorService.getOrFail(id);
        try {
            orchestratorStateService.enable(orchestrator);
        } catch (PluginConfigurationException e) {
            log.error("Failed to instanciate orchestrator because of invalid configuration.", e);
            return RestResponseBuilder
                    .<Void> builder()
                    .error(RestErrorBuilder.builder(RestErrorCode.INVALID_PLUGIN_CONFIGURATION)
                            .message("Fail to update cloud configuration because Plugin used is not valid.").build()).build();
        }
        return RestResponseBuilder.<Void> builder().build();
    }

    @ApiOperation(value = "Disable an orchestrator. Destroys the instance of the orchestrator connector.", authorizations = { @Authorization("ADMIN") })
    @RequestMapping(value = "/{id}/instance", method = RequestMethod.DELETE)
    @PreAuthorize("hasAuthority('ADMIN')")
    public RestResponse<Void> disable(
            @ApiParam(value = "Id of the orchestrator to enable", required = true) @PathVariable String id,
            @ApiParam(value = "This parameter is useful only when trying to disable the orchestrator, if deployments are performed using this orchestrator disable "
                    + "operation will fail unnless the force flag is true", required = false) @RequestParam(required = false, defaultValue = "false") boolean force,
            @ApiParam(value = "In case an orchestrator with deployment is forced to be disabled, the user may decide to mark all deployments managed "
                    + "by this orchestrator as ended.", required = false) @RequestParam(required = false, defaultValue = "false") boolean clearDeployments) {
        Orchestrator orchestrator = orchestratorService.getOrFail(id);
        orchestratorStateService.disable(orchestrator, force);
        return RestResponseBuilder.<Void> builder().build();
    }

    @ApiOperation(value = "Get information on the locations that an orchestrator can support.", authorizations = { @Authorization("ADMIN") })
    @RequestMapping(value = "/{id}/locationsupport", method = RequestMethod.GET)
    @PreAuthorize("hasAuthority('ADMIN')")
    public RestResponse<LocationSupport> getLocationSupport(
            @ApiParam(value = "Id of the orchestrator for which to get location support informations", required = true) @PathVariable String id) {
        LocationSupport support = orchestratorService.getLocationSupport(id);
        return RestResponseBuilder.<LocationSupport> builder().data(support).build();
    }

    @ApiOperation(value = "Get information on the artifacts that an orchestrator can support.", authorizations = { @Authorization("ADMIN") })
    @RequestMapping(value = "/{id}/artifacts-support", method = RequestMethod.GET)
    @PreAuthorize("hasAuthority('ADMIN')")
    public RestResponse<String[]> getArtifactsSupport(
            @ApiParam(value = "Id of the orchestrator for which to get artifact support informations", required = true) @PathVariable String id) {
        if (orchestratorService.getArtifactSupport(id) == null || orchestratorService.getArtifactSupport(id).getTypes() == null) {
            log.error("An orchestrator should have an artifact support information.");
            return RestResponseBuilder.<String[]> builder().data(null).build();
        }
        String[] supportedArtifacts = orchestratorService.getArtifactSupport(id).getTypes();
        return RestResponseBuilder.<String[]> builder().data(supportedArtifacts).build();
    }

}