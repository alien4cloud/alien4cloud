package org.alien4cloud.tosca.editor;

import org.alien4cloud.tosca.editor.commands.IEditorOperation;

import alien4cloud.model.topology.Topology;

/**
 * Every IEditorOperation has a related IEditorOperationProcessor implementation that will process it and update the topology model accordingly as well as
 * managing the undo processor creation.
 */
public interface IEditorOperationProcessor<T extends IEditorOperation> {
    /**
     * Get the operation backed by the processor instance.
     *
     * @return The operation backed by the processor instance.
     */
    T getBackedOperation();

    /**
     * Process the backed operation to update the topology.
     */
    void process();

    /**
     * Allows to know if the operation can be undone.
     *
     * @return True if the operation can be undone.
     */
    boolean canUndo();

    /**
     * Undo the backed operation reverting the topology state.
     */
    void undo();
}
