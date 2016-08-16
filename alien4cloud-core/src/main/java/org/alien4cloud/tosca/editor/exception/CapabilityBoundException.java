package org.alien4cloud.tosca.editor.exception;

import alien4cloud.exception.TechnicalException;

/**
 * Exception thrown if an operation is breaching the bounds of a requirement.
 */
public class CapabilityBoundException extends TechnicalException {
    public CapabilityBoundException(String nodeTemplateName, String capabilityName) {
        super("UpperBound reached on capability <" + capabilityName + "> on node <" + nodeTemplateName + ">.");
    }
}