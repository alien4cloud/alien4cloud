package org.alien4cloud.tosca.variable;

import org.alien4cloud.tosca.variable.model.Variable;

import lombok.Getter;
import lombok.Setter;

/**
 * DTO to define for a given environment, the definition of a given variable
 */
@Getter
@Setter
public class EnvironmentVariableDefinitionDTO {
    private String environmentId;
    private String environmentName;
    private Variable variable;
}
