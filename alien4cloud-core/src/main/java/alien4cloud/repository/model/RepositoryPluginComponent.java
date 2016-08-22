package alien4cloud.repository.model;

import alien4cloud.plugin.model.PluginComponent;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor(suppressConstructorProperties = true)
@NoArgsConstructor
public class RepositoryPluginComponent {

    private PluginComponent pluginComponent;

    private String repositoryType;
}
