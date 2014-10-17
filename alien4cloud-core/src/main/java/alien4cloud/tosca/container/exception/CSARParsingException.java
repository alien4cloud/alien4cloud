package alien4cloud.tosca.container.exception;

import alien4cloud.tosca.container.validation.CSARCompilationError;
import alien4cloud.tosca.container.validation.CSARDuplicatedTypeError;
import alien4cloud.tosca.container.validation.CSARError;
import alien4cloud.tosca.container.validation.CSARErrorCode;
import alien4cloud.tosca.container.validation.CSARErrorFactory;
import alien4cloud.tosca.container.validation.CSARParsingError;
import alien4cloud.tosca.container.validation.CSARUnrecognizedPropertyError;
import lombok.Getter;

/**
 * Exception thrown when problem happened while parsing csar files
 * 
 * @author mkv
 */
@Getter
@SuppressWarnings("PMD.UnusedPrivateField")
public class CSARParsingException extends CSARFunctionalException {

    private static final long serialVersionUID = 2318462065249492587L;

    private final String fileName;
    private final String propertyName;
    private final int lineNr;
    private final int colNr;

    private final CSARErrorCode errorCode;

    public CSARParsingException(String fileName, String propertyName, int lineNr, int colNr, CSARErrorCode errorCode, String message, Throwable cause) {
        super(errorCode + " In file [" + fileName + "] : " + message, cause);
        this.fileName = fileName;
        this.errorCode = errorCode;
        this.propertyName = propertyName;
        this.lineNr = lineNr;
        this.colNr = colNr;
    }

    public CSARParsingException(String fileName, int lineNr, int colNr, CSARErrorCode errorCode, String message, Throwable cause) {
        this(fileName, null, lineNr, colNr, errorCode, message, cause);
    }

    public CSARParsingException(String fileName, CSARErrorCode errorCode, String message, Throwable cause) {
        this(fileName, 0, 0, errorCode, message, cause);
    }

    public CSARParsingException(String fileName, CSARErrorCode errorCode, String message) {
        this(fileName, errorCode, message, null);
    }

    public CSARError createCSARError() {
        CSARError error = null;
        error = createDuplicatedDefinitionError(error);
        error = createUnrecognizedPropertyError(error);
        error = createMappingPropertyError(error);
        return error == null ? CSARErrorFactory.createParsingError(errorCode, lineNr, colNr, getMessage()) : error;
    }

    private CSARError createDuplicatedDefinitionError(CSARError error) {
        if (error != null) {
            return error;
        }
        if (CSARErrorCode.DUPLICATED_DEFINITION_DECLARATION.equals(errorCode) || CSARErrorCode.DUPLICATED_DEFINITION_DECLARATION.equals(errorCode)) {
            return new CSARDuplicatedTypeError(this.errorCode.toString(), this.getMessage(), fileName);
        }
        return null;
    }

    private CSARError createUnrecognizedPropertyError(CSARError error) {
        if (error != null) {
            return error;
        }
        if (CSARErrorCode.UNRECOGNIZED_PROP_ERROR_DEFINITION_FILE.equals(errorCode) || CSARErrorCode.UNRECOGNIZED_PROP_ERROR_METADATA_FILE.equals(errorCode)) {
            return new CSARUnrecognizedPropertyError(this.propertyName, this.lineNr, this.colNr, this.errorCode.toString(), fileName);
        }
        return null;
    }

    private CSARError createMappingPropertyError(CSARError error) {
        if (error != null) {
            return error;
        }
        if (CSARErrorCode.MAPPING_ERROR_DEFINITION_FILE.equals(errorCode) || CSARErrorCode.MAPPING_ERROR_METADATA_FILE.equals(errorCode)) {
            return new CSARParsingError(this.lineNr, this.colNr, this.errorCode.toString(), fileName);
        }
        return null;
    }
}