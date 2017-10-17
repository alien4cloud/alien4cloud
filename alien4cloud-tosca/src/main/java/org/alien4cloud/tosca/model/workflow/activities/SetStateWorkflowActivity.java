package org.alien4cloud.tosca.model.workflow.activities;

import lombok.Getter;
import lombok.Setter;

/**
 * Set the state of a node.
 */
@Getter
@Setter
public class SetStateWorkflowActivity extends AbstractWorkflowActivity {

    /* The new state of the node or relationship */
    private String stateName;

    @Override
    public String getRepresentation() {
        return stateName;
    }
}
