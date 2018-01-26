package org.alien4cloud.tosca.editor.exception;

import alien4cloud.exception.TechnicalException;

/**
 * Exception thrown if an operation is breaching the bounds of a requirement.
 */
public class RequirementBoundException extends TechnicalException {
    public RequirementBoundException(String nodeTemplateName, String requirementName) {
        super("UpperBound reached on requirement [" + requirementName + "] on node [" + nodeTemplateName + "].");
    }
}