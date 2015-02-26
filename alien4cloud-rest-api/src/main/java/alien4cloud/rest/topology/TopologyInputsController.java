package alien4cloud.rest.topology;

import java.util.Arrays;
import java.util.Map;
import java.util.Map.Entry;

import javax.annotation.Resource;

import lombok.extern.slf4j.Slf4j;

import org.hibernate.validator.constraints.NotBlank;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import alien4cloud.dao.IGenericSearchDAO;
import alien4cloud.model.components.AbstractPropertyValue;
import alien4cloud.model.components.FunctionPropertyValue;
import alien4cloud.model.components.IndexedNodeType;
import alien4cloud.model.components.PropertyDefinition;
import alien4cloud.model.topology.NodeTemplate;
import alien4cloud.model.topology.RelationshipTemplate;
import alien4cloud.model.topology.Topology;
import alien4cloud.topology.TopologyServiceCore;
import alien4cloud.tosca.normative.ToscaFunctionConstants;
import alien4cloud.tosca.properties.constraints.exception.ConstraintViolationException;

import com.wordnik.swagger.annotations.ApiOperation;
import com.wordnik.swagger.annotations.ApiParam;

@Slf4j
@RestController
@RequestMapping("/rest/topologies/inputs")
public class TopologyInputsController {

    @Resource(name = "alien-es-dao")
    private IGenericSearchDAO alienDAO;

    @Resource
    private TopologyService topologyService;

    @Resource
    private TopologyServiceCore topologyServiceCore;

