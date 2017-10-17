package org.alien4cloud.tosca.model.workflow.activities;

import lombok.Getter;
import lombok.Setter;

/**
 * Delegate the workflow for a node expected to be provided by the orchestrator.
 */
@Getter
@Setter
public class DelegateWorkflowActivity extends AbstractWorkflowActivity {
    /** The name of the delegate workflow. */
    private String delegate;

    @Override
    public String getRepresentation() {
        return delegate;
    }
}
