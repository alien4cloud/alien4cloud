package alien4cloud.rest.orchestrator.model;

import lombok.Getter;
import lombok.Setter;

import org.hibernate.validator.constraints.NotEmpty;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

@Getter
@Setter
@ApiModel("Request for creation of a new orchestrators.")
public class CreateOrchestratorRequest {
    @NotEmpty
    @ApiModelProperty(value = "Name of the orchestrators (must be unique as this allow users to identify it).", required = true)
    private String name;
    @NotEmpty
    @ApiModelProperty(value = "Id of the plugin to use to manage communication with the orchestrators.", required = true)
    private String pluginId;
    @NotEmpty
    @ApiModelProperty(value = "Id of the element of the plugin to use to manage communication with the orchestrators (plugins may have multiple components).", required = true)
    private String pluginBean;
}