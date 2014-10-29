package alien4cloud.tosca.container.validation;

import java.util.Map;
import java.util.Set;

import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import alien4cloud.tosca.container.deserializer.CSARErrorDeserializer;
import alien4cloud.tosca.model.Csar;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@SuppressWarnings("PMD.UnusedPrivateField")
@EqualsAndHashCode
public class CSARValidationResult {

    private Csar csar;
    /**
     * Path of the definition file where error has been detected
     */
    @JsonDeserialize(contentUsing = CSARErrorDeserializer.class)
    private Map<String, Set<CSARError>> errors;

    @JsonIgnore
    public boolean isValid() {
        return errors == null || errors.isEmpty();
    }

    @Override
    public String toString() {
        StringBuilder buffer = new StringBuilder();
        if (csar != null) {
            buffer.append("Archive: [").append(csar.getName()).append("], Version: [").append(csar.getVersion()).append("]: \n");
        }
        if (isValid()) {
            buffer.append("No errors found for definitions files " + errors.keySet());
        } else {
            buffer.append("Validation Errors found :\n");
            for (Map.Entry<String, Set<CSARError>> errorEntry : getErrors().entrySet()) {
                buffer.append("- For file ").append(errorEntry.getKey()).append('\n');
                if (errorEntry.getValue() != null && !errorEntry.getValue().isEmpty()) {
                    for (CSARError error : errorEntry.getValue()) {
                        buffer.append("\t+ ").append(error.getMessage()).append('\n');
                    }
                }
            }
        }
        return buffer.toString();
    }

    public CSARValidationResult(Map<String, Set<CSARError>> errors) {
        this.errors = errors;
    }
}
