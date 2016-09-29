package alien4cloud.rest.component;

import java.util.Set;

import org.alien4cloud.tosca.model.CSARDependency;
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