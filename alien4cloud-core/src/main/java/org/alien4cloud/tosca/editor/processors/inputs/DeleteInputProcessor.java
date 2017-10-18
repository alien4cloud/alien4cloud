package org.alien4cloud.tosca.editor.processors.inputs;

import java.util.Map;

import org.alien4cloud.tosca.editor.EditionContextManager;
import org.alien4cloud.tosca.editor.operations.inputs.DeleteInputOperation;
import org.alien4cloud.tosca.model.Csar;
import org.alien4cloud.tosca.model.definitions.AbstractPropertyValue;
import org.alien4cloud.tosca.model.definitions.FunctionPropertyValue;
import org.alien4cloud.tosca.model.definitions.PropertyDefinition;
import org.alien4cloud.tosca.model.templates.Capability;
import org.alien4cloud.tosca.model.templates.NodeTemplate;
import org.alien4cloud.tosca.model.templates.RelationshipTemplate;
import org.alien4cloud.tosca.model.templates.Topology;
import org.alien4cloud.tosca.model.types.CapabilityType;
import org.alien4cloud.tosca.model.types.NodeType;
import org.alien4cloud.tosca.model.types.RelationshipType;
import org.alien4cloud.tosca.normative.constants.ToscaFunctionConstants;
import org.springframework.stereotype.Component;

import alien4cloud.exception.NotFoundException;
import alien4cloud.tosca.context.ToscaContext;
import alien4cloud.utils.PropertyUtil;
import lombok.extern.slf4j.Slf4j;

/**
 * Removes an input from the topology and reset the properties values if the input was assigned.
 */
@Slf4j
@Component
public class DeleteInputProcessor extends AbstractInputProcessor<DeleteInputOperation> {
    @Override
    protected void processInputOperation(Csar csar, Topology topology, DeleteInputOperation operation, Map<String, PropertyDefinition> inputs) {
        if (!inputs.containsKey(operation.getInputName())) {
            throw new NotFoundException("Input " + operation.getInputName() + "not found in topology");
        }
        inputs.remove(operation.getInputName());

        Map<String, NodeTemplate> nodeTemplates = topology.getNodeTemplates();
        if (nodeTemplates == null) {
            return;
        }

        for (NodeTemplate nodeTemplate : nodeTemplates.values()) {
            NodeType nodeType = ToscaContext.get(NodeType.class, nodeTemplate.getType());
            removeInputIdInProperties(nodeTemplate.getProperties(), nodeType.getProperties(), operation.getInputName());
            if (nodeTemplate.getRelationships() != null) {
                for (RelationshipTemplate relationshipTemplate : nodeTemplate.getRelationships().values()) {
                    RelationshipType relationshipType = ToscaContext.get(RelationshipType.class, relationshipTemplate.getType());
                    removeInputIdInProperties(relationshipTemplate.getProperties(), relationshipType.getProperties(), operation.getInputName());
                }
            }
            if (nodeTemplate.getCapabilities() != null) {
                for (Capability capability : nodeTemplate.getCapabilities().values()) {
                    CapabilityType capabilityType = ToscaContext.get(CapabilityType.class, capability.getType());
                    removeInputIdInProperties(capability.getProperties(), capabilityType.getProperties(), operation.getInputName());
                }
            }
        }

        log.debug("Remove the input " + operation.getInputName() + " from the topology " + topology.getId());
    }

    /**
     * Remove the inputId for the {@link FunctionPropertyValue} of a Map of properties.
     *
     * @param properties the list of properties values currently defined in the topology.
     * @param propertyDefinitions the list of definitions of properties (matching the list of values).
     * @param inputId The id of the input to remove.
     */
    private void removeInputIdInProperties(final Map<String, AbstractPropertyValue> properties, final Map<String, PropertyDefinition> propertyDefinitions,
            final String inputId) {
        if (properties == null) {
            return;
        }
        for (Map.Entry<String, AbstractPropertyValue> propertyEntry : properties.entrySet()) {
            if (propertyEntry.getValue() instanceof FunctionPropertyValue) {
                FunctionPropertyValue functionPropertyValue = (FunctionPropertyValue) propertyEntry.getValue();
                if (ToscaFunctionConstants.GET_INPUT.equals(functionPropertyValue.getFunction()) && functionPropertyValue.getTemplateName().equals(inputId)) {
                    PropertyDefinition pd = propertyDefinitions.get(propertyEntry.getKey());
                    AbstractPropertyValue pv = PropertyUtil.getDefaultPropertyValueFromPropertyDefinition(pd);
                    propertyEntry.setValue(pv);
                }
            }
        }
    }

    @Override
    protected boolean create() {
        return false;
    }
}
