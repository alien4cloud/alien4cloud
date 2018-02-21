package org.alien4cloud.tosca.editor.operations.variable;

import org.apache.commons.lang3.StringUtils;

import lombok.Getter;
import lombok.Setter;

/**
 * Update the expression of an environment variable of the archive.
 */
@Getter
@Setter
public class UpdateEnvironmentVariableOperation extends AbstractUpdateTopologyVariableOperation {
    private String environmentId;

    @Override
    public String commitMessage() {
        if (StringUtils.isBlank(this.getExpression())) {
            return "Deleted variable <" + this.getName() + "> from environment <" + environmentId + ">";
        } else {
            return "Updated value of variable <" + this.getName() + "> to <" + getExpression() + "> for environment <" + environmentId + ">";
        }
    }
}
