package alien4cloud.orchestrators.rest.model;

import lombok.Getter;
import lombok.Setter;

import org.hibernate.validator.constraints.NotBlank;

@Getter
@Setter
public class UpdateLocationResourceTemplatePropertyRequest {
    @NotBlank
    private String propertyName;
    private Object propertyValue;
}
