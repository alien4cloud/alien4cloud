package org.alien4cloud.tosca.editor.exception;

/**
 * Exception thrown if an operation is breaching the bounds of a requirement.
 */
public class CapabilityBoundException extends Exception {
    private String nodeTemplateName;
    private String capabilityName;

    public CapabilityBoundException(String nodeTemplateName, String capabilityName) {
        super("UpperBound reached on capability <" + capabilityName + "> on node <" + nodeTemplateName + ">.");
        this.nodeTemplateName = nodeTemplateName;
        this.capabilityName = capabilityName;
    }
}
