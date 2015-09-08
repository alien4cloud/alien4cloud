package alien4cloud.rest.topology;

import java.util.Map;

import javax.annotation.Resource;

import lombok.extern.slf4j.Slf4j;

import org.hibernate.validator.constraints.NotBlank;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import alien4cloud.audit.annotation.Audit;
import alien4cloud.component.ICSARRepositorySearchService;
import alien4cloud.csar.services.CsarService;
import alien4cloud.dao.IGenericSearchDAO;
import alien4cloud.dao.model.FacetedSearchResult;
import alien4cloud.exception.AlreadyExistException;
import alien4cloud.exception.DeleteReferencedObjectException;
import alien4cloud.exception.InvalidArgumentException;
import alien4cloud.exception.NotFoundException;
import alien4cloud.model.components.CSARDependency;
import alien4cloud.model.components.Csar;
import alien4cloud.model.components.IncompatiblePropertyDefinitionException;
import alien4cloud.model.components.IndexedNodeType;
import alien4cloud.model.templates.TopologyTemplate;
import alien4cloud.model.topology.NodeTemplate;
import alien4cloud.model.topology.SubstitutionMapping;
import alien4cloud.model.topology.SubstitutionTarget;
import alien4cloud.model.topology.Topology;
import alien4cloud.rest.model.RestResponse;
import alien4cloud.rest.model.RestResponseBuilder;
import alien4cloud.topology.TopologyDTO;
import alien4cloud.topology.TopologyService;
import alien4cloud.topology.TopologyServiceCore;
import alien4cloud.topology.TopologyTemplateVersionService;

import com.google.common.collect.Maps;
import com.wordnik.swagger.annotations.ApiOperation;
import com.wordnik.swagger.annotations.ApiParam;

@Slf4j
@RestController
@RequestMapping("/rest/topologies")
public class TopologySubstitutionsController {

    @Resource(name = "alien-es-dao")
    private IGenericSearchDAO alienDAO;

    @Resource
    private TopologyService topologyService;

    @Resource
    private TopologyServiceCore topologyServiceCore;

    @Resource
    private ICSARRepositorySearchService csarRepoSearchService;

    @Resource
    private TopologyTemplateVersionService topologyTemplateVersionService;

    @Resource
    private CsarService csarService;

    @ApiOperation(value = "Define the type this topology can substitute. When this method is called, a new type is created : it is derived from this one.", notes = "Returns a topology with it's details. Role required [ ARCHITECT ]")
    @RequestMapping(value = "/{topologyId}/substitutions/type", method = RequestMethod.PUT, produces = MediaType.APPLICATION_JSON_VALUE)
    @Audit
    @ResponseStatus(value = HttpStatus.CREATED)
    public RestResponse<TopologyDTO> setSubstitutionType(@PathVariable String topologyId, @NotBlank @RequestParam("elementId") String elementId) {
        Topology topology = topologyServiceCore.getOrFail(topologyId);
        topologyService.checkEditionAuthorizations(topology);
        topologyService.throwsErrorIfReleased(topology);
        if (!topology.getDelegateType().equals(TopologyTemplate.class.getSimpleName().toLowerCase())) {
            throw new InvalidArgumentException("This operation is only allowed for topology templates");
        }

        if (topology.getSubstitutionMapping() == null) {
            topology.setSubstitutionMapping(new SubstitutionMapping());
        }

        IndexedNodeType nodeType = csarRepoSearchService.getElementInDependencies(IndexedNodeType.class, elementId, topology.getDependencies());
        if (nodeType != null) {
            // the node type exists in the dependencies, there is no choices for this type version
        } else {
            // the node type does'nt exist in this topology dependencies
            // we need to find the latest version of this component and use it as default
            Map<String, String[]> filters = Maps.newHashMap();
            filters.put("elementId", new String[] { elementId });
            FacetedSearchResult result = csarRepoSearchService.search(IndexedNodeType.class, null, 0, Integer.MAX_VALUE, filters, false);
            if (result.getTotalResults() > 0) {
                nodeType = (IndexedNodeType) result.getData()[0];
            }
            // add in dependencies
            topology.getDependencies().add(new CSARDependency(nodeType.getArchiveName(), nodeType.getArchiveVersion()));
        }
        topology.getSubstitutionMapping().setSubstitutionType(nodeType);
        alienDAO.save(topology);
        topologyServiceCore.updateSubstitutionType(topology);
        return RestResponseBuilder.<TopologyDTO> builder().data(topologyService.buildTopologyDTO(topology)).build();
    }

