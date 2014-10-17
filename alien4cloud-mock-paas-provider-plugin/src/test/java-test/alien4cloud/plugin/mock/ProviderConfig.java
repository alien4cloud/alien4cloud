package alien4cloud.plugin.mock;

import java.util.List;
import java.util.Map;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import alien4cloud.component.model.Tag;
import alien4cloud.tosca.container.model.type.PropertyDefinition;
import alien4cloud.ui.form.annotation.FormProperties;

@Getter
@Setter
@NoArgsConstructor
@FormProperties({ "firstArgument", "secondArgument", "thirdArgument", "tags", "properties" })
@SuppressWarnings("PMD.UnusedPrivateField")
public class ProviderConfig {

    private List<Tag> tags;

    private Map<String, PropertyDefinition> properties;
}
