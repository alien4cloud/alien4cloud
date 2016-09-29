package alien4cloud.plugin.mock;

import java.util.List;
import java.util.Map;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import alien4cloud.model.common.Tag;
import org.alien4cloud.tosca.model.definitions.PropertyDefinition;
import alien4cloud.ui.form.annotation.FormProperties;

@Getter
@Setter
@NoArgsConstructor
@FormProperties({ "firstArgument", "secondArgument", "thirdArgument", "tags", "properties" })
public class ProviderConfig {

    private List<Tag> tags;

    private Map<String, PropertyDefinition> properties;
}
