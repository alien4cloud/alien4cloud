package org.alien4cloud.tosca.model.workflow.activities;

import lombok.Getter;
import lombok.Setter;

/**
 * Inline another workflow defined in the topology (to allow reusability).
 */
@Getter
@Setter
public class InlineWorkflowActivity extends AbstractWorkflowActivity {
    /** The name a topology workflow to inline. */
    private String inline;

    @Override
    public String getRepresentation() {
        return inline;
    }
}
