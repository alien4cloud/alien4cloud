package alien4cloud.tosca.container.exception;

import alien4cloud.tosca.container.validation.CSARError;
import alien4cloud.tosca.container.validation.CSARErrorCode;
import alien4cloud.tosca.container.validation.CSARErrorFactory;

/**
 * Thrown when type or element definition is duplicated in the same archive or already existed in the repository.
 * 
 * @author mkv
 * 
 */
public class CSARDuplicatedElementDeclarationException extends CSARParsingException {

    private static final long serialVersionUID = 695334688586642015L;

    private String elementName;

    public CSARDuplicatedElementDeclarationException(String fileName, CSARErrorCode csarErrorCode, String elementName, String message) {
        super(fileName, csarErrorCode, message);
        this.elementName = elementName;
    }

    @Override
    public CSARError createCSARError() {
        return CSARErrorFactory.createDuplicatedTypeError(getErrorCode(), this.elementName);
    }
}
