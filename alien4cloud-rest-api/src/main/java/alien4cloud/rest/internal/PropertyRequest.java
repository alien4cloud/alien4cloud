package alien4cloud.rest.internal;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import alien4cloud.model.components.PropertyDefinition;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuppressWarnings("PMD.UnusedPrivateField")
public class PropertyRequest {
    private String value;
    private String definitionId;
    private PropertyDefinition propertyDefinition;
}
