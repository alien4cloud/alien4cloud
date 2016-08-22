package alien4cloud.plugin.model;

import alien4cloud.plugin.model.PluginComponentDescriptor;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor(suppressConstructorProperties = true)
@NoArgsConstructor
@ApiModel("Result for a request for specific plugin components.")
public class PluginComponent {
    @ApiModelProperty(value = "Id of the plugin that contains the component.")
    private String pluginId;
    @ApiModelProperty(value = "Name of the plugin that contains the component.")
    private String pluginName;
    @ApiModelProperty(value = "Version of the plugin that contains the component.")
    private String version;
    @ApiModelProperty(value = "Description of the component within the plugin.")
    private PluginComponentDescriptor componentDescriptor;
}
