package alien4cloud.orchestrators.rest;

import javax.annotation.Resource;
import javax.validation.Valid;

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
import alien4cloud.orchestrators.rest.model.CreateOrchestratorRequest;
import alien4cloud.orchestrators.services.OrchestratorService;
import alien4cloud.rest.model.RestResponse;
import alien4cloud.rest.model.RestResponseBuilder;
import alien4cloud.security.AuthorizationUtil;
import alien4cloud.security.model.CloudRole;

import com.wordnik.swagger.annotations.Api;
import com.wordnik.swagger.annotations.ApiOperation;
import com.wordnik.swagger.annotations.ApiParam;
import com.wordnik.swagger.annotations.Authorization;

/**
 * Controller to manage orchestrators.
 */
@RestController
@RequestMapping(value = "/rest/orchestrators", produces = MediaType.APPLICATION_JSON_VALUE)
@Api(value = "Orchestrators", description = "Manages orchestrators.", authorizations = { @Authorization("ADMIN") }, position = 4300)
public class OrchestratorController {
    @Resource
    private OrchestratorService orchestratorService;

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
    public void update(@ApiParam(value = "Id of the orchestrators to update.", required = true) @PathVariable @Valid @NotEmpty String id,
            @ApiParam(value = "Orchestrator's new name.", required = true) @Valid @NotEmpty @RequestBody String name) {
        orchestratorService.updateName(id, name);
    }

    @ApiOperation(value = "Delete an existing orchestrators.", authorizations = { @Authorization("ADMIN") })
    @RequestMapping(value = "/{id}", method = RequestMethod.DELETE)
    @PreAuthorize("hasAuthority('ADMIN')")
    @Audit
    public void delete(@ApiParam(value = "Id of the orchestrators to delete.", required = true) @PathVariable @Valid @NotEmpty String id) {
        orchestratorService.delete(id);
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
        AuthorizationUtil.checkAuthorizationForCloud(orchestrator, CloudRole.CLOUD_DEPLOYER);
        return RestResponseBuilder.<Orchestrator> builder().data(orchestrator).build();
    }

    @ApiOperation(value = "Enable an orchestrator.", authorizations = { @Authorization("ADMIN") })
    @RequestMapping(value = "/{id}/enable", method = RequestMethod.PUT)
    @PreAuthorize("hasAuthority('ADMIN')")
    public RestResponse<Void> get(
            @ApiParam(value = "Id of the orchestrator to enable", required = true) @PathVariable String id,
            @ApiParam(value = "If true the orchestrator will be initialized and will try to connect. If false the orchestrator will be disabled", required = false) @RequestParam(required = false, defaultValue = "true") boolean enable,
            @ApiParam(value = "This parameter is useful only when trying to disable the orchestrator, if deployments are performed using this orchestrator disable "
                    + "operation will fail unnless the force flag is true", required = false) @RequestParam(required = false, defaultValue = "false") boolean force) {
        if (enable) {
            // orchestratorService.
        } else {

        }
        return RestResponseBuilder.<Void> builder().build();
    }
}