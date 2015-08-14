package alien4cloud.rest.paasprovider;

import java.util.Map;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import alien4cloud.plugin.model.PluginComponentDescriptor;
import alien4cloud.model.components.ScalarPropertyValue;

@Getter
@Setter
@NoArgsConstructor
@SuppressWarnings("PMD.UnusedPrivateField")
public class PaaSProviderDTO {
    private String pluginId;
    private String pluginName;
    private String version;
    private PluginComponentDescriptor componentDescriptor;
}
