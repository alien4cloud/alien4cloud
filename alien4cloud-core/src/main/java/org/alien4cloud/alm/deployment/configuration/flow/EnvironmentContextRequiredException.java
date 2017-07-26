package org.alien4cloud.alm.deployment.configuration.flow;

import alien4cloud.exception.TechnicalException;

/**
 * Exception to be thrown when a ITopologyModifier requires to be processed with an environment context and none is provided.
 */
public class EnvironmentContextRequiredException extends TechnicalException {
    public EnvironmentContextRequiredException(String providerName) {
        super("Topology modifier <" + providerName + "> requires an environment to be processed and none was available.");
    }
}