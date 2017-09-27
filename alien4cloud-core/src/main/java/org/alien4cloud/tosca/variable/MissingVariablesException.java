package org.alien4cloud.tosca.variable;

import lombok.Getter;

import java.util.Set;

@Getter
public class MissingVariablesException extends Exception {
    private Set<String> missingVariables;
    private Set<String> unresolvableInputs;

    public MissingVariablesException(Set<String> missingVariables, Set<String> unresolvableInputs) {
        this.missingVariables = missingVariables;
        this.unresolvableInputs = unresolvableInputs;
    }
}