    @ApiOperation(value = "Remove the substitution type, delete the corresponding type (if not already used)", notes = "Returns a topology with it's details. Role required [ ARCHITECT ]")
    @RequestMapping(value = "/{topologyId}/substitutions/type", method = RequestMethod.DELETE, produces = MediaType.APPLICATION_JSON_VALUE)
    @Audit
    public RestResponse<TopologyDTO> removeSubstitution(@PathVariable String topologyId) {
        Topology topology = topologyServiceCore.getOrFail(topologyId);
        topologyService.checkEditionAuthorizations(topology);
        topologyService.throwsErrorIfReleased(topology);
        if (topology.getSubstitutionMapping() == null || topology.getSubstitutionMapping().getSubstitutionType() == null) {
            throw new NotFoundException("No substitution type has been found");
        }
        IndexedNodeType substitutionType = topology.getSubstitutionMapping().getSubstitutionType();
        Csar csar = csarService.getTopologySubstitutionCsar(topologyId);
        Topology[] topologies = csarService.getDependantTopologies(csar.getName(), csar.getVersion());
        if (topologies != null) {
            for (Topology topologyThatUseCsar : topologies) {
                if (!topologyThatUseCsar.getId().equals(topologyId)) {
                    throw new DeleteReferencedObjectException(
                            "The substitution can not be removed since it's type is already used in at least another topology");
                }
            }
        }
        Csar[] dependantCsars = csarService.getDependantCsars(csar.getName(), csar.getVersion());
        if (dependantCsars != null && dependantCsars.length > 0) {
            throw new DeleteReferencedObjectException("The substitution can not be removed since it's a deendency for another csar");
        }
        topologyService.unloadType(topology, new String[] { substitutionType.getElementId() });
        topology.setSubstitutionMapping(null);
        alienDAO.save(topology);
        // unset the substitution topologyId on the csar
        csar.setSubstitutionTopologyId(null);
        alienDAO.save(csar);
        // delete the CSAR and the type
        csarService.deleteCsar(csar.getId());
        return RestResponseBuilder.<TopologyDTO> builder().data(topologyService.buildTopologyDTO(topology)).build();
    }

