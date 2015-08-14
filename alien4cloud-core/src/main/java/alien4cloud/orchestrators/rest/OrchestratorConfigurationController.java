package alien4cloud.orchestrators.rest;

import javax.annotation.Resource;

import com.wordnik.swagger.annotations.Api;
import com.wordnik.swagger.annotations.Authorization;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import alien4cloud.model.orchestrators.OrchestratorConfiguration;
import alien4cloud.orchestrators.services.OrchestratorService;
import alien4cloud.rest.model.RestResponse;
import alien4cloud.rest.model.RestResponseBuilder;

import com.wordnik.swagger.annotations.ApiOperation;
import com.wordnik.swagger.annotations.ApiParam;

@RestController
@RequestMapping(value = "/rest/orchestrators/{id}/configuration", produces = MediaType.APPLICATION_JSON_VALUE)
@Api(value = "Orchestrator Configuration", description = "Get and update orchestrator configuration.", authorizations = { @Authorization("ADMIN") })
public class OrchestratorConfigurationController {
    @Resource
    private OrchestratorService orchestratorService;

    @ApiOperation(value = "Get an orchestrator configuration.")
    @RequestMapping(method = RequestMethod.GET)
    @PreAuthorize("isAuthenticated()")
    public RestResponse<OrchestratorConfiguration> get(@ApiParam(value = "Id of the orchestrator to get", required = true) @PathVariable String id) {
        OrchestratorConfiguration configuration = orchestratorService.getConfigurationOrFail(id);
        return RestResponseBuilder.<OrchestratorConfiguration> builder().data(configuration).build();
    }
}