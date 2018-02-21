package org.alien4cloud.tosca.variable;

import org.alien4cloud.tosca.variable.model.Variable;

import lombok.Getter;
import lombok.Setter;

/**
 * DTO to define for a given scope, the expression (value) of a given variable
 * Scope can be: application, environment or environment Type
 */
@Getter
@Setter
public class ScopeVariableExpressionDTO {
    private String scopeId;
    private String scopeName;
    private Variable variable;
}
