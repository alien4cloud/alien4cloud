package alien4cloud.model.components;

import alien4cloud.exception.TechnicalException;

/**
 * Exception happened while user would link tow incompatible PropertyDefinition
 * 
 */
public class IncompatiblePropertyDefinitionException extends TechnicalException {

    private static final long serialVersionUID = 1L;

    public IncompatiblePropertyDefinitionException() {
        super("The two PropertyDefinition are incompatible.");
    }

    public IncompatiblePropertyDefinitionException(String message) {
        super(message);
    }

    public IncompatiblePropertyDefinitionException(String message, Throwable cause) {
        super(message, cause);
    }

}
