package alien4cloud.rest.service.model;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.validator.constraints.NotBlank;
import org.hibernate.validator.constraints.NotEmpty;

import javax.validation.constraints.NotNull;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@ApiModel("Request for creation of a new service.")
public class CreateServiceResourceRequest {
    @NotEmpty
    @ApiModelProperty(value = "Name of the new service (must be unique for a given version).", required = true)
    private String name;

    @NotEmpty
    @ApiModelProperty(value = "Version of the new service.", required = true)
    private String version;

    @NotEmpty
    @ApiModelProperty(value = "The node type to use to build the service node template.", required = true)
    private String nodeType;

    @NotEmpty
    @ApiModelProperty(value = "Archive version of the node type.", required = true)
    private String nodeTypeVersion;
}