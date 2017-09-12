package org.alien4cloud.tosca.model.workflow.declarative;

import java.util.Set;

import lombok.Getter;
import lombok.Setter;

/**
 * Define how a source or target wait for its partner on a relationship
 */
@Getter
@Setter
public class RelationshipWeavingDependencies {
    private String waitSourceState;
    private String waitTargetState;
    private Set<String> waitSourceOperations;
    private Set<String> waitTargetOperations;
}
