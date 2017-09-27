package org.alien4cloud.tosca.model.workflow.declarative;

import java.util.Set;

import lombok.Getter;
import lombok.Setter;

/**
 * This represents dependencies on the states or operations of a node / relationship. For each state, set-state steps will be created if does not exist.
 */
@Getter
@Setter
public class OperationDeclarativeWorkflow {

    /**
     * The preceding set-state step will be executed before this operation execution step
     */
    private String precedingState;

    /**
     * The following set-state step will be executed after this operation execution step
     */
    private String followingState;

    /**
     * The preceding call-operation steps will be executed before this operation execution step
     */
    private Set<String> precedingOperations;

    /**
     * The following call-operation steps will be executed after this operation execution step
     */
    private Set<String> followingOperations;
}
