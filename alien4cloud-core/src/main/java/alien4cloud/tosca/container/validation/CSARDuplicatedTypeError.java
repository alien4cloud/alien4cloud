package alien4cloud.tosca.container.validation;

import lombok.NoArgsConstructor;

@NoArgsConstructor
public class CSARDuplicatedTypeError extends CSARCompilationError {

    public CSARDuplicatedTypeError(String errorCode, String message, String invalidElementName) {
        super(errorCode, message, invalidElementName);
    }

}
