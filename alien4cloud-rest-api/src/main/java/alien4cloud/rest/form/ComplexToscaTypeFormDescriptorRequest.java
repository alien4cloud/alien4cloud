package alien4cloud.rest.form;

import java.util.Set;

import javax.validation.constraints.NotNull;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.alien4cloud.tosca.model.CSARDependency;
import org.alien4cloud.tosca.model.definitions.PropertyDefinition;

@Getter
@Setter
@NoArgsConstructor
public class ComplexToscaTypeFormDescriptorRequest {

    @NotNull
    private PropertyDefinition propertyDefinition;

    private Set<CSARDependency> dependencies;
}
