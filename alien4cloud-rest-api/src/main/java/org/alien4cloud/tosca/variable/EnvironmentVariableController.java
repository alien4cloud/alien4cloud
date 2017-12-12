package org.alien4cloud.tosca.variable;

import java.util.List;

import javax.inject.Inject;

import org.alien4cloud.tosca.variable.service.VariableExpressionService;
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
@RequestMapping({ "/rest/v2/applications/{applicationId:.+}", "/rest/latest/applications/{applicationId:.+}" })
@Api
public class EnvironmentVariableController {

    @Inject
    private VariableExpressionService variableExpressionService;

    @RequestMapping(value = "/topologyVersion/{topologyVersion}/environments/variables/{varName}", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("isAuthenticated()")
    public RestResponse<List<ScopeVariableExpressionDTO>> getVariableExpressionForEnvironement(@PathVariable String applicationId,
            @PathVariable String topologyVersion, @PathVariable String varName, @RequestParam(required = false) String envId) {
        List<ScopeVariableExpressionDTO> envDef = variableExpressionService.getInEnvironmentScope(varName, applicationId, topologyVersion, envId);
        return RestResponseBuilder.<List<ScopeVariableExpressionDTO>> builder().data(envDef).build();
    }

    @RequestMapping(value = "/topologyVersion/{topologyVersion}/environmentTypes/variables/{varName}", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("isAuthenticated()")
    public RestResponse<List<ScopeVariableExpressionDTO>> getVariableExpressionForEnvironmentType(@PathVariable String applicationId,
            @PathVariable String topologyVersion, @PathVariable String varName, @RequestParam(required = false) String envType) {
        List<ScopeVariableExpressionDTO> envDef = variableExpressionService.getInEnvironmentTypeScope(varName, applicationId, topologyVersion, envType);
        return RestResponseBuilder.<List<ScopeVariableExpressionDTO>> builder().data(envDef).build();
    }
}
