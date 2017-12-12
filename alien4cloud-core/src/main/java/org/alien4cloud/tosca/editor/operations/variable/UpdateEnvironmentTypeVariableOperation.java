package org.alien4cloud.tosca.editor.operations.variable;

import org.alien4cloud.tosca.editor.operations.AbstractUpdateFileOperation;

import alien4cloud.model.application.EnvironmentType;
import lombok.Getter;
import lombok.Setter;

/**
 * Update the expression of an environment type variable of the archive.
 */
@Getter
@Setter
public class UpdateEnvironmentTypeVariableOperation extends AbstractUpdateFileOperation {
    private EnvironmentType environmentType;
    private String name;
    private String expression;
}
