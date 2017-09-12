package org.alien4cloud.tosca.model.workflow.declarative;

import java.util.Map;

import lombok.Getter;
import lombok.Setter;

/**
 * The declarative workflow on the relationship level
 */
@Getter
@Setter
public class RelationshipDeclarativeWorkflow {

    /**
     * All operations and their dependencies that must be executed by the workflow
     * Each key in the map Configure.create for ex will generate an operation-call step
     */
    private Map<String, RelationshipOperationDeclarativeWorkflow> operations;
}
