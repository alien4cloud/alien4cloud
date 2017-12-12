package org.alien4cloud.tosca.editor.operations.variable;

import org.alien4cloud.tosca.editor.operations.AbstractUpdateFileOperation;

import lombok.Getter;
import lombok.Setter;

/**
 * Update the expression of an environment variable of the archive.
 */
@Getter
@Setter
public class UpdateEnvironmentVariableOperation extends AbstractUpdateFileOperation {
    private String environmentId;
    private String name;
    private String expression;
}
