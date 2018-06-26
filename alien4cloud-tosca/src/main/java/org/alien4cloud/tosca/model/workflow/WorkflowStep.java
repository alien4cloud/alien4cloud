package org.alien4cloud.tosca.model.workflow;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.alien4cloud.tosca.model.workflow.activities.AbstractWorkflowActivity;
import org.alien4cloud.tosca.model.workflow.conditions.AbstractConditionClause;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

import alien4cloud.paas.exception.NotSupportedException;
import lombok.Getter;
import lombok.Setter;

/**
 * A step in the workflow.
 */
@Getter
@Setter
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonTypeInfo(use = JsonTypeInfo.Id.CLASS, include = JsonTypeInfo.As.PROPERTY, property = "type")
public abstract class WorkflowStep {
    /** The target of the step (this can be a node template name, a group name). */
    private String target;
    /** The target host of the step. SOURCE or TARGET for a relationship. */
    private String operationHost;
    /** Filter definition for optional steps. */
    private List<AbstractConditionClause> filter;
    /** The list of activities to call in a sequence as part of that workflow step. */
    private List<AbstractWorkflowActivity> activities;
    /** The steps to trigger (in parallel if multiple) if the workflow step has been executed correctly. */
    private Set<String> onSuccess = new HashSet<>();
    /** The steps to trigger (in parallel if multiple) if the workflow step has failed. */
    private Set<String> onFailure;

    /*
     * ________________________________________________________________________________________________
     * Everything underneath is non tosca, it does exist to facilitate implementation in Alien4Cloud
     * ________________________________________________________________________________________________
     */
    /** The id / name of the step in the workflow **/
    private String name;
    /** The steps that precedes immediately this step in the workflow sequence **/
    private Set<String> precedingSteps = new HashSet<>();

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
    public abstract String getStepAsString();

    public void addPreceding(String name) {
        this.precedingSteps.add(name);
    }

    public void addAllPrecedings(Set<String> precedings) {
        this.precedingSteps.addAll(precedings);
    }

    public void removeAllPrecedings(Set<String> precedingSteps) {
        this.precedingSteps.removeAll(precedingSteps);
    }

    public boolean removePreceding(String name) {
        return this.precedingSteps.remove(name);
    }

    public void addFollowing(String name) {
        this.onSuccess.add(name);
    }

    public void addAllFollowings(Set<String> followings) {
        this.onSuccess.addAll(followings);
    }

    public boolean removeFollowing(String name) {
        return this.onSuccess.remove(name);
    }

    public void removeAllFollowings(Set<String> followings) {
        this.onSuccess.removeAll(followings);
    }

}