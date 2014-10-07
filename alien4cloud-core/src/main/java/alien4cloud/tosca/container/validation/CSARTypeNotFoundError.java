package alien4cloud.tosca.container.validation;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@SuppressWarnings("PMD.UnusedPrivateField")
public class CSARTypeNotFoundError extends CSARCompilationError {

    @SuppressWarnings("PMD.SingularField")
    private String notFoundType;

    public CSARTypeNotFoundError(String errorCode, String message, String invalidElementName, String notFoundType) {
        super(errorCode, message, invalidElementName);
        this.notFoundType = notFoundType;
    }

}
