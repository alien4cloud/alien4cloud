package org.alien4cloud.tosca.editor.processors;

import org.alien4cloud.tosca.editor.operations.AbstractEditorOperation;

/**
 * Interface to implement if an operation should perform some persistent operations (to other objects, to file store) before the save and commit.
 */
public interface IEditorCommitableProcessor<T extends AbstractEditorOperation> extends IEditorOperationProcessor<T> {
    /**
     * This method is called before the save and commit operations when the editor save method is triggered.
     * 
     * @param operation The operation of the processor (same operation that is processed against the in memory topology).
     */
    void beforeCommit(T operation);
}