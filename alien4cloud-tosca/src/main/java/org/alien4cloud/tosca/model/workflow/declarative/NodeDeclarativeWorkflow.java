package org.alien4cloud.tosca.model.workflow.declarative;

import java.util.Map;
import java.util.Set;

import lombok.Getter;
import lombok.Setter;

/**
 * The declarative workflow on the node level
 */
@Getter
@Setter
public class NodeDeclarativeWorkflow {
    /**
     * All the states of the node that could be set by the workflow
     * Each state will generate a set-state step
     */
    private Set<String> states;

    /**
     * All operations and their dependencies that must be executed by the workflow
     * Each key in the map Standard.create for ex will generate an operation-call step
     */
    private Map<String, NodeOperationDeclarativeWorkflow> operations;
}
