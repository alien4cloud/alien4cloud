package alien4cloud.rest.internal;

import alien4cloud.tosca.container.model.type.PropertyDefinition;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuppressWarnings("PMD.UnusedPrivateField")
public class PropertyRequest {
    private String propertyId;
    private String propertyValue;
    private PropertyDefinition propertyDefinition;
}
