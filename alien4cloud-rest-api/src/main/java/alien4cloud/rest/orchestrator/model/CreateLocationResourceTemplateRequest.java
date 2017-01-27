package alien4cloud.rest.orchestrator.model;

import lombok.Getter;
import lombok.Setter;

import org.hibernate.validator.constraints.NotBlank;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

@Getter
@Setter
@ApiModel("Request for creation of a new location's resource.")
public class CreateLocationResourceTemplateRequest {
    @NotBlank
    @ApiModelProperty(value = "Type of the location's resource.", required = true)
    private String resourceType;
    @NotBlank
    @ApiModelProperty(value = "Name of the location's resource.", required = true)
    private String resourceName;
    @ApiModelProperty(value = "Archive name of the resource type.")
    private String archiveName;
    @ApiModelProperty(value = "Archive version of the resource type.")
    private String archiveVersion;
}
