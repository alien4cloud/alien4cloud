package org.alien4cloud.tosca.model.workflow.declarative;

import lombok.Getter;
import lombok.Setter;

/**
 * {@link RelationshipOperationDeclarativeWorkflow} declare dependencies for a particular relationship operation. This
 * {@link RelationshipWeavingDeclarativeWorkflow} on the other hand declare dependencies between the source and the target of a relationship and not linked to
 * any relationship operation.
 */
@Getter
@Setter
public class RelationshipWeavingDeclarativeWorkflow {
    private RelationshipWeaving source;
    private RelationshipWeaving target;
}
