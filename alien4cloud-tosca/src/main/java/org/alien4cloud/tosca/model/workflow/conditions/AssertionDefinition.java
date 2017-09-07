package org.alien4cloud.tosca.model.workflow.conditions;

import org.alien4cloud.tosca.model.definitions.PropertyConstraint;

import java.util.List;

/**
 * Assertion condition clause.
 */
public class AssertionDefinition {
    private String attribute;
    private List<PropertyConstraint> constraints;
}