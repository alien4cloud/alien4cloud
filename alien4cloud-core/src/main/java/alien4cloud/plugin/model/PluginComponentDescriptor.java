package alien4cloud.plugin.model;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Getter;
import lombok.Setter;

import org.springframework.stereotype.Service;

/**
 * Describe a component of a plugin (can be an IPaaSProvider etc.)
 */
@Setter
@Getter
@Service
@ApiModel("Describe a component of a plugin (can be an IOrchestrator etc.).")
public class PluginComponentDescriptor {
    /** Name of the component bean in the plugin spring context. */
    @ApiModelProperty(value = "Name of the component bean in the plugin spring context.")
    private String beanName;
    /** Name of the plugin component. */
    @ApiModelProperty(value = "Name of the plugin component.")
    private String name;
    /** Description of the plugin. */
    @ApiModelProperty(value = "Description of the plugin.")
    private String description;
    /** Type of the plugin (injected by ALIEN plugin loader) */
    @ApiModelProperty(value = "Type of the plugin.")
    private String type;
}