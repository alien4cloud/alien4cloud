package org.alien4cloud.tosca.model.workflow;

import java.util.List;
import java.util.Map;

import org.alien4cloud.tosca.model.definitions.PropertyDefinition;
import org.alien4cloud.tosca.model.workflow.conditions.PreconditionDefinition;

import lombok.Getter;
import lombok.Setter;

/**
 * Tosca Workflow that can be executed on a topology.
 */
@Getter
@Setter
public class Workflow {
    /** Description of the workflow. */
    private String description;
    /** Additional metadata for the workflow. */
    private Map<String, String> metadata;
    /** Inputs of the workflow. */
    private Map<String, PropertyDefinition> inputs;
    /** List of preconditions that must be valid so the workflow or sub-workflow is executed. */
    private List<PreconditionDefinition> preconditions;
    /** Initial steps of the workflow. */
    private Map<String, WorkflowStep> steps;
}