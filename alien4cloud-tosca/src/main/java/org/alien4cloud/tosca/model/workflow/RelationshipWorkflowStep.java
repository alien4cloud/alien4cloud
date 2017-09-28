package org.alien4cloud.tosca.model.workflow;

import org.alien4cloud.tosca.model.workflow.activities.AbstractWorkflowActivity;

import com.fasterxml.jackson.annotation.JsonIgnore;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class RelationshipWorkflowStep extends WorkflowStep {
    /**
     * The optional name of a requirement of the target in case the step refers to a relationship rather than a node or group. Note that this is applicable only
     * if the target is a node.
     */
    private String targetRelationship;

    /*
     * ________________________________________________________________________________________________
     * Everything underneath is non tosca, it does exist to facilitate implementation in Alien4Cloud
     * ________________________________________________________________________________________________
     */

    public RelationshipWorkflowStep(String target, String targetRelationship, String sourceHostId, String targetHostId, AbstractWorkflowActivity activity) {
        setTarget(target);
        this.targetRelationship = targetRelationship;
        this.sourceHostId = sourceHostId;
        this.targetHostId = targetHostId;
        setActivity(activity);
    }

    /** The node id of the host of the source of the relationship **/
    private String sourceHostId;

    /** The node id of the host of the target of the relationship **/
    private String targetHostId;

    @JsonIgnore
    public String getStepAsString() {
        String sid = getTarget() + "_" + getTargetRelationship();
        return sid + "_" + getActivity().getRepresentation();
    }
}
