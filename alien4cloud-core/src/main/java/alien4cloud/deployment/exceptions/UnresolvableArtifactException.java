package alien4cloud.deployment.exceptions;

import alien4cloud.exception.TechnicalException;

/**
 * Cannot resolve an artifact
 */
public class UnresolvableArtifactException extends TechnicalException {
    public UnresolvableArtifactException(String message, Throwable cause) {
        super(message, cause);
    }

    public UnresolvableArtifactException(String message) {
        super(message);
    }
}
