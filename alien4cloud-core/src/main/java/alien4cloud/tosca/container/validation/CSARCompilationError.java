package alien4cloud.tosca.container.validation;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@SuppressWarnings("PMD.UnusedPrivateField")
public abstract class CSARCompilationError extends CSARError {

    @SuppressWarnings("PMD.SingularField")
    private String invalidElementName;

    public CSARCompilationError(String errorCode, String message, String invalidElementName) {
        super(errorCode, message);
        this.invalidElementName = invalidElementName;
    }
}