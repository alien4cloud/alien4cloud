package alien4cloud.tosca.container.validation;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@NoArgsConstructor
@Getter
@Setter
@SuppressWarnings("PMD.UnusedPrivateField")
public class CSARValidationError extends CSARError {

    @SuppressWarnings("PMD.SingularField")
    private String path;

    public CSARValidationError(String errorCode, String message, String path) {
        super(errorCode, message);
        this.path = path;
    }

}
