package org.alien4cloud.tosca.editor.operations.variable;

import org.alien4cloud.tosca.editor.operations.AbstractUpdateFileOperation;

import lombok.Getter;
import lombok.Setter;

/**
 * Update the expression of an environment type variable of the archive.
 */
@Getter
@Setter
public abstract class AbstractUpdateTopologyVariableOperation extends AbstractUpdateFileOperation {
    private String name;
    private String expression;
}
