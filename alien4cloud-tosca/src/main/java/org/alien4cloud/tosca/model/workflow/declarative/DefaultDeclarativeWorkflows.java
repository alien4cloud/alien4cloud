package org.alien4cloud.tosca.model.workflow.declarative;

import java.util.Map;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class DefaultDeclarativeWorkflows {
    /* Map of workflow name -> default node declarative workflow */
    private Map<String, NodeDeclarativeWorkflow> nodeWorkflows;
    /* Map of workflow name -> default relationship declarative workflow */
    private Map<String, RelationshipDeclarativeWorkflow> relationshipWorkflows;
    /* Map of relationship type -> workflow name -> weaving configuration for the workflow */
    private Map<String, Map<String, RelationshipWeavingDeclarativeWorkflow>> relationshipsWeaving;
}
