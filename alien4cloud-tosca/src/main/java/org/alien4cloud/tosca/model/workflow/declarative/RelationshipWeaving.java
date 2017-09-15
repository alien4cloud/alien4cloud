package org.alien4cloud.tosca.model.workflow.declarative;

import java.util.Map;

import lombok.Getter;
import lombok.Setter;

/**
 * Contains state and operation weaving configuration for source or target of a relationship
 */
@Getter
@Setter
public class RelationshipWeaving {

    private Map<String, OperationDeclarativeWorkflow> states;

    private Map<String, OperationDeclarativeWorkflow> operations;
}
