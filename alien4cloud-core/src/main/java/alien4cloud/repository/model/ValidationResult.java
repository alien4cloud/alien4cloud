package alien4cloud.repository.model;

import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@NoArgsConstructor
@AllArgsConstructor(suppressConstructorProperties = true)
@Getter
@Setter
@EqualsAndHashCode(of = "status")
public class ValidationResult {
    public static final ValidationResult SUCCESS = new ValidationResult(ValidationStatus.SUCCESS, null);
    private ValidationStatus status;
    private String message;
}
