package org.alien4cloud.tosca.model.workflow.conditions;

import java.util.List;

import lombok.Getter;
import lombok.Setter;

/**
 * Definition of a precondition that must be valid for a workflow or sub-workflow to be executed.
 */
@Getter
@Setter
public class PreconditionDefinition {
    /** The target of the precondition (this can be a node template name, a group name). */
    private String target;
    /**
     * The optional name of a requirement of the target in case the precondition has to be processed on a relationship rather than a node or group. Note that
     * this is applicable only if the target is a node..
     */
    private String targetRelationship;
    /** A list of workflow condition clause definitions. Assertion between elements of the condition are evaluated as an AND condition. */
    private List<AbstractConditionClause> condition;
}
