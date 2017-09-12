package org.alien4cloud.tosca.model.workflow.declarative;

import java.util.Map;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class DefaultDeclarativeWorkflows {
    private Map<String, NodeDeclarativeWorkflow> nodeWorkflows;
    /* Map of workflow name -> default relationship declarative workflow */
    private Map<String, RelationshipDeclarativeWorkflow> relationshipWorkflows;
    /* Map of workflow name -> weaving configuration for the workflow */
    private Map<String, RelationshipWeavingDeclarativeWorkflow> relationshipsWeaving;
}
