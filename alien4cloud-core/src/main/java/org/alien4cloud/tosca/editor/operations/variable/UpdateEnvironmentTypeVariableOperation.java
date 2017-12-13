package org.alien4cloud.tosca.editor.operations.variable;

import alien4cloud.model.application.EnvironmentType;
import lombok.Getter;
import lombok.Setter;

/**
 * Update the expression of an environment type variable of the archive.
 */
@Getter
@Setter
public class UpdateEnvironmentTypeVariableOperation extends AbstractUpdateTopologyVariableOperation {
    private EnvironmentType environmentType;
}
