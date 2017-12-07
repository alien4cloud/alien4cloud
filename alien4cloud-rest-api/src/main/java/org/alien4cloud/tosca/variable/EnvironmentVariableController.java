package org.alien4cloud.tosca.variable;

import java.util.List;

import javax.inject.Inject;

import org.alien4cloud.tosca.variable.service.VariableDefinitionService;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import alien4cloud.rest.model.RestResponse;
import alien4cloud.rest.model.RestResponseBuilder;
import io.swagger.annotations.Api;

/**
 * Endpoints for variable browsing
 * Browse variables from saved and context (edited) topology
 */

@RestController
@RequestMapping({ "/rest/v2/applications/{applicationId:.+}/environments/variables", "/rest/latest/applications/{applicationId:.+}/environments/variables" })
@Api
public class EnvironmentVariableController {

    @Inject
    private VariableDefinitionService variableDefinitionService;

    @RequestMapping(value = "/{varName}", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("isAuthenticated()")
    public RestResponse<List<EnvironmentVariableDefinitionDTO>> getVariableExpression(@PathVariable String applicationId, @PathVariable String varName,
            @RequestParam(required = false) String envId) {
        List<EnvironmentVariableDefinitionDTO> envDef = variableDefinitionService.getInEnvironmentScope(varName, applicationId, envId);
        return RestResponseBuilder.<List<EnvironmentVariableDefinitionDTO>> builder().data(envDef).build();
    }
}
