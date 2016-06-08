package org.alien4cloud.tosca.editor.processors;

import org.alien4cloud.tosca.editor.commands.IEditorOperation;

/**
 * Every IEditorOperation has a related IEditorOperationProcessor implementation that will process it and update the topology model accordingly as well as
 * managing the undo processor creation.
 */
public interface IEditorOperationProcessor<T extends IEditorOperation> {
    /**
     * Process the backed operation to update the topology.
     */
    void process(T operation);
}