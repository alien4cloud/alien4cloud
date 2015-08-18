package alien4cloud.orchestrators.rest;

import javax.annotation.Resource;

import alien4cloud.audit.annotation.Audit;
import alien4cloud.orchestrators.services.OrchestratorConfigurationService;
import alien4cloud.paas.exception.PluginConfigurationException;
import alien4cloud.rest.model.RestErrorBuilder;
import alien4cloud.rest.model.RestErrorCode;
import com.wordnik.swagger.annotations.Api;
import com.wordnik.swagger.annotations.Authorization;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import alien4cloud.model.orchestrators.OrchestratorConfiguration;
import alien4cloud.orchestrators.services.OrchestratorService;
import alien4cloud.rest.model.RestResponse;
import alien4cloud.rest.model.RestResponseBuilder;

import com.wordnik.swagger.annotations.ApiOperation;
import com.wordnik.swagger.annotations.ApiParam;

import java.io.IOException;

@Slf4j
@RestController
@RequestMapping(value = "/rest/orchestrators/{id}/configuration", produces = MediaType.APPLICATION_JSON_VALUE)
@Api(value = "Orchestrator Configuration", description = "Get and update orchestrator configuration.", authorizations = { @Authorization("ADMIN") }, position = 4310)
public class OrchestratorConfigurationController {
    @Resource
    private OrchestratorConfigurationService orchestratorService;

    @ApiOperation(value = "Get an orchestrator configuration.", authorizations = { @Authorization("ADMIN") })
    @RequestMapping(method = RequestMethod.GET)
    @PreAuthorize("hasAuthority('ADMIN')")
    public RestResponse<OrchestratorConfiguration> get(@ApiParam(value = "Id of the orchestrator to get", required = true) @PathVariable String id) {
        OrchestratorConfiguration configuration = orchestratorService.getConfigurationOrFail(id);
        return RestResponseBuilder.<OrchestratorConfiguration> builder().data(configuration).build();
    }

    @ApiOperation(value = "Update the configuration for an orchestrator.", authorizations = { @Authorization("ADMIN") })
    @RequestMapping(method = RequestMethod.PUT, consumes = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasAuthority('ADMIN')")
    @Audit
    public RestResponse<Void> update(
            @ApiParam(value = "Id of the orchestrator for which to update the configuration.", required = true) @PathVariable String id,
            @ApiParam(value = "The configuration object for the orchestrator - Type depends of the selected orchestrator.", required = true) @RequestBody Object configuration) {
        try {
            Object configurationObject = orchestratorService.configurationAsValidObject(id, configuration);
            orchestratorService.updateConfiguration(id, configurationObject);
        } catch (IOException e) {
            log.error("Failed to update cloud configuration. Specified json cannot be processed.", e);
            return RestResponseBuilder
                    .<Void> builder()
                    .error(RestErrorBuilder.builder(RestErrorCode.INVALID_PLUGIN_CONFIGURATION).message("Fail to parse the provided plugin configuration.")
                            .build()).build();
        } catch (PluginConfigurationException e) {
            log.error("Failed to update cloud configuration.", e);
            return RestResponseBuilder
                    .<Void> builder()
                    .error(RestErrorBuilder.builder(RestErrorCode.INVALID_PLUGIN_CONFIGURATION)
                            .message("Fail to update cloud configuration because Plugin used is not valid.").build()).build();
        }

        return RestResponseBuilder.<Void> builder().build();
    }
}