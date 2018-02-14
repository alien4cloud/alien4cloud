package org.alien4cloud.tosca.editor.operations.variable;

import org.apache.commons.lang3.StringUtils;

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

    @Override
    public String commitMessage() {
        if (StringUtils.isBlank(this.getExpression())) {
            return "Deleted variable <" + this.getName() + "> from environment type <" + environmentType + ">";
        } else {
            return "Updated value of variable <" + this.getName() + "> to <" + getExpression() + "> for environment type <" + environmentType + ">";
        }
    }
}