    /**
     * Add a new input.
     *
     * @param topologyId The id of the topology to retrieve.
     * @return
     */
    @ApiOperation(value = "Add a new input", notes = "Add a new input. Application role required [ APPLICATION_MANAGER | APPLICATION_DEVOPS ]")
    @RequestMapping(value = "/{topologyId}/add/{newInputId}", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    public void addInput(@ApiParam(value = "The topology id.", required = true) @NotBlank @PathVariable final String topologyId,
            @ApiParam(value = "The name of new input.", required = true) @NotBlank @PathVariable final String newInputId) {
        Topology topology = topologyServiceCore.getMandatoryTopology(topologyId);
        topologyService.checkEditionAuthorizations(topology);
        Map<String, PropertyDefinition> inputProperties = topology.getInputs();
        inputProperties.put(newInputId, new PropertyDefinition());

        log.debug("Add a new input <{}> for the topology <{}>.", newInputId, topologyId);
        alienDAO.save(topology);
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
                if (ToscaFunctionConstants.GET_INPUT.equals(functionPropertyValue.getFunction()) && functionPropertyValue.getParameters().get(0) == oldInputId) {
                    functionPropertyValue.setParameters(Arrays.asList(newInputId));
                }
            }
        }
    }

    /**
     * Change the name of an input parameter. This will update all the get_input function used in node templates and relationship template to set the new value.
     * 
     * @param oldInputId
     * @param newInputId
     */
    @ApiOperation(value = "Change the name of an input parameter.", notes = "Application role required [ APPLICATION_MANAGER | APPLICATION_DEVOPS ]")
    @RequestMapping(value = "/{topologyqId:.+}/updateInputId/{oldInputId}/{newInputId}", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
    public void updateInputId(@ApiParam(value = "The topology id.", required = true) final String topologyId,
            @ApiParam(value = "The name of the old input.", required = true) @NotBlank @PathVariable final String oldInputId,
            @ApiParam(value = "The name of the new input.", required = true) @NotBlank @PathVariable final String newInputId) {
        Topology topology = topologyServiceCore.getMandatoryTopology(topologyId);
        topologyService.checkEditionAuthorizations(topology);
        Map<String, PropertyDefinition> inputProperties = topology.getInputs();
        PropertyDefinition propertyDefinition = inputProperties.remove(oldInputId);
        inputProperties.put(newInputId, propertyDefinition);

        Map<String, NodeTemplate> nodeTemplates = topology.getNodeTemplates();
        for (NodeTemplate nodeTemp : nodeTemplates.values()) {
            updateInputIdInProperties(nodeTemp.getProperties(), oldInputId, newInputId);
            for (RelationshipTemplate relationshipTemplate : nodeTemp.getRelationships().values()) {
                updateInputIdInProperties(relationshipTemplate.getProperties(), oldInputId, newInputId);
            }
        }

        log.debug("Change the name of an input parameter <{}> to <{}> for the topology ", oldInputId, newInputId, topologyId);
        alienDAO.save(topology);
    }

    /**
     * Remove the inputId for the {@link FunctionPropertyValue} of a Map of properties.
     * 
     * @param properties
     * @param inputId
     */
    private void removeInputIdInProperties(final Map<String, AbstractPropertyValue> properties, final String inputId) {
        for (Entry<String, AbstractPropertyValue> propertyEntry : properties.entrySet()) {
            if (propertyEntry.getValue() instanceof FunctionPropertyValue) {
                FunctionPropertyValue functionPropertyValue = (FunctionPropertyValue) propertyEntry.getValue();
                if (ToscaFunctionConstants.GET_INPUT.equals(functionPropertyValue.getFunction()) && functionPropertyValue.getParameters().get(0) == inputId) {
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
    @RequestMapping(value = "/{topologyqId:.+}/remove/{inputId}", method = RequestMethod.DELETE, produces = MediaType.APPLICATION_JSON_VALUE)
    public void removeInput(@ApiParam(value = "The topology id.", required = true) @NotBlank @PathVariable final String topologyId,
            @ApiParam(value = "The name of the input.", required = true) @NotBlank @PathVariable final String inputId) {
        Topology topology = topologyServiceCore.getMandatoryTopology(topologyId);
        topologyService.checkEditionAuthorizations(topology);
        Map<String, PropertyDefinition> inputProperties = topology.getInputs();
        inputProperties.remove(inputId);

        Map<String, NodeTemplate> nodeTemplates = topology.getNodeTemplates();
        for (NodeTemplate nodeTemp : nodeTemplates.values()) {
            removeInputIdInProperties(nodeTemp.getProperties(), inputId);
            for (RelationshipTemplate relationshipTemplate : nodeTemp.getRelationships().values()) {
                removeInputIdInProperties(relationshipTemplate.getProperties(), inputId);
            }
        }

        log.debug("Remove the input " + inputId + " from the topology " + topologyId);
        alienDAO.save(topology);
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
     * @param nodeTemplateId
     * @param propertyId
     * @throws ConstraintViolationException
     */
    @ApiOperation(value = "Associate the property of a node template to an input of the topology.", notes = "Application role required [ APPLICATION_MANAGER | APPLICATION_DEVOPS ]")
    @RequestMapping(value = "/{topologyId:.+}/setinput/{inputId}/nodetemplate/{nodeTemplateId}/property/{propertyId}", method = RequestMethod.PUT, produces = MediaType.APPLICATION_JSON_VALUE)
    public void setInputToNodeTemplate(@ApiParam(value = "The topology id.", required = true) final String topologyId,
            @ApiParam(value = "The name of the input.", required = true) @NotBlank @PathVariable final String inputId,
            @ApiParam(value = "The node temlate id.", required = true) @NotBlank @PathVariable final String nodeTemplateId,
            @ApiParam(value = "The property id.", required = true) @NotBlank @PathVariable final String propertyId)
            throws ConstraintViolationException {
        Topology topology = topologyServiceCore.getMandatoryTopology(topologyId);
        topologyService.checkEditionAuthorizations(topology);
        Map<String, PropertyDefinition> inputProperties = topology.getInputs();
        NodeTemplate nodeTemplate = topology.getNodeTemplates().get(nodeTemplateId);
        IndexedNodeType indexedNodeType = alienDAO.findById(IndexedNodeType.class, nodeTemplate.getType());

        if (inputProperties.containsKey(inputId)) {
            PropertyDefinition propertyDefinition = inputProperties.get(inputId);
            propertyDefinition.mergeConstraintsIfValid(indexedNodeType.getProperties().get(propertyId));
        } else {
            inputProperties.put(inputId, indexedNodeType.getProperties().get(propertyId));
        }
        FunctionPropertyValue getInput = new FunctionPropertyValue();
        getInput.setFunction(ToscaFunctionConstants.GET_INPUT);
        getInput.setParameters(Arrays.asList(inputId));
        nodeTemplate.getProperties().put(propertyId, getInput);

        log.debug("Associate the property <{}> of the node template <{}> to an input of the topology <{}>.", propertyId, nodeTemplateId, topologyId);
        alienDAO.save(topology);
    }

    /**
     * <p>
     * Associate the property of a relationship template to an input of the topology.
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
     * @param nodeTemplateId
     * @param relationshipTemplateId
     * @param propertyId
     * @throws ConstraintViolationException
     */
    @ApiOperation(value = "Associate the property of a relationship template to an input of the topology.", notes = "Application role required [ APPLICATION_MANAGER | APPLICATION_DEVOPS ]")
    @RequestMapping(value = "/{topologyId:.+}/setinput/{inputId}/nodetemplate/{nodeTemplateId}/relationship/{relationshipTemplateId}/property/{propertyId}", method = RequestMethod.PUT, produces = MediaType.APPLICATION_JSON_VALUE)
    public void setInputToRelationshipTemplate(@ApiParam(value = "The topology id.", required = true) final String topologyId,
            @ApiParam(value = "The name of the input.", required = true) @NotBlank @PathVariable final String inputId,
            @ApiParam(value = "The node temlate id.", required = true) @NotBlank @PathVariable final String nodeTemplateId,
            @ApiParam(value = "The property id.", required = true) @NotBlank @PathVariable final String propertyId,
            @ApiParam(value = "The relationship template id.", required = true) @NotBlank @PathVariable final String relationshipTemplateId)
            throws ConstraintViolationException {
        Topology topology = topologyServiceCore.getMandatoryTopology(topologyId);
        topologyService.checkEditionAuthorizations(topology);
        Map<String, PropertyDefinition> inputProperties = topology.getInputs();
        NodeTemplate nodeTemplate = topology.getNodeTemplates().get(nodeTemplateId);
        IndexedNodeType indexedNodeType = alienDAO.findById(IndexedNodeType.class, nodeTemplate.getType());
        RelationshipTemplate relationshipTemplate = nodeTemplate.getRelationships().get(relationshipTemplateId);

        if (inputProperties.containsKey(inputId)) {
            PropertyDefinition propertyDefinition = inputProperties.get(inputId);
            propertyDefinition.mergeConstraintsIfValid(indexedNodeType.getProperties().get(propertyId));
        } else {
            inputProperties.put(inputId, indexedNodeType.getProperties().get(propertyId));
        }
        FunctionPropertyValue getInput = new FunctionPropertyValue();
        getInput.setFunction(ToscaFunctionConstants.GET_INPUT);
        getInput.setParameters(Arrays.asList(inputId));
        relationshipTemplate.getProperties().put(propertyId, getInput);

        log.debug("Associate the property <{}> of the relationship template <{}> to an input of the topology <{}>.", propertyId, relationshipTemplateId,
                topologyId);
        alienDAO.save(topology);
    }
}
