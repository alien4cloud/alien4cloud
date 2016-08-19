package alien4cloud.rest.tags;

import java.util.Set;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor(suppressConstructorProperties = true)
public class TagConfigurationSaveResponse {

    private String id;

    private Set<TagConfigurationValidationError> validationErrors;
}
