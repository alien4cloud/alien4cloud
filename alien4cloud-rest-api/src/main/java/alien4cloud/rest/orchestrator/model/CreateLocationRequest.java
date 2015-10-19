package alien4cloud.rest.orchestrator.model;

import lombok.Getter;
import lombok.Setter;

import org.hibernate.validator.constraints.NotEmpty;

import com.wordnik.swagger.annotations.ApiModel;
import com.wordnik.swagger.annotations.ApiModelProperty;

@Getter
@Setter
@ApiModel("Request for creation of a new location.")
public class CreateLocationRequest {
    @NotEmpty
    @ApiModelProperty(value = "Name of the location (must be unique for this orchestrator as this allow users to identify it).", required = true)
    private String name;
    @NotEmpty
    @ApiModelProperty(value = "Type of the infrastructure of the new location.", notes = "The infrastructure type must be one of the one supported by the orchestrator.", required = true)
    private String infrastructureType;
}