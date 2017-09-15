package org.alien4cloud.tosca.model.workflow;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.alien4cloud.tosca.model.definitions.PropertyDefinition;
import org.alien4cloud.tosca.model.workflow.conditions.PreconditionDefinition;
import org.apache.commons.collections4.CollectionUtils;

import alien4cloud.paas.wf.validation.AbstractWorkflowError;
import lombok.Getter;
import lombok.Setter;

/**
 * Tosca Workflow that can be executed on a topology.
 */
@Getter
@Setter
public class Workflow {
    /** Name of the workflow **/
    private String name;
    /** Description of the workflow. */
    private String description;
    /** Additional metadata for the workflow. */
    private Map<String, String> metadata;
    /** Inputs of the workflow. */
    private Map<String, PropertyDefinition> inputs;
    /** List of preconditions that must be valid so the workflow or sub-workflow is executed. */
    private List<PreconditionDefinition> preconditions;
    /** Initial steps of the workflow. */
    private Map<String, WorkflowStep> steps = new HashMap<>();

    /*
     * ________________________________________________________________________________________________
     * Everything underneath is non tosca, it does exist to facilitate implementation in Alien4Cloud
     * ________________________________________________________________________________________________
     */
    private boolean isStandard;
    private Set<String> hosts = new HashSet<>();
    private List<AbstractWorkflowError> errors = new ArrayList<>();

    public WorkflowStep addStep(WorkflowStep step) {
        steps.put(step.getName(), step);
        return step;
    }

    public void clearErrors() {
        errors = new ArrayList<>();
    }

    public boolean hasErrors() {
        return CollectionUtils.isNotEmpty(errors);
    }

    public void addErrors(List<AbstractWorkflowError> errorsToAdd) {
        errors.addAll(errorsToAdd);
    }
}