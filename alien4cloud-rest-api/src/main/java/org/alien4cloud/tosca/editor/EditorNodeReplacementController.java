package org.alien4cloud.tosca.editor;

import javax.inject.Inject;

import io.swagger.annotations.Api;
import org.hibernate.validator.constraints.NotBlank;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;

import org.alien4cloud.tosca.model.types.NodeType;
import alien4cloud.rest.model.RestResponse;
import alien4cloud.rest.model.RestResponseBuilder;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;

/**
 * This controller provides helper operations to find possible replacement indexedNodeTypes for a node template.
 *
 * This is not intended to be a user API.
 */
@RestController
@RequestMapping({ "/rest/v2/editor/{topologyId}/nodetemplates/{nodeTemplateName}/replacementhelper", "/rest/latest/editor/{topologyId}/nodetemplates/{nodeTemplateName}/replacementhelper" })
@Api(value = "", description = "Node replacement helper")
public class EditorNodeReplacementController {

    @Inject
    private EditorNodeReplacementService editorService;

    /**
     * Get the possible replacement indexedNodeTypes for a node template.
     */
    @ApiOperation(value = "Get the possible type candidates to replace this node.", notes = "Application role required [ APPLICATION_MANAGER | APPLICATION_DEVOPS ]")
    @RequestMapping(method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    public RestResponse<NodeType[]> getReplacementForNode(
            @ApiParam(value = "The topology id.", required = true) @NotBlank @PathVariable final String topologyId,
            @ApiParam(value = "The node temlate id.", required = true) @NotBlank @PathVariable(value = "nodeTemplateName") final String nodeTemplateName) {
        NodeType[] replacementsNodeTypes = editorService.getReplacementForNode(topologyId, nodeTemplateName);
        return RestResponseBuilder.<NodeType[]> builder().data(replacementsNodeTypes).build();
    }
}