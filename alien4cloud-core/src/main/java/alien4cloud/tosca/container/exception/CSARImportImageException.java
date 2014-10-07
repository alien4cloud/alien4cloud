package alien4cloud.tosca.container.exception;

import alien4cloud.tosca.container.validation.CSARErrorCode;

/**
 * Exception thrown when a problem occurs when importing image/icon
 * 
 * @author mourouvi
 * 
 */

public class CSARImportImageException extends CSARParsingException {

    private static final long serialVersionUID = 2459052732532403306L;

    public CSARImportImageException(String fileName, CSARErrorCode errorCode, String message, Throwable cause) {
        super(fileName, errorCode, message, cause);
    }

    public CSARImportImageException(String fileName, CSARErrorCode errorCode, String message) {
        super(fileName, errorCode, message);
    }

}
