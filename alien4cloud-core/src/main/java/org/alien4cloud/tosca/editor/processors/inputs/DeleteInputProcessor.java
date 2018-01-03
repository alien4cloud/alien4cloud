package org.alien4cloud.tosca.editor.processors.inputs;

import static alien4cloud.utils.AlienUtils.safe;

import java.util.Map;

import javax.inject.Inject;

import org.alien4cloud.tosca.editor.operations.inputs.DeleteInputOperation;
import org.alien4cloud.tosca.editor.operations.inputs.UpdateInputExpressionOperation;
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

    @Inject
    private UpdateInputExpressionProcessor updateInputExpressionProcessor;

    @Override
    protected void processInputOperation(Csar csar, Topology topology, DeleteInputOperation operation, Map<String, PropertyDefinition> inputs) {
        if (!inputs.containsKey(operation.getInputName())) {
            throw new NotFoundException("Input " + operation.getInputName() + "not found in topology");
        }

        for (NodeTemplate nodeTemplate : safe(topology.getNodeTemplates()).values()) {
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

        deletePreConfiguredInput(csar, topology, operation);

        inputs.remove(operation.getInputName());
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

    /**
     * Remove if existed, the preconfigured input from the inputs file
     *
     * @param csar
     * @param topology
     * @param deleteInputOperation
     */
    private void deletePreConfiguredInput(Csar csar, Topology topology, DeleteInputOperation deleteInputOperation) {
        UpdateInputExpressionOperation updateInputExpressionOperation = new UpdateInputExpressionOperation();
        // only set the name, leaving the expression to null, as a null expression is considered as a removal of the pre-conf input entry
        updateInputExpressionOperation.setName(deleteInputOperation.getInputName());
        updateInputExpressionProcessor.process(csar, topology, updateInputExpressionOperation);
    }

    @Override
    protected boolean create() {
        return false;
    }
}
