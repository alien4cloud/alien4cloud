package alien4cloud.rest.orchestrator.model;

import com.wordnik.swagger.annotations.ApiModel;
import com.wordnik.swagger.annotations.ApiModelProperty;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@ApiModel("Request to update a location resource.")
public class UpdateLocationResourceTemplateRequest {
    @ApiModelProperty(value = "New name of the resource.", required = false)
    private String name;
    @ApiModelProperty(value = "Flag to know if the resource is available to be used for configuration or matching.", required = false)
    private Boolean enabled;
}
