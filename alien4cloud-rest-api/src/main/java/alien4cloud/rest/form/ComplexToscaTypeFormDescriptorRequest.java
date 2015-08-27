package alien4cloud.rest.form;

import java.util.Set;

import javax.validation.constraints.NotNull;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import alien4cloud.model.components.CSARDependency;
import alien4cloud.model.components.PropertyDefinition;

@Getter
@Setter
@NoArgsConstructor
public class ComplexToscaTypeFormDescriptorRequest {

    @NotNull
    private PropertyDefinition propertyDefinition;

    private Set<CSARDependency> dependencies;
}
