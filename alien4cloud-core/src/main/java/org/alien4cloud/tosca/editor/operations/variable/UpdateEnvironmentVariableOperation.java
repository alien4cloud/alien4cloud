package org.alien4cloud.tosca.editor.operations.variable;

import lombok.Getter;
import lombok.Setter;

/**
 * Update the expression of an environment variable of the archive.
 */
@Getter
@Setter
public class UpdateEnvironmentVariableOperation extends AbstractUpdateTopologyVariableOperation {
    private String environmentId;
}
