package org.alien4cloud.tosca.editor.processors;

import org.alien4cloud.tosca.editor.operations.AbstractEditorOperation;
import org.alien4cloud.tosca.model.Csar;
import org.alien4cloud.tosca.model.templates.Topology;

/**
 * Every IEditorOperation has a related IEditorOperationProcessor implementation that will process it and update the topology model accordingly as well as
 * managing the undo processor creation.
 */
public interface IEditorOperationProcessor<T extends AbstractEditorOperation> {
    /**
     * Process the backed operation to update the topology.
     */
    void process(Csar csar, Topology topology, T operation);
}