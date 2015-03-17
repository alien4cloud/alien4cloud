package alien4cloud.rest.topology;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import javax.annotation.Resource;

import lombok.extern.slf4j.Slf4j;

import org.hibernate.validator.constraints.NotBlank;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import alien4cloud.component.ICSARRepositorySearchService;
import alien4cloud.dao.IGenericSearchDAO;
import alien4cloud.exception.AlreadyExistException;
import alien4cloud.exception.NotFoundException;
import alien4cloud.model.components.AbstractPropertyValue;
import alien4cloud.model.components.FunctionPropertyValue;
import alien4cloud.model.components.IncompatiblePropertyDefinitionException;
import alien4cloud.model.components.IndexedCapabilityType;
import alien4cloud.model.components.IndexedNodeType;
import alien4cloud.model.components.IndexedRelationshipType;
import alien4cloud.model.components.PropertyDefinition;
import alien4cloud.model.components.ScalarPropertyValue;
import alien4cloud.model.topology.Capability;
import alien4cloud.model.topology.NodeTemplate;
import alien4cloud.model.topology.RelationshipTemplate;
import alien4cloud.model.topology.Topology;
import alien4cloud.rest.model.RestResponse;
import alien4cloud.rest.model.RestResponseBuilder;
import alien4cloud.topology.TopologyServiceCore;
import alien4cloud.tosca.normative.ToscaFunctionConstants;

import com.google.common.collect.Maps;
import com.wordnik.swagger.annotations.ApiOperation;
import com.wordnik.swagger.annotations.ApiParam;

@Slf4j
@RestController
@RequestMapping("/rest/topologies")
public class TopologyInputsController {

    @Resource(name = "alien-es-dao")
    private IGenericSearchDAO alienDAO;

    @Resource
    private TopologyService topologyService;

    @Resource
    private TopologyServiceCore topologyServiceCore;

    @Resource
    private ICSARRepositorySearchService csarRepoSearchService;