    @ApiOperation(value = "Expose the given capability as a capability for the substitution type associated with this topology.", notes = "Role required [ ARCHITECT ]")
    @RequestMapping(value = "/{topologyId:.+}/substitutions/capabilities/{substitutionCapabilityId}", method = RequestMethod.PUT, produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseStatus(value = HttpStatus.CREATED)
    @Audit
    public RestResponse<TopologyDTO> exposeCapability(
            @ApiParam(value = "The topology id.", required = true) @NotBlank @PathVariable final String topologyId,
            @ApiParam(value = "The substitution capability name.", required = true) @NotBlank @PathVariable final String substitutionCapabilityId,
            @ApiParam(value = "The node template id.", required = true) @NotBlank @RequestParam("nodeTemplateName") final String nodeTemplateName,
            @ApiParam(value = "The source node capability id.", required = true) @NotBlank @RequestParam("capabilityId") final String capabilityId)
            throws IncompatiblePropertyDefinitionException {
        Topology topology = topologyServiceCore.getOrFail(topologyId);
        topologyService.checkEditionAuthorizations(topology);
        topologyService.throwsErrorIfReleased(topology);
        if (topology.getNodeTemplates() == null || !topology.getNodeTemplates().containsKey(nodeTemplateName)) {
            throw new NotFoundException("Node " + nodeTemplateName + " do not exist");
        }

        NodeTemplate nodeTemplate = topology.getNodeTemplates().get(nodeTemplateName);
        if (nodeTemplate.getCapabilities() == null || !nodeTemplate.getCapabilities().containsKey(capabilityId)) {
            throw new NotFoundException("Capability " + capabilityId + " do not exist for node " + nodeTemplateName);
        }

        if (topology.getSubstitutionMapping() == null || topology.getSubstitutionMapping().getSubstitutionType() == null) {
            throw new NotFoundException("No substitution type has been found");
        }

        Map<String, SubstitutionTarget> substitutionCapabilities = topology.getSubstitutionMapping().getCapabilities();
        if (substitutionCapabilities == null) {
            substitutionCapabilities = Maps.newHashMap();
            topology.getSubstitutionMapping().setCapabilities(substitutionCapabilities);
        } else if (substitutionCapabilities.containsKey(substitutionCapabilityId)) {
            // ensure name unicity
            throw new AlreadyExistException(String.format("The substitution capability <%s> already exists", substitutionCapabilityId));
        }
        substitutionCapabilities.put(substitutionCapabilityId, new SubstitutionTarget(nodeTemplateName, capabilityId));
        alienDAO.save(topology);
        // update the type
        topologyServiceCore.updateSubstitutionType(topology);
        return RestResponseBuilder.<TopologyDTO> builder().data(topologyService.buildTopologyDTO(topology)).build();
    }

    @ApiOperation(value = "Update the substitution capability (typically change it's name).", notes = "Role required [ ARCHITECT ]")
    @RequestMapping(value = "/{topologyId:.+}/substitutions/capabilities/{substitutionCapabilityId}", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
    @Audit
    public RestResponse<TopologyDTO> updateSubstitutionCapability(
            @ApiParam(value = "The topology id.", required = true) @NotBlank @PathVariable final String topologyId,
            @ApiParam(value = "The substitution capability name.", required = true) @NotBlank @PathVariable final String substitutionCapabilityId,
            @ApiParam(value = "The new capability name.", required = true) @NotBlank @RequestParam("newCapabilityId") final String newCapabilityId)
            throws IncompatiblePropertyDefinitionException {
        Topology topology = topologyServiceCore.getOrFail(topologyId);
        topologyService.checkEditionAuthorizations(topology);
        topologyService.throwsErrorIfReleased(topology);
        if (topology.getSubstitutionMapping() == null || topology.getSubstitutionMapping().getSubstitutionType() == null) {
            throw new NotFoundException("No substitution type has been found");
        }

        Map<String, SubstitutionTarget> substitutionCapabilities = topology.getSubstitutionMapping().getCapabilities();
        return updateSubstitutionKey(topology, substitutionCapabilities, substitutionCapabilityId, newCapabilityId);
    }

    private RestResponse<TopologyDTO> updateSubstitutionKey(Topology topology, Map<String, SubstitutionTarget> targetMap, String oldKey, String newKey) {
        if (targetMap == null) {
            throw new NotFoundException("No substitution capabilities or requirements has been found");
        }
        SubstitutionTarget target = targetMap.remove(oldKey);
        if (target == null) {
            throw new NotFoundException("No substitution capability or requirement has been found for key " + oldKey);
        }
        if (targetMap.containsKey(newKey)) {
            throw new AlreadyExistException(String.format("Can not rename from <%s> to <%s> since capability or requirement <%s> already exists", oldKey,
                    newKey, newKey));
        }
        targetMap.put(newKey, target);
        alienDAO.save(topology);
        // update the type
        topologyServiceCore.updateSubstitutionType(topology);
        return RestResponseBuilder.<TopologyDTO> builder().data(topologyService.buildTopologyDTO(topology)).build();
    }

    @ApiOperation(value = "Remove the substitution capability from the substitution type.", notes = "Role required [ ARCHITECT ]")
    @RequestMapping(value = "/{topologyId:.+}/substitutions/capabilities/{substitutionCapabilityId}", method = RequestMethod.DELETE, produces = MediaType.APPLICATION_JSON_VALUE)
    @Audit
    public RestResponse<TopologyDTO> removeSubstitutionCapability(
            @ApiParam(value = "The topology id.", required = true) @NotBlank @PathVariable final String topologyId,
            @ApiParam(value = "The substitution capability name.", required = true) @NotBlank @PathVariable final String substitutionCapabilityId)
            throws IncompatiblePropertyDefinitionException {
        Topology topology = topologyServiceCore.getOrFail(topologyId);
        topologyService.checkEditionAuthorizations(topology);
        topologyService.throwsErrorIfReleased(topology);
        if (topology.getSubstitutionMapping() == null || topology.getSubstitutionMapping().getSubstitutionType() == null) {
            throw new NotFoundException("No substitution type has been found");
        }

        Map<String, SubstitutionTarget> substitutionCapabilities = topology.getSubstitutionMapping().getCapabilities();
        return removeSubstitutionKey(topology, substitutionCapabilities, substitutionCapabilityId);
    }
    
    private RestResponse<TopologyDTO> removeSubstitutionKey(Topology topology, Map<String, SubstitutionTarget> targetMap, String key) {
        if (targetMap == null) {
            throw new NotFoundException("No substitution capabilities or requirements has been found");
        }
        SubstitutionTarget target = targetMap.remove(key);
        if (target == null) {
            throw new NotFoundException("No substitution capability or requirement has been found for key " + key);
        }
        alienDAO.save(topology);
        // update the type
        topologyServiceCore.updateSubstitutionType(topology);
        return RestResponseBuilder.<TopologyDTO> builder().data(topologyService.buildTopologyDTO(topology)).build();
    }

    @ApiOperation(value = "Expose the given requirement as a requirement for the substitution type associated with this topology.", notes = "Role required [ ARCHITECT ]")
    @RequestMapping(value = "/{topologyId:.+}/substitutions/requirements/{substitutionRequirementId}", method = RequestMethod.PUT, produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseStatus(value = HttpStatus.CREATED)
    @Audit
    public RestResponse<TopologyDTO> exposeRequirement(@ApiParam(value = "The topology id.", required = true) @NotBlank @PathVariable final String topologyId,
            @ApiParam(value = "The substitution requirement name.", required = true) @NotBlank @PathVariable final String substitutionRequirementId,
            @ApiParam(value = "The node template id.", required = true) @NotBlank @RequestParam("nodeTemplateName") final String nodeTemplateName,
            @ApiParam(value = "The source node requirement id.", required = true) @NotBlank @RequestParam("requirementId") final String requirementId)
            throws IncompatiblePropertyDefinitionException {
        Topology topology = topologyServiceCore.getOrFail(topologyId);
        topologyService.checkEditionAuthorizations(topology);
        topologyService.throwsErrorIfReleased(topology);
        if (topology.getNodeTemplates() == null || !topology.getNodeTemplates().containsKey(nodeTemplateName)) {
            throw new NotFoundException("Node " + nodeTemplateName + " do not exist");
        }

        NodeTemplate nodeTemplate = topology.getNodeTemplates().get(nodeTemplateName);
        if (nodeTemplate.getRequirements() == null || !nodeTemplate.getRequirements().containsKey(requirementId)) {
            throw new NotFoundException("Requirement " + requirementId + " do not exist for node " + nodeTemplateName);
        }

        if (topology.getSubstitutionMapping() == null || topology.getSubstitutionMapping().getSubstitutionType() == null) {
            throw new NotFoundException("No substitution type has been found");
        }

        Map<String, SubstitutionTarget> substitutionRequirements = topology.getSubstitutionMapping().getRequirements();
        if (substitutionRequirements == null) {
            substitutionRequirements = Maps.newHashMap();
            topology.getSubstitutionMapping().setRequirements(substitutionRequirements);
        } else if (substitutionRequirements.containsKey(substitutionRequirementId)) {
            // ensure name unicity
            throw new AlreadyExistException(String.format("The substitution requirement <%s> already exists", substitutionRequirementId));
        }
        substitutionRequirements.put(substitutionRequirementId, new SubstitutionTarget(nodeTemplateName, requirementId));
        alienDAO.save(topology);
        // update the type
        topologyServiceCore.updateSubstitutionType(topology);
        return RestResponseBuilder.<TopologyDTO> builder().data(topologyService.buildTopologyDTO(topology)).build();
    }

    @ApiOperation(value = "Update the substitution requirement (typically change it's name).", notes = "Role required [ ARCHITECT ]")
    @RequestMapping(value = "/{topologyId:.+}/substitutions/requirements/{substitutionRequirementId}", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
    @Audit
    public RestResponse<TopologyDTO> updateSubstitutionRequirement(
            @ApiParam(value = "The topology id.", required = true) @NotBlank @PathVariable final String topologyId,
            @ApiParam(value = "The substitution requirement name.", required = true) @NotBlank @PathVariable final String substitutionRequirementId,
            @ApiParam(value = "The new substution requirement name.", required = true) @NotBlank @RequestParam("newRequirementId") final String newRequirementId)
            throws IncompatiblePropertyDefinitionException {
        Topology topology = topologyServiceCore.getOrFail(topologyId);
        topologyService.checkEditionAuthorizations(topology);
        topologyService.throwsErrorIfReleased(topology);
        if (topology.getSubstitutionMapping() == null || topology.getSubstitutionMapping().getSubstitutionType() == null) {
            throw new NotFoundException("No substitution type has been found");
        }

        Map<String, SubstitutionTarget> substitutionRequirements = topology.getSubstitutionMapping().getRequirements();
        return updateSubstitutionKey(topology, substitutionRequirements, substitutionRequirementId, newRequirementId);
    }

    @ApiOperation(value = "Remove the requirement from the substitution type associated to this topology.", notes = "Role required [ ARCHITECT ]")
    @RequestMapping(value = "/{topologyId:.+}/substitutions/requirements/{substitutionRequirementId}", method = RequestMethod.DELETE, produces = MediaType.APPLICATION_JSON_VALUE)
    @Audit
    public RestResponse<TopologyDTO> removeSubstitutionRequirement(
            @ApiParam(value = "The topology id.", required = true) @NotBlank @PathVariable final String topologyId,
            @ApiParam(value = "The substitution requirement name.", required = true) @NotBlank @PathVariable final String substitutionRequirementId)
            throws IncompatiblePropertyDefinitionException {
        Topology topology = topologyServiceCore.getOrFail(topologyId);
        topologyService.checkEditionAuthorizations(topology);
        topologyService.throwsErrorIfReleased(topology);
        if (topology.getSubstitutionMapping() == null || topology.getSubstitutionMapping().getSubstitutionType() == null) {
            throw new NotFoundException("No substitution type has been found");
        }

        Map<String, SubstitutionTarget> substitutionRequirements = topology.getSubstitutionMapping().getRequirements();
        return removeSubstitutionKey(topology, substitutionRequirements, substitutionRequirementId);
    }

}
