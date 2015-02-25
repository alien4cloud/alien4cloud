package alien4cloud.rest.topology;

import java.util.Arrays;
import java.util.Map;

import javax.annotation.Resource;

import lombok.extern.slf4j.Slf4j;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import alien4cloud.dao.IGenericSearchDAO;
import alien4cloud.model.components.AbstractPropertyValue;
import alien4cloud.model.components.FunctionPropertyValue;
import alien4cloud.model.components.IndexedNodeType;
import alien4cloud.model.components.PropertyConstraint;
import alien4cloud.model.components.PropertyDefinition;
import alien4cloud.model.topology.NodeTemplate;
import alien4cloud.model.topology.RelationshipTemplate;
import alien4cloud.model.topology.Topology;
import alien4cloud.topology.TopologyServiceCore;
import alien4cloud.tosca.normative.ToscaFunctionConstants;
import alien4cloud.tosca.properties.constraints.exception.ConstraintViolationException;

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

    // /**
    // * Add a new input.
    // *
    // * @param topologyId The id of the topology to retrieve.
    // * @return
    // */
    // @ApiOperation(value = "Add a new input", notes = "Add a new input. Application role required [ APPLICATION_MANAGER | APPLICATION_DEVOPS ]")
    // @RequestMapping(value = "/{topologyId}", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    // public void addInput(@PathVariable String topologyId, @PathVariable String type, @PathVariable String value) {
    // Topology topology = topologyServiceCore.getMandatoryTopology(topologyId);
    // topologyService.checkEditionAuthorizations(topology);
    // Map<String, PropertyDefinition> inputProperties = topology.getInputs();
    //
    // }

    /**
     * Update the inputId for the {@link FunctionPropertyValue} of a Map of properties.
     * 
     * @param properties
     * @param oldInputId
     * @param newInputId
     */
    private void updateInputIdInProperties(Map<String, AbstractPropertyValue> properties, final String oldInputId, final String newInputId) {
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
    public void updateInputId(String topologyId, String oldInputId, String newInputId) {
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
    }

    /**
     * Remove the inputId for the {@link FunctionPropertyValue} of a Map of properties.
     * 
     * @param properties
     * @param inputId
     */
    private void removeInputIdInProperties(Map<String, AbstractPropertyValue> properties, String inputId) {
        for (AbstractPropertyValue propertyValue : properties.values()) {
            if (propertyValue instanceof FunctionPropertyValue) {
                FunctionPropertyValue functionPropertyValue = (FunctionPropertyValue) propertyValue;
                if (ToscaFunctionConstants.GET_INPUT.equals(functionPropertyValue.getFunction()) && functionPropertyValue.getParameters().get(0) == inputId) {
                    functionPropertyValue.setParameters(Arrays.asList((String) null));
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
    public void removeInput(String topologyId, String inputId) {
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
    public void setInputToNodeTemplate(String topologyId, String inputId, String nodeTemplateId, String propertyId) throws ConstraintViolationException {
        Topology topology = topologyServiceCore.getMandatoryTopology(topologyId);
        topologyService.checkEditionAuthorizations(topology);
        Map<String, PropertyDefinition> inputProperties = topology.getInputs();
        NodeTemplate nodeTemplate = topology.getNodeTemplates().get(nodeTemplateId);
        IndexedNodeType indexedNodeType = alienDAO.findById(IndexedNodeType.class, nodeTemplate.getType());

        if (inputProperties.containsKey(inputId)) {
            AbstractPropertyValue abstractPropertyValue = nodeTemplate.getProperties().get(propertyId);
            PropertyDefinition propertyDefinition = inputProperties.get(inputId);
            for (PropertyConstraint propertyConstraint : propertyDefinition.getConstraints()) {
                propertyConstraint.validate(abstractPropertyValue);
            }
            propertyDefinition.mergeConstraintsIfValid(indexedNodeType.getProperties().get(propertyId));
        } else {
            inputProperties.put(inputId, indexedNodeType.getProperties().get(propertyId));
        }
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
    public void setInputToRelationshipTemplate(String topologyId, String inputId, String nodeTemplateId, String relationshipTemplateId, String propertyId)
            throws ConstraintViolationException {
        Topology topology = topologyServiceCore.getMandatoryTopology(topologyId);
        topologyService.checkEditionAuthorizations(topology);
        Map<String, PropertyDefinition> inputProperties = topology.getInputs();
        NodeTemplate nodeTemplate = topology.getNodeTemplates().get(nodeTemplateId);
        IndexedNodeType indexedNodeType = alienDAO.findById(IndexedNodeType.class, nodeTemplate.getType());

        if (inputProperties.containsKey(inputId)) {
            RelationshipTemplate relationshipTemplate = nodeTemplate.getRelationships().get(relationshipTemplateId);
            AbstractPropertyValue abstractPropertyValue = relationshipTemplate.getProperties().get(propertyId);
            PropertyDefinition propertyDefinition = inputProperties.get(inputId);
            for (PropertyConstraint propertyConstraint : propertyDefinition.getConstraints()) {
                propertyConstraint.validate(abstractPropertyValue);
            }
            propertyDefinition.mergeConstraintsIfValid(indexedNodeType.getProperties().get(propertyId));
        } else {
            inputProperties.put(inputId, indexedNodeType.getProperties().get(propertyId));
        }
    }
}
