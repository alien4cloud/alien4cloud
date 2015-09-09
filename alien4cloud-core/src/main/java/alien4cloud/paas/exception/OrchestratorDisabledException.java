package alien4cloud.paas.exception;

import alien4cloud.exception.FunctionalException;
import alien4cloud.exception.TechnicalException;

/**
 * Functional exception to be thrown when a user tries to interact with an non-enabled cloud.
 */
public class OrchestratorDisabledException extends TechnicalException {
    private static final long serialVersionUID = 1L;

    public OrchestratorDisabledException(String message) {
        super(message);
    }
}