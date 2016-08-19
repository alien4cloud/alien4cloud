package org.alien4cloud.tosca.editor;

import java.util.List;

import javax.inject.Inject;

import org.hibernate.validator.constraints.NotBlank;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;

import alien4cloud.model.components.IndexedCapabilityType;
import alien4cloud.model.components.IndexedNodeType;
import alien4cloud.model.components.IndexedRelationshipType;
import alien4cloud.model.topology.Capability;
import alien4cloud.model.topology.RelationshipTemplate;
import alien4cloud.rest.model.RestResponse;
import alien4cloud.rest.model.RestResponseBuilder;
import alien4cloud.tosca.context.ToscaContext;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;

/**
 * This controller provides helper operations to find input candidates from the editor.
 *
 * This is not intended to be a user API.
 */
@RestController
@RequestMapping({ "/rest/v2/editor/{topologyId}/inputhelper", "/rest/latest/editor/{topologyId}/inputhelper" })
public class EditorInputHelperController {
    @Inject
    private EditorInputHelperService editorService;

    /**
     * Get the possible inputs candidates to be associated with this node template property.
     */
    @ApiOperation(value = "Get the possible inputs candidates to be associated with this property.", notes = "Application role required [ APPLICATION_MANAGER | APPLICATION_DEVOPS ]")
    @RequestMapping(value = "/node", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    public RestResponse<List<String>> getNodetemplatePropertyInputCandidate(
            @ApiParam(value = "The topology id.", required = true) @NotBlank @PathVariable final String topologyId,
            @ApiParam(value = "The node temlate id.", required = true) @NotBlank @RequestParam(value = "nodeTemplateName") final String nodeTemplateName,
            @ApiParam(value = "The property id.", required = true) @NotBlank @RequestParam("propertyId") final String propertyId) {
        List<String> inputCandidates = editorService.getInputCandidates(topologyId, nodeTemplateName,
                nodeTemplate -> ToscaContext.get(IndexedNodeType.class, nodeTemplate.getType()).getProperties().get(propertyId));
        return RestResponseBuilder.<List<String>> builder().data(inputCandidates).build();
    }

    /**
     * Get the possible inputs candidates to be associated with this capability property.
     */
    @ApiOperation(value = "Get the possible inputs candidates to be associated with this capability property.", notes = "Application role required [ APPLICATION_MANAGER | APPLICATION_DEVOPS ]")
    @RequestMapping(value = "/capability", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    public RestResponse<List<String>> getCapabilitiesPropertyInputCandidate(
            @ApiParam(value = "The topology id.", required = true) @NotBlank @PathVariable final String topologyId,
            @ApiParam(value = "The node temlate id.", required = true) @NotBlank @RequestParam(value = "nodeTemplateName") final String nodeTemplateName,
            @ApiParam(value = "The property id.", required = true) @NotBlank @RequestParam(value = "propertyId") final String propertyId,
            @ApiParam(value = "The capability template id.", required = true) @NotBlank @RequestParam(value = "capabilityId") final String capabilityId) {
        List<String> inputCandidates = editorService.getInputCandidates(topologyId, nodeTemplateName, nodeTemplate -> {
            Capability capabilityTemplate = nodeTemplate.getCapabilities().get(capabilityId);
            return ToscaContext.get(IndexedCapabilityType.class, capabilityTemplate.getType()).getProperties().get(propertyId);
        });
        return RestResponseBuilder.<List<String>> builder().data(inputCandidates).build();
    }

    /**
     * Get the possible inputs candidates to be associated with this relationship property.
     */
    @ApiOperation(value = "Get the possible inputs candidates to be associated with this relationship property.", notes = "Application role required [ APPLICATION_MANAGER | APPLICATION_DEVOPS ]")
    @RequestMapping(value = "/relationship", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    public RestResponse<List<String>> getRelationshipPropertyInputCandidate(
            @ApiParam(value = "The topology id.", required = true) @NotBlank @PathVariable final String topologyId,
            @ApiParam(value = "The node temlate id.", required = true) @NotBlank @RequestParam(value = "nodeTemplateName") final String nodeTemplateName,
            @ApiParam(value = "The property id.", required = true) @NotBlank @RequestParam(value = "propertyId") final String propertyId,
            @ApiParam(value = "The relationship template id.", required = true) @NotBlank @RequestParam(value = "relationshipId") final String relationshipId) {
        List<String> inputCandidates = editorService.getInputCandidates(topologyId, nodeTemplateName, nodeTemplate -> {
            RelationshipTemplate relationshipTemplate = nodeTemplate.getRelationships().get(relationshipId);
            return ToscaContext.get(IndexedRelationshipType.class, relationshipTemplate.getType()).getProperties().get(propertyId);
        });
        return RestResponseBuilder.<List<String>> builder().data(inputCandidates).build();
    }
}