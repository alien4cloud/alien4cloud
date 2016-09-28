package org.alien4cloud.tosca.editor.processors.inputs;

import java.util.Map;

import org.alien4cloud.tosca.editor.EditionContextManager;
import org.alien4cloud.tosca.editor.operations.inputs.AbstractInputOperation;
import org.alien4cloud.tosca.editor.processors.IEditorOperationProcessor;

import com.google.common.collect.Maps;

import alien4cloud.exception.NotFoundException;
import org.alien4cloud.tosca.model.definitions.PropertyDefinition;
import org.alien4cloud.tosca.model.templates.Topology;

/**
 * Abstract class to process input operations. Saves common input checking.
 */
public abstract class AbstractInputProcessor<T extends AbstractInputOperation> implements IEditorOperationProcessor<T> {
    @Override
    public void process(T operation) {
        Topology topology = EditionContextManager.getTopology();
        Map<String, PropertyDefinition> inputs = topology.getInputs();
        if (inputs == null) {
            if (create()) {
                inputs = Maps.newHashMap();
            } else {
                throw new NotFoundException("The topology has no defined input");
            }
        }
        processInputOperation(operation, inputs);
    }

    /**
     * If true then the inputs map will be created rather than throwing an exception.
     * 
     * @return true if we should create the input map if none exists.
     */
    protected abstract boolean create();

    protected abstract void processInputOperation(T operation, Map<String, PropertyDefinition> inputs);
}
