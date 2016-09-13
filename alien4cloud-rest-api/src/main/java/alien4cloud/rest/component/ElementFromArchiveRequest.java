package alien4cloud.rest.component;

import java.util.Collection;
import java.util.Set;

import alien4cloud.model.components.CSARDependency;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor(suppressConstructorProperties = true)
public class ElementFromArchiveRequest {
    private String elementName;
    private QueryComponentType componentType;
    private Set<CSARDependency> dependencies;
}