package alien4cloud.tosca.container.validation;

import javax.validation.ConstraintViolation;

public final class CSARErrorFactory {

    private CSARErrorFactory() {
    }

    /**
     * Convert a Spring validation error to a CSARError
     * 
     * @param violation the violation to be translated
     * @return CSARError
     */
    public static CSARError createValidationError(ConstraintViolation<?> violation) {
        StringBuilder buffer = new StringBuilder("Error while validating type [");
        buffer.append(violation.getLeafBean().getClass().getName()).append("], Path [");
        buffer.append(violation.getPropertyPath().toString()).append("] : ");
        buffer.append(violation.getMessage());
        return new CSARValidationError(violation.getConstraintDescriptor() != null ? violation.getConstraintDescriptor().getAnnotation().annotationType()
                .getSimpleName() : "", buffer.toString(), violation.getPropertyPath().toString());
    }

    /**
     * Referenced type not found
     * 
     * @param errorCode the specific error code
     * @param elementName name of the element
     * @param notFoundType not found referenced type
     * @return CSARError
     */
    public static CSARError createTypeNotFoundError(CSARErrorCode errorCode, String elementName, String notFoundType) {
        return new CSARTypeNotFoundError(errorCode.toString(), "Error while compiling element [" + elementName + "] : Referenced type not found ["
                + notFoundType + "]", elementName, notFoundType);
    }

    /**
     * A type is declared more than once
     * 
     * @param errorCode the specific error code
     * @param elementName name of the element
     * @return CSARError
     */
    public static CSARError createDuplicatedTypeError(CSARErrorCode errorCode, String elementName) {
        return new CSARDuplicatedTypeError(errorCode.toString(), "Duplicated declaration detected for element with name [" + elementName + "]", elementName);
    }

    /**
     * Error related to parsing
     * 
     * @param errorCode the error code
     * @param message the linked message
     * @return CSARError
     */
    public static CSARError createParsingError(CSARErrorCode errorCode, int lineNr, int colNr, String message) {
        return new CSARParsingError(lineNr, colNr, errorCode.toString(), message);
    }
}