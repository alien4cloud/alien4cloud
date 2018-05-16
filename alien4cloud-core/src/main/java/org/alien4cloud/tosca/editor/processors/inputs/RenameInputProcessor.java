package org.alien4cloud.tosca.editor.processors.inputs;

import static alien4cloud.utils.AlienUtils.safe;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Map;

import javax.inject.Inject;

import org.alien4cloud.tosca.editor.EditorFileService;
import org.alien4cloud.tosca.editor.operations.UpdateFileOperation;
import org.alien4cloud.tosca.editor.operations.inputs.RenameInputOperation;
import org.alien4cloud.tosca.editor.processors.UpdateFileProcessor;
import org.alien4cloud.tosca.model.Csar;
import org.alien4cloud.tosca.model.definitions.AbstractPropertyValue;
import org.alien4cloud.tosca.model.definitions.FunctionPropertyValue;
import org.alien4cloud.tosca.model.definitions.PropertyDefinition;
import org.alien4cloud.tosca.model.templates.Capability;
import org.alien4cloud.tosca.model.templates.NodeTemplate;
import org.alien4cloud.tosca.model.templates.RelationshipTemplate;
import org.alien4cloud.tosca.model.templates.Topology;
import org.alien4cloud.tosca.normative.constants.ToscaFunctionConstants;
import org.alien4cloud.tosca.variable.service.QuickFileStorageService;
import org.apache.commons.collections4.MapUtils;
import org.springframework.stereotype.Component;

import alien4cloud.exception.AlreadyExistException;
import alien4cloud.exception.InvalidNameException;
import alien4cloud.exception.NotFoundException;
import alien4cloud.utils.YamlParserUtil;
import lombok.extern.slf4j.Slf4j;

/**
 * Removes an input from the topology and reset the properties values if the input was assigned.
 */
@Slf4j
@Component
public class RenameInputProcessor extends AbstractInputProcessor<RenameInputOperation> {

    @Inject
    private QuickFileStorageService quickFileStorageService;
    @Inject
    private EditorFileService editorFileService;
    @Inject
    private UpdateFileProcessor updateFileProcessor;

    @Override
    protected void processInputOperation(Csar csar, Topology topology, RenameInputOperation operation, Map<String, PropertyDefinition> inputs) {
        if (!inputs.containsKey(operation.getInputName())) {
            throw new NotFoundException("Input " + operation.getInputName() + " not found");
        }
        if (operation.getInputName().equals(operation.getNewInputName())) {
            return; // nothing has changed.
        }
        if (operation.getNewInputName() == null || operation.getNewInputName().isEmpty() || !operation.getNewInputName().matches("\\w+")) {
            throw new InvalidNameException("newInputName", operation.getNewInputName(), "\\w+");
        }
        if (inputs.containsKey(operation.getNewInputName())) {
            throw new AlreadyExistException("Input " + operation.getNewInputName() + " already existed");
        }

        PropertyDefinition propertyDefinition = inputs.remove(operation.getInputName());
        inputs.put(operation.getNewInputName(), propertyDefinition);

        Map<String, NodeTemplate> nodeTemplates = topology.getNodeTemplates();
        for (NodeTemplate nodeTemp : safe(nodeTemplates).values()) {
            renameInputInProperties(nodeTemp.getProperties(), operation.getInputName(), operation.getNewInputName());
            if (nodeTemp.getRelationships() != null) {
                for (RelationshipTemplate relationshipTemplate : nodeTemp.getRelationships().values()) {
                    renameInputInProperties(relationshipTemplate.getProperties(), operation.getInputName(), operation.getNewInputName());
                }
            }
            if (nodeTemp.getCapabilities() != null) {
                for (Capability capability : nodeTemp.getCapabilities().values()) {
                    renameInputInProperties(capability.getProperties(), operation.getInputName(), operation.getNewInputName());
                }
            }
        }

        // rename preconfigured input
        renamePreconfiguredInput(csar, topology, operation);

        log.debug("Change the name of an input parameter [ {} ] to [ {} ] for the topology ", operation.getInputName(), operation.getNewInputName(),
                topology.getId());
    }

    /**
     * Update the input name for the {@link FunctionPropertyValue} in a map of properties.
     *
     * @param properties The map of properties in which to rename input
     * @param inputName The name of the input to rename.
     * @param newInputName The new name for the input.
     */
    private void renameInputInProperties(final Map<String, AbstractPropertyValue> properties, final String inputName, final String newInputName) {
        if (MapUtils.isNotEmpty(properties)) {
            for (AbstractPropertyValue propertyValue : properties.values()) {
                if (propertyValue instanceof FunctionPropertyValue) {
                    FunctionPropertyValue functionPropertyValue = (FunctionPropertyValue) propertyValue;
                    if (ToscaFunctionConstants.GET_INPUT.equals(functionPropertyValue.getFunction())
                            && functionPropertyValue.getParameters().get(0).equals(inputName)) {
                        functionPropertyValue.setParameters(Arrays.asList(newInputName));
                    }
                }
            }
        }
    }

    /**
     * Rename the preconfigured input entry in the inputs file
     *
     * @param csar
     * @param topology
     * @param operation
     */
    private void renamePreconfiguredInput(Csar csar, Topology topology, RenameInputOperation operation) {
        Map<String, Object> variables = editorFileService.loadInputsVariables(csar.getId());
        if (!variables.containsKey(operation.getInputName())) {
            return;
        }
        Object value = variables.remove(operation.getInputName());
        variables.put(operation.getNewInputName(), value);
        UpdateFileOperation updateFileOperation = new UpdateFileOperation(quickFileStorageService.getRelativeInputsFilePath(),
                new ByteArrayInputStream(YamlParserUtil.dumpAsMap(variables).getBytes(StandardCharsets.UTF_8)));

        updateFileProcessor.process(csar, topology, updateFileOperation);
    }

    @Override
    protected boolean create() {
        return false;
    }
}