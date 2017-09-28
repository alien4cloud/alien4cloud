package org.alien4cloud.tosca.model.workflow;

import org.alien4cloud.tosca.model.workflow.activities.AbstractWorkflowActivity;

import com.fasterxml.jackson.annotation.JsonIgnore;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@NoArgsConstructor
@Getter
@Setter
public class NodeWorkflowStep extends WorkflowStep {

    /** The node id of the host where the step will be executed **/
    private String hostId;

    public NodeWorkflowStep(String target, String hostId, AbstractWorkflowActivity activity) {
        setTarget(target);
        this.hostId = hostId;
        setActivity(activity);
    }

    @JsonIgnore
    public String getStepAsString() {
        return getTarget() + "_" + getActivity().getRepresentation();
    }
}
