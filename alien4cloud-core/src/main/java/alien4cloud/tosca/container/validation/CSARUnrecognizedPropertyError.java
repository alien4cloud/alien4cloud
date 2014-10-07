package alien4cloud.tosca.container.validation;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Error to be dispatched when a yaml file has an incorrect mapping (field outside of model definition etc.)
 */
@Getter
@Setter
@NoArgsConstructor
@SuppressWarnings("PMD.UnusedPrivateField")
public class CSARUnrecognizedPropertyError extends CSARParsingError {
    private String propertyName;

    public CSARUnrecognizedPropertyError(String propertyName, int lineNr, int colNr, String errorCode, String message) {
        super(lineNr, colNr, errorCode, message);
        this.propertyName = propertyName;
    }
}