    /**
     * Add a new input.
     *
     * @param topologyId The id of the topology to retrieve.
     * @return
     */
    @ApiOperation(value = "Activate a property as an input property.", notes = "Activate a property as an input property. Application role required [ APPLICATION_MANAGER | APPLICATION_DEVOPS ]")
    @RequestMapping(value = "/{topologyId}/inputs/{inputId}", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
    public RestResponse<Void> addInput(@ApiParam(value = "The topology id.", required = true) @NotBlank @PathVariable final String topologyId,
            @ApiParam(value = "The name of new input.", required = true) @NotBlank @PathVariable final String inputId,
            @ApiParam(value = "The property definition of the new input.", required = true) @RequestBody PropertyDefinition newPropertyDefinition) {
        Topology topology = topologyServiceCore.getMandatoryTopology(topologyId);
        topologyService.checkEditionAuthorizations(topology);
        Map<String, PropertyDefinition> inputs = getInputs(topology, true);

        if (inputs.containsKey(inputId)) {
            throw new AlreadyExistException("An input with the id " + inputId + "already exist in the topology " + topologyId);
        }

        inputs.put(inputId, newPropertyDefinition);
        topology.setInputs(inputs);

        log.debug("Add a new input <{}> for the topology <{}>.", inputId, topologyId);
        alienDAO.save(topology);
        return RestResponseBuilder.<Void> builder().build();
    }

    /**
     * Update the inputId for the {@link FunctionPropertyValue} of a Map of properties.
     *
     * @param properties
     * @param oldInputId
     * @param newInputId
     */
    private void updateInputIdInProperties(final Map<String, AbstractPropertyValue> properties, final String oldInputId, final String newInputId) {
        for (AbstractPropertyValue propertyValue : properties.values()) {
            if (propertyValue instanceof FunctionPropertyValue) {
                FunctionPropertyValue functionPropertyValue = (FunctionPropertyValue) propertyValue;
                if (ToscaFunctionConstants.GET_INPUT.equals(functionPropertyValue.getFunction())
                        && functionPropertyValue.getParameters().get(0).equals(oldInputId)) {
                    functionPropertyValue.setParameters(Arrays.asList(newInputId));
                }
            }
        }
    }

    /**
     * Change the name of an input parameter. This will update all the get_input function used in node templates and relationship template to set the new value.
     *
     * @param inputId
     * @param newInputId
     */
    @ApiOperation(value = "Change the name of an input parameter.", notes = "Application role required [ APPLICATION_MANAGER | APPLICATION_DEVOPS ]")
    @RequestMapping(value = "/{topologyId:.+}/inputs/{inputId}", method = RequestMethod.PUT, produces = MediaType.APPLICATION_JSON_VALUE)
    public RestResponse<TopologyDTO> updateInputId(@ApiParam(value = "The topology id.", required = true) @NotBlank @PathVariable final String topologyId,
            @ApiParam(value = "The name of the old input.", required = true) @NotBlank @PathVariable final String inputId,
            @ApiParam(value = "The name of the new input.", required = true) @NotBlank @RequestParam final String newInputId) {
        Topology topology = topologyServiceCore.getMandatoryTopology(topologyId);
        topologyService.checkEditionAuthorizations(topology);

        Map<String, PropertyDefinition> inputs = topology.getInputs();
        if (inputs == null || !inputs.containsKey(inputId)) {
            throw new NotFoundException("Input " + inputId + " not found");
        }

        if (inputs.containsKey(newInputId)) {
            throw new AlreadyExistException("Input " + newInputId + " already existed");
        }
        PropertyDefinition propertyDefinition = inputs.remove(inputId);
        inputs.put(newInputId, propertyDefinition);

        Map<String, NodeTemplate> nodeTemplates = topology.getNodeTemplates();
        for (NodeTemplate nodeTemp : nodeTemplates.values()) {
            updateInputIdInProperties(nodeTemp.getProperties(), inputId, newInputId);
            if (nodeTemp.getRelationships() != null) {
                for (RelationshipTemplate relationshipTemplate : nodeTemp.getRelationships().values()) {
                    updateInputIdInProperties(relationshipTemplate.getProperties(), inputId, newInputId);
                }
            }
        }

        log.debug("Change the name of an input parameter <{}> to <{}> for the topology ", inputId, newInputId, topologyId);
        alienDAO.save(topology);
        return RestResponseBuilder.<TopologyDTO> builder().data(topologyService.buildTopologyDTO(topology)).build();
    }

    /**
     * Remove the inputId for the {@link FunctionPropertyValue} of a Map of properties.
     *
     * @param properties
     * @param inputId
     */
    private void removeInputIdInProperties(final Map<String, AbstractPropertyValue> properties, final String inputId) {
        if (properties == null) {
            return;
        }
        for (Entry<String, AbstractPropertyValue> propertyEntry : properties.entrySet()) {
            if (propertyEntry.getValue() instanceof FunctionPropertyValue) {
                FunctionPropertyValue functionPropertyValue = (FunctionPropertyValue) propertyEntry.getValue();
                if (ToscaFunctionConstants.GET_INPUT.equals(functionPropertyValue.getFunction()) && functionPropertyValue.getTemplateName().equals(inputId)) {
                    propertyEntry.setValue(null);
                }
            }
        }
    }

    /**
     * Remove an input from a topology. This will reset the properties set the get_input(given input id) to null in all node templates and relationship
     * templates.
     *
     * @param topologyId
     * @param inputId
     */
    @ApiOperation(value = "Remove an input from a topology.", notes = "Application role required [ APPLICATION_MANAGER | APPLICATION_DEVOPS ]")
    @RequestMapping(value = "/{topologyId:.+}/inputs/{inputId}", method = RequestMethod.DELETE, produces = MediaType.APPLICATION_JSON_VALUE)
    public RestResponse<TopologyDTO> removeInput(@ApiParam(value = "The topology id.", required = true) @NotBlank @PathVariable final String topologyId,
            @ApiParam(value = "The name of the input.", required = true) @NotBlank @PathVariable final String inputId) {
        Topology topology = topologyServiceCore.getMandatoryTopology(topologyId);
        topologyService.checkEditionAuthorizations(topology);

        Map<String, PropertyDefinition> inputProperties = topology.getInputs();
        if (inputProperties == null || !inputProperties.containsKey(inputId)) {
            throw new NotFoundException("Input " + inputId + "not found in topology");
        }
        inputProperties.remove(inputId);

        Map<String, NodeTemplate> nodeTemplates = topology.getNodeTemplates();
        for (NodeTemplate nodeTemp : nodeTemplates.values()) {
            removeInputIdInProperties(nodeTemp.getProperties(), inputId);
            if (nodeTemp.getRelationships() != null) {
                for (RelationshipTemplate relationshipTemplate : nodeTemp.getRelationships().values()) {
                    removeInputIdInProperties(relationshipTemplate.getProperties(), inputId);
                }
            }
        }

        log.debug("Remove the input " + inputId + " from the topology " + topologyId);
        alienDAO.save(topology);
        return RestResponseBuilder.<TopologyDTO> builder().data(topologyService.buildTopologyDTO(topologyServiceCore.getMandatoryTopology(topologyId))).build();
    }

    /**
     * <p>
     * Associate the property of a node template to an input of the topology.
     * </p>
     * <p>
     * If no input with the given inputId exist in the topology, a new input will be created based on the property definition of the property to associate.
     * </p>
     * <p>
     * If the input already exists in the topology then we validate that the property definition (PD) of the property to associate with the input is compatible
     * with the property definition currently set on the input. If so we also improve the input's PD with the constraints from the associated property.
     * </p>
     *
     * @param topologyId
     * @param inputId
     * @param nodeTemplateName
     * @param propertyId
     * @throws IncompatiblePropertyDefinitionException
     */
    @ApiOperation(value = "Associate the property of a node template to an input of the topology.", notes = "Application role required [ APPLICATION_MANAGER | APPLICATION_DEVOPS ]")
    @RequestMapping(value = "/{topologyId}/nodetemplates/{nodeTemplateName}/property/{propertyId}/input", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
    public RestResponse<Void> setInputToNodeTemplate(@ApiParam(value = "The topology id.", required = true) @NotBlank @PathVariable final String topologyId,
            @ApiParam(value = "The name of the input.", required = true) @NotBlank @RequestParam final String inputId,
            @ApiParam(value = "The node temlate id.", required = true) @NotBlank @PathVariable final String nodeTemplateName,
            @ApiParam(value = "The property id.", required = true) @NotBlank @PathVariable final String propertyId)
            throws IncompatiblePropertyDefinitionException {
        Topology topology = topologyServiceCore.getMandatoryTopology(topologyId);
        topologyService.checkEditionAuthorizations(topology);
        Map<String, PropertyDefinition> inputs = getInputs(topology, false);
        NodeTemplate nodeTemplate = topology.getNodeTemplates().get(nodeTemplateName);
        IndexedNodeType indexedNodeType = csarRepoSearchService.getRequiredElementInDependencies(IndexedNodeType.class, nodeTemplate.getType(),
                topology.getDependencies());

        if (inputs.containsKey(inputId)) {
            PropertyDefinition propertyDefinition = inputs.get(inputId);
            propertyDefinition.checkIfCompatibleOrFail(indexedNodeType.getProperties().get(propertyId));
        } else {
            throw new NotFoundException("Input " + inputId + " is not found");
        }

        FunctionPropertyValue getInput = new FunctionPropertyValue();
        getInput.setFunction(ToscaFunctionConstants.GET_INPUT);
        getInput.setParameters(Arrays.asList(inputId));
        nodeTemplate.getProperties().put(propertyId, getInput);
        topology.setInputs(inputs);

        log.debug("Associate the property <{}> of the node template <{}> to an input of the topology <{}>.", propertyId, nodeTemplateName, topologyId);
        alienDAO.save(topology);
        return RestResponseBuilder.<Void> builder().build();
    }

    /**
     * <p>
     * Disassociated the property of a node template to an input of the topology.
     * </p>
     *
     * @param topologyId
     * @param nodeTemplateName
     * @param propertyId
     */
    @ApiOperation(value = "Disassociated the property of a node template to an input of the topology.", notes = "Application role required [ APPLICATION_MANAGER | APPLICATION_DEVOPS ]")
    @RequestMapping(value = "/{topologyId}/nodetemplates/{nodeTemplateName}/property/{propertyId}/input", method = RequestMethod.DELETE, produces = MediaType.APPLICATION_JSON_VALUE)
    public RestResponse<Void> unsetInputToNodeTemplate(@ApiParam(value = "The topology id.", required = true) @NotBlank @PathVariable final String topologyId,
            @ApiParam(value = "The node temlate id.", required = true) @NotBlank @PathVariable final String nodeTemplateName,
            @ApiParam(value = "The property id.", required = true) @NotBlank @PathVariable final String propertyId) {
        Topology topology = topologyServiceCore.getMandatoryTopology(topologyId);
        topologyService.checkEditionAuthorizations(topology);
        NodeTemplate nodeTemplate = topology.getNodeTemplates().get(nodeTemplateName);

        if (nodeTemplate.getProperties().containsKey(propertyId)) {
            // search the property definition for this property
            IndexedNodeType indexedNodeType = csarRepoSearchService.getRequiredElementInDependencies(IndexedNodeType.class, nodeTemplate.getType(),
                    topology.getDependencies());
            PropertyDefinition pd = indexedNodeType.getProperties().get(propertyId);

            if (pd != null && pd.getDefault() != null) {
                nodeTemplate.getProperties().put(propertyId, new ScalarPropertyValue(pd.getDefault()));
            } else {
                nodeTemplate.getProperties().put(propertyId, null);
            }
            log.debug("Disassociated the property <{}> of the node template <{}> to an input of the topology <{}>.", propertyId, nodeTemplateName, topologyId);
            alienDAO.save(topology);
        }

        return RestResponseBuilder.<Void> builder().build();
    }

    /**
     * Get the possible inputs candidates to be associated with this node template property.
     */
    @ApiOperation(value = "Get the possible inputs candidates to be associated with this property.", notes = "Application role required [ APPLICATION_MANAGER | APPLICATION_DEVOPS ]")
    @RequestMapping(value = "/{topologyId}/nodetemplates/{nodeTemplateName}/property/{propertyId}/inputcandidats", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    public RestResponse<List<String>> getNodetemplatePropertyInputCandidate(
            @ApiParam(value = "The topology id.", required = true) @NotBlank @PathVariable final String topologyId,
            @ApiParam(value = "The node temlate id.", required = true) @NotBlank @PathVariable final String nodeTemplateName,
            @ApiParam(value = "The property id.", required = true) @NotBlank @PathVariable final String propertyId) {
        Topology topology = topologyServiceCore.getMandatoryTopology(topologyId);
        topologyService.checkEditionAuthorizations(topology);
        NodeTemplate nodeTemplate = topology.getNodeTemplates().get(nodeTemplateName);
        // search the property definition for this property
        IndexedNodeType indexedNodeType = csarRepoSearchService.getRequiredElementInDependencies(IndexedNodeType.class, nodeTemplate.getType(),
                topology.getDependencies());
        PropertyDefinition pd = indexedNodeType.getProperties().get(propertyId);
        if (pd == null) {
            // throw 404
            throw new NotFoundException("Property definition for " + propertyId + "not found for this node");
        }
        return getPropertyInputCandidate(pd, topology);
    }

    private RestResponse<List<String>> getPropertyInputCandidate(PropertyDefinition pd, Topology topology) {
        Map<String, PropertyDefinition> inputs = topology.getInputs();
        List<String> inputIds = new ArrayList<String>();
        if (inputs != null && !inputs.isEmpty()) {
            // iterate overs existing inputs and filter them by checking constraint compatibility
            for (Entry<String, PropertyDefinition> inputEntry : inputs.entrySet()) {
                try {
                    inputEntry.getValue().checkIfCompatibleOrFail(pd);
                    inputIds.add(inputEntry.getKey());
                } catch (IncompatiblePropertyDefinitionException e) {
                    // Nothing to do here, the id won't be added to the list
                }
            }
        }
        return RestResponseBuilder.<List<String>> builder().data(inputIds).build();
    }

    /**
     * Get the possible inputs candidates to be associated with this relationship property.
     */
    @ApiOperation(value = "Get the possible inputs candidates to be associated with this relationship property.", notes = "Application role required [ APPLICATION_MANAGER | APPLICATION_DEVOPS ]")
    @RequestMapping(value = "/{topologyId}/nodetemplates/{nodeTemplateName}/relationship/{relationshipId}/property/{propertyId}/inputcandidats", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    public RestResponse<List<String>> getRelationshipPropertyInputCandidate(
            @ApiParam(value = "The topology id.", required = true) @NotBlank @PathVariable final String topologyId,
            @ApiParam(value = "The node temlate id.", required = true) @NotBlank @PathVariable final String nodeTemplateName,
            @ApiParam(value = "The property id.", required = true) @NotBlank @PathVariable final String propertyId,
            @ApiParam(value = "The relationship template id.", required = true) @NotBlank @PathVariable final String relationshipId) {
        Topology topology = topologyServiceCore.getMandatoryTopology(topologyId);
        topologyService.checkEditionAuthorizations(topology);
        NodeTemplate nodeTemplate = topology.getNodeTemplates().get(nodeTemplateName);
        RelationshipTemplate relationshipTemplate = nodeTemplate.getRelationships().get(relationshipId);

        // search the property definition for this property
        IndexedRelationshipType indexedNodeType = csarRepoSearchService.getRequiredElementInDependencies(IndexedRelationshipType.class,
                relationshipTemplate.getType(), topology.getDependencies());
        PropertyDefinition pd = indexedNodeType.getProperties().get(propertyId);
        if (pd == null) {
            // throw 404
            throw new NotFoundException("Property definition for " + propertyId + "not found for this node");
        }
        return getPropertyInputCandidate(pd, topology);
    }

    /**
     * Get the possible inputs candidates to be associated with this capability property.
     */
    @ApiOperation(value = "Get the possible inputs candidates to be associated with this capability property.", notes = "Application role required [ APPLICATION_MANAGER | APPLICATION_DEVOPS ]")
    @RequestMapping(value = "/{topologyId}/nodetemplates/{nodeTemplateName}/capability/{capabilityId}/property/{propertyId}/inputcandidats", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    public RestResponse<List<String>> getCapabilitiesPropertyInputCandidate(
            @ApiParam(value = "The topology id.", required = true) @NotBlank @PathVariable final String topologyId,
            @ApiParam(value = "The node temlate id.", required = true) @NotBlank @PathVariable final String nodeTemplateName,
            @ApiParam(value = "The property id.", required = true) @NotBlank @PathVariable final String propertyId,
            @ApiParam(value = "The capability template id.", required = true) @NotBlank @PathVariable final String capabilityId) {
        Topology topology = topologyServiceCore.getMandatoryTopology(topologyId);
        topologyService.checkEditionAuthorizations(topology);
        NodeTemplate nodeTemplate = topology.getNodeTemplates().get(nodeTemplateName);
        Capability capabilityTemplate = nodeTemplate.getCapabilities().get(capabilityId);

        // search the property definition for this property
        IndexedCapabilityType indexedCapabilityType = csarRepoSearchService.getRequiredElementInDependencies(IndexedCapabilityType.class,
                capabilityTemplate.getType(), topology.getDependencies());
        PropertyDefinition pd = indexedCapabilityType.getProperties().get(propertyId);
        if (pd == null) {
            // throw 404
            throw new NotFoundException("Property definition for " + propertyId + "not found for this node");
        }
        return getPropertyInputCandidate(pd, topology);
    }

    /**
     * Associate the property of a relationship template to an input of the topology.
     * <p>
     * If no input with the given inputId exist in the topology, a new input will be created based on the property definition of the property to associate.
     * </p>
     * <p>
     * If the input already exists in the topology then we validate that the property definition (PD) of the property to associate with the input is compatible
     * with the property definition currently set on the input. If so we also improve the input's PD with the constraints from the associated property.
     * </p>
     *
     * @param topologyId
     * @param inputId
     * @param nodeTemplateName
     * @param relationshipId
     * @param propertyId
     * @throws IncompatiblePropertyDefinitionException
     */
    @ApiOperation(value = "Associate the property of a relationship template to an input of the topology.", notes = "Application role required [ APPLICATION_MANAGER | APPLICATION_DEVOPS ]")
    @RequestMapping(value = "/{topologyId:.+}/nodetemplates/{nodeTemplateName}/relationship/{relationshipId}/property/{propertyId}/input", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
    public RestResponse<Void> setInputToRelationshipTemplate(
            @ApiParam(value = "The topology id.", required = true) @NotBlank @PathVariable final String topologyId,
            @ApiParam(value = "The name of the input.", required = true) @NotBlank @RequestParam final String inputId,
            @ApiParam(value = "The node temlate id.", required = true) @NotBlank @PathVariable final String nodeTemplateName,
            @ApiParam(value = "The property id.", required = true) @NotBlank @PathVariable final String propertyId,
            @ApiParam(value = "The relationship template id.", required = true) @NotBlank @PathVariable final String relationshipId)
            throws IncompatiblePropertyDefinitionException {
        Topology topology = topologyServiceCore.getMandatoryTopology(topologyId);
        topologyService.checkEditionAuthorizations(topology);
        Map<String, PropertyDefinition> inputs = getInputs(topology, false);
        if (topology.getNodeTemplates() == null || !topology.getNodeTemplates().containsKey(nodeTemplateName)) {
            throw new NotFoundException("Node " + nodeTemplateName + " do not exist");
        }
        NodeTemplate nodeTemplate = topology.getNodeTemplates().get(nodeTemplateName);
        if (nodeTemplate.getRelationships() == null || !nodeTemplate.getRelationships().containsKey(relationshipId)) {
            throw new NotFoundException("Relationship " + relationshipId + " do not exist for node " + nodeTemplateName);
        }
        RelationshipTemplate relationshipTemplate = nodeTemplate.getRelationships().get(relationshipId);
        IndexedRelationshipType indexedRelationshipType = csarRepoSearchService.getRequiredElementInDependencies(IndexedRelationshipType.class,
                relationshipTemplate.getType(), topology.getDependencies());
        if (indexedRelationshipType.getProperties() == null || !indexedRelationshipType.getProperties().containsKey(propertyId)) {
            throw new NotFoundException("Property " + propertyId + " do not exist for relationship " + relationshipId + " of node " + nodeTemplateName);
        }
        if (inputs.containsKey(inputId)) {
            PropertyDefinition propertyDefinition = inputs.get(inputId);
            propertyDefinition.checkIfCompatibleOrFail(indexedRelationshipType.getProperties().get(propertyId));
        } else {
            throw new NotFoundException("Input " + inputId + " is not found");
        }

        FunctionPropertyValue getInput = new FunctionPropertyValue();
        getInput.setFunction(ToscaFunctionConstants.GET_INPUT);
        getInput.setParameters(Arrays.asList(inputId));
        relationshipTemplate.getProperties().put(propertyId, getInput);
        topology.setInputs(inputs);

        log.debug("Associate the property <{}> of the relationship template <{}> to an input of the topology <{}>.", propertyId, relationshipId, topologyId);
        alienDAO.save(topology);
        return RestResponseBuilder.<Void> builder().build();
    }

    /**
     * <p>
     * Disassociated the property of a relationship template to an input of the topology.
     * </p>
     *
     * @param topologyId
     * @param nodeTemplateName
     * @param relationshipId
     * @param propertyId
     */
    @ApiOperation(value = "Disassociated the property of a relationship template to an input of the topology.", notes = "Application role required [ APPLICATION_MANAGER | APPLICATION_DEVOPS ]")
    @RequestMapping(value = "/{topologyId:.+}/nodetemplates/{nodeTemplateName}/relationship/{relationshipId}/property/{propertyId}/input", method = RequestMethod.DELETE, produces = MediaType.APPLICATION_JSON_VALUE)
    public RestResponse<Void> unsetInputToRelationshipTemplate(
            @ApiParam(value = "The topology id.", required = true) @NotBlank @PathVariable final String topologyId,
            @ApiParam(value = "The node temlate id.", required = true) @NotBlank @PathVariable final String nodeTemplateName,
            @ApiParam(value = "The property id.", required = true) @NotBlank @PathVariable final String propertyId,
            @ApiParam(value = "The relationship template id.", required = true) @NotBlank @PathVariable final String relationshipId) {
        Topology topology = topologyServiceCore.getMandatoryTopology(topologyId);
        topologyService.checkEditionAuthorizations(topology);
        if (topology.getNodeTemplates() == null || !topology.getNodeTemplates().containsKey(nodeTemplateName)) {
            throw new NotFoundException("Node " + nodeTemplateName + " do not exist");
        }
        NodeTemplate nodeTemplate = topology.getNodeTemplates().get(nodeTemplateName);

        if (nodeTemplate.getRelationships() == null || !nodeTemplate.getRelationships().containsKey(relationshipId)) {
            throw new NotFoundException("Relationship " + relationshipId + " do not exist for node " + nodeTemplateName);
        }
        RelationshipTemplate relationshipTemplate = nodeTemplate.getRelationships().get(relationshipId);

        if (relationshipTemplate.getProperties().containsKey(propertyId)) {
            // search the property definition for this property
            IndexedRelationshipType indexedNodeType = csarRepoSearchService.getRequiredElementInDependencies(IndexedRelationshipType.class,
                    relationshipTemplate.getType(), topology.getDependencies());
            PropertyDefinition pd = indexedNodeType.getProperties().get(propertyId);
            if (pd != null && pd.getDefault() != null) {
                relationshipTemplate.getProperties().put(propertyId, new ScalarPropertyValue(pd.getDefault()));
            } else {
                relationshipTemplate.getProperties().put(propertyId, null);
            }
            log.debug("Disassociated the property <{}> of the relationship template <{}> to an input of the topology <{}>.", propertyId, relationshipId,
                    topologyId);
        } else {
            throw new NotFoundException("Property " + propertyId + " do not exist for relationship " + relationshipId + " of node " + nodeTemplateName);
        }
        alienDAO.save(topology);
        return RestResponseBuilder.<Void> builder().build();
    }

    /**
     * <p>
     * Associate the property of a capability template to an input of the topology.
     * </p>
     *
     * @param topologyId
     * @param inputId
     * @param nodeTemplateName
     * @param capabilityId
     * @param propertyId
     * @throws IncompatiblePropertyDefinitionException
     */
    @ApiOperation(value = "Associate the property of a capability template to an input of the topology.", notes = "Application role required [ APPLICATION_MANAGER | APPLICATION_DEVOPS ]")
    @RequestMapping(value = "/{topologyId:.+}/nodetemplates/{nodeTemplateName}/capability/{capabilityId}/property/{propertyId}/input", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
    public RestResponse<Void> setInputToCapabilityTemplate(
            @ApiParam(value = "The topology id.", required = true) @NotBlank @PathVariable final String topologyId,
            @ApiParam(value = "The name of the input.", required = true) @NotBlank @RequestParam final String inputId,
            @ApiParam(value = "The node temlate id.", required = true) @NotBlank @PathVariable final String nodeTemplateName,
            @ApiParam(value = "The property id.", required = true) @NotBlank @PathVariable final String propertyId,
            @ApiParam(value = "The capability template id.", required = true) @NotBlank @PathVariable final String capabilityId)
            throws IncompatiblePropertyDefinitionException {
        Topology topology = topologyServiceCore.getMandatoryTopology(topologyId);
        topologyService.checkEditionAuthorizations(topology);
        Map<String, PropertyDefinition> inputs = getInputs(topology, false);
        if (topology.getNodeTemplates() == null || !topology.getNodeTemplates().containsKey(nodeTemplateName)) {
            throw new NotFoundException("Node " + nodeTemplateName + " do not exist");
        }

        NodeTemplate nodeTemplate = topology.getNodeTemplates().get(nodeTemplateName);
        if (nodeTemplate.getCapabilities() == null || !nodeTemplate.getCapabilities().containsKey(capabilityId)) {
            throw new NotFoundException("Capability " + capabilityId + " do not exist for node " + nodeTemplateName);
        }

        Capability capabilityTemplate = nodeTemplate.getCapabilities().get(capabilityId);
        IndexedCapabilityType indexedCapabilityType = csarRepoSearchService.getRequiredElementInDependencies(IndexedCapabilityType.class,
                capabilityTemplate.getType(), topology.getDependencies());
        if (indexedCapabilityType.getProperties() == null || !indexedCapabilityType.getProperties().containsKey(propertyId)) {
            throw new NotFoundException("Property " + propertyId + " do not exist for capability " + capabilityId + " of node " + nodeTemplateName);
        }
        if (inputs.containsKey(inputId)) {
            PropertyDefinition propertyDefinition = inputs.get(inputId);
            propertyDefinition.checkIfCompatibleOrFail(indexedCapabilityType.getProperties().get(propertyId));
        } else {
            throw new NotFoundException("Input " + inputId + " is not found");
        }

        FunctionPropertyValue getInput = new FunctionPropertyValue();
        getInput.setFunction(ToscaFunctionConstants.GET_INPUT);
        getInput.setParameters(Arrays.asList(inputId));
        capabilityTemplate.getProperties().put(propertyId, getInput);
        topology.setInputs(inputs);

        log.debug("Associate the property <{}> of the capability template <{}> to an input of the topology <{}>.", propertyId, capabilityId, topologyId);
        alienDAO.save(topology);
        return RestResponseBuilder.<Void> builder().build();
    }

    /**
     * <p>
     * Disassociated the property of a capability template to an input of the topology.
     * </p>
     *
     * @param topologyId
     * @param nodeTemplateName
     * @param capabilityId
     * @param propertyId
     */
    @ApiOperation(value = "Associate the property of a capability template to an input of the topology.", notes = "Application role required [ APPLICATION_MANAGER | APPLICATION_DEVOPS ]")
    @RequestMapping(value = "/{topologyId:.+}/nodetemplates/{nodeTemplateName}/capability/{capabilityId}/property/{propertyId}/input", method = RequestMethod.DELETE, produces = MediaType.APPLICATION_JSON_VALUE)
    public RestResponse<Void> unsetInputToCapabilityTemplate(
            @ApiParam(value = "The topology id.", required = true) @NotBlank @PathVariable final String topologyId,
            @ApiParam(value = "The node temlate id.", required = true) @NotBlank @PathVariable final String nodeTemplateName,
            @ApiParam(value = "The property id.", required = true) @NotBlank @PathVariable final String propertyId,
            @ApiParam(value = "The capability template id.", required = true) @NotBlank @PathVariable final String capabilityId) {
        Topology topology = topologyServiceCore.getMandatoryTopology(topologyId);
        topologyService.checkEditionAuthorizations(topology);
        if (topology.getNodeTemplates() == null || !topology.getNodeTemplates().containsKey(nodeTemplateName)) {
            throw new NotFoundException("Node " + nodeTemplateName + " do not exist");
        }

        NodeTemplate nodeTemplate = topology.getNodeTemplates().get(nodeTemplateName);
        if (nodeTemplate.getCapabilities() == null || !nodeTemplate.getCapabilities().containsKey(capabilityId)) {
            throw new NotFoundException("Capability " + capabilityId + " do not exist for node " + nodeTemplateName);
        }

        Capability capabilityTemplate = nodeTemplate.getCapabilities().get(capabilityId);
        if (capabilityTemplate.getProperties().containsKey(propertyId)) {
            // search the property definition for this property
            IndexedCapabilityType indexedCapabilityType = csarRepoSearchService.getRequiredElementInDependencies(IndexedCapabilityType.class,
                    capabilityTemplate.getType(), topology.getDependencies());
            PropertyDefinition pd = indexedCapabilityType.getProperties().get(propertyId);
            if (pd != null && pd.getDefault() != null) {
                capabilityTemplate.getProperties().put(propertyId, new ScalarPropertyValue(pd.getDefault()));
            } else {
                capabilityTemplate.getProperties().put(propertyId, null);
            }
            log.debug("Disassociated the property <{}> of the capability template <{}> to an input of the topology <{}>.", propertyId, capabilityId, topologyId);
        } else {
            throw new NotFoundException("Property " + propertyId + " do not exist for capability " + capabilityId + " of node " + nodeTemplateName);
        }
        alienDAO.save(topology);
        return RestResponseBuilder.<Void> builder().build();
    }

    private Map<String, PropertyDefinition> getInputs(Topology topology, boolean create) {
        Map<String, PropertyDefinition> inputs = topology.getInputs();
        if (inputs == null) {
            if (create) {
                inputs = Maps.newHashMap();
            } else {
                throw new NotFoundException("The topology has no defined input");
            }
        }
        return inputs;
    }
}
