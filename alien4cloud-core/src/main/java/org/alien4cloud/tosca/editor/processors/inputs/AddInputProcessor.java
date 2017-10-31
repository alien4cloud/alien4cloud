package org.alien4cloud.tosca.editor.processors.inputs;

import java.util.Map;

import org.alien4cloud.tosca.editor.EditionContextManager;
import org.alien4cloud.tosca.editor.operations.inputs.AddInputOperation;
import org.alien4cloud.tosca.model.Csar;
import org.alien4cloud.tosca.model.definitions.PropertyDefinition;
import org.alien4cloud.tosca.model.templates.Topology;
import org.springframework.stereotype.Component;

import alien4cloud.exception.AlreadyExistException;
import alien4cloud.exception.InvalidNameException;
import lombok.extern.slf4j.Slf4j;

/**
 * Process an add input operation.
 */
@Slf4j
@Component
public class AddInputProcessor extends AbstractInputProcessor<AddInputOperation> {
    @Override
    protected void processInputOperation(Csar csar, Topology topology, AddInputOperation operation, Map<String, PropertyDefinition> inputs) {
        if (operation.getInputName() == null || operation.getInputName().isEmpty() || !operation.getInputName().matches("\\w+")) {
            throw new InvalidNameException("newInputName", operation.getInputName(), "\\w+");
        }

        if (inputs.containsKey(operation.getInputName())) {
            throw new AlreadyExistException("An input with the id " + operation.getInputName() + "already exist in the topology " + topology.getId());
        }

        inputs.put(operation.getInputName(), operation.getPropertyDefinition());
        topology.setInputs(inputs);

        log.debug("Add a new input [ {} ] for the topology [ {} ].", operation.getInputName(), topology.getId());
    }

    @Override
    protected boolean create() {
        return true; // create the inputs map if null in the topology.
    }
}