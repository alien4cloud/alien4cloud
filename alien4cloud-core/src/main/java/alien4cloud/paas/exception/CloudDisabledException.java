package alien4cloud.paas.exception;

import alien4cloud.exception.FunctionalException;

/**
 * Functional exception to be thrown when a user tries to interact with an non-enabled cloud.
 */
public class CloudDisabledException extends FunctionalException {
    private static final long serialVersionUID = 1L;

    public CloudDisabledException(String message) {
        super(message);
    }
}