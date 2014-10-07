package alien4cloud.tosca.container.validation;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@SuppressWarnings("PMD.UnusedPrivateField")
public abstract class CSARError {

    private String errorCode;
    private String message;

    @Override
    public String toString() {
        return "CSAR Error [" + getErrorCode() + "] : " + getMessage();
    }
}
