package alien4cloud.tosca.container.exception;

import alien4cloud.tosca.container.validation.CSARValidationResult;
import lombok.Getter;

/**
 * Exception dispatched when a CSAR validation process fails.
 */
@Getter
@SuppressWarnings("PMD.UnusedPrivateField")
public class CSARValidationException extends CSARFunctionalException {
    private static final long serialVersionUID = 1L;
    private CSARValidationResult csarValidationResult;

    public CSARValidationException(String message, CSARValidationResult csarValidationResult) {
        super(csarValidationResult == null ? message : message + "\n" + csarValidationResult.toString());
        this.csarValidationResult = csarValidationResult;
    }
}