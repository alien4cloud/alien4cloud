package org.alien4cloud.tosca.model.workflow;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.alien4cloud.tosca.model.workflow.activities.AbstractWorkflowActivity;
import org.alien4cloud.tosca.model.workflow.conditions.AbstractConditionClause;

import com.fasterxml.jackson.annotation.JsonIgnore;

import alien4cloud.paas.exception.NotSupportedException;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * A step in the workflow.
 */
@Getter
@Setter
@NoArgsConstructor
public class WorkflowStep {
    /** The target of the step (this can be a node template name, a group name). */
    private String target;
    /**
     * The optional name of a requirement of the target in case the step refers to a relationship rather than a node or group. Note that this is applicable only
     * if the target is a node.
     */
    private String targetRelationship;
    /** The target host of the step. SOURCE or TARGET for a relationship. */
    private String operationHost;
    /** Filter definition for optional steps. */
    private List<AbstractConditionClause> filter;
    /** The list of activities to call in a sequence as part of that workflow step. */
    private List<AbstractWorkflowActivity> activities;
    /** The steps to trigger (in parallel if multiple) if the workflow step has been executed correctly. */
    private Set<String> onSuccess;
    /** The steps to trigger (in parallel if multiple) if the workflow step has failed. */
    private Set<String> onFailure;

    /*
     * ________________________________________________________________________________________________
     * Everything underneath is non tosca, it does exist to facilitate implementation in Alien4Cloud
     * ________________________________________________________________________________________________
     */

    public WorkflowStep(String target, String hostId, AbstractWorkflowActivity activity) {
        this.target = target;
        this.hostId = hostId;
        setActivity(activity);
    }

    /** The id / name of the step in the workflow **/
    private String name;
    /** The steps that precedes immediately this step in the workflow sequence **/
    private Set<String> precedingSteps;
    /** The node id of the host where the step will be executed **/
    private String hostId;

    @JsonIgnore
    public AbstractWorkflowActivity getActivity() {
        if (activities == null) {
            return null;
        }
        if (activities.size() > 1) {
            throw new NotSupportedException("Only support single activity step");
        }
        return activities.iterator().next();
    }

    @JsonIgnore
    public void setActivity(AbstractWorkflowActivity activity) {
        if (activities == null) {
            activities = new ArrayList<>();
        }
        activities.add(activity);
    }

    @JsonIgnore
    public String getStepAsString() {
        String sid = getTarget() + (getTargetRelationship() != null ? "_" + getTargetRelationship() : "");
        return sid + "_" + getActivity().getRepresentation();
    }

    public void addPreceding(String name) {
        if (this.precedingSteps == null) {
            this.precedingSteps = new HashSet<>();
        }
        this.precedingSteps.add(name);
    }

    public void removePreceding(String name) {
        this.precedingSteps.remove(name);
    }

    public void addFollowing(String name) {
        if (this.onSuccess == null) {
            this.onSuccess = new HashSet<>();
        }
        this.onSuccess.add(name);
    }

    public void removeFollowing(String name) {
        this.onSuccess.remove(name);
    }
}