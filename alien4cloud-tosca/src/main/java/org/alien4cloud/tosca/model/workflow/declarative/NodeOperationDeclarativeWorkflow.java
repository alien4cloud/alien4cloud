package org.alien4cloud.tosca.model.workflow.declarative;

import java.util.Set;

import lombok.Getter;
import lombok.Setter;

/**
 * This declarative workflow represents how the operation will be generated in an imperative workflow, the dependencies specified in the super class
 * {@link OperationDeclarativeWorkflow} indicate dependencies on operations / states of the node it-self
 */
@Getter
@Setter
public class NodeOperationDeclarativeWorkflow extends OperationDeclarativeWorkflow {
    /**
     * A precondition will be added so that the operation will only be executed if the node is in one of those states.
     */
    private Set<String> states;

    /**
     * The host on which the node operation should be executed (SELF or CENTRAL, the manager it-self)
     */
    private NodeOperationHost operationHost;
}
