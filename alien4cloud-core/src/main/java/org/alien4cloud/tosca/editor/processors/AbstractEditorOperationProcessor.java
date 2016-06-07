package org.alien4cloud.tosca.editor.processors;

import alien4cloud.model.topology.Topology;
import org.alien4cloud.tosca.editor.IEditorOperationProcessor;
import org.alien4cloud.tosca.editor.commands.IEditorOperation;

/**
 * Abstract editor operation processor manage the undo operation.
 */
public abstract class AbstractEditorOperationProcessor<T extends IEditorOperation> implements IEditorOperationProcessor<T> {
    private IEditorOperationProcessor<? extends IEditorOperation> undoProcessor;

    @Override
    public void process() {
        this.undoProcessor = doProcess();
    }

    /**
     * process the backed operation on the thread topology context.
     * 
     * @return An instance of the undo processor for the operation or null if the operation cannot be undone.
     */
    public abstract IEditorOperationProcessor<? extends IEditorOperation> doProcess();

    @Override
    public void undo() {
        if (this.undoProcessor != null) {
            this.undoProcessor.process();
        } else {
            throw new IllegalArgumentException(
                    "Undo is not possible for the current operation. Check that undo is supported for the action and that the action has be done before trying to be undone.");
        }
    }

    @Override
    public boolean canUndo() {
        return undoProcessor != null;
    }
}
