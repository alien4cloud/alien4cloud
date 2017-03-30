package alien4cloud.rest.service.model;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;

import org.hibernate.validator.constraints.NotBlank;

import com.fasterxml.jackson.annotation.JsonInclude;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Getter;
import lombok.Setter;

import java.util.Map;

/**
 * Update a Service Resource.
 */
@Getter
@Setter
@JsonInclude(JsonInclude.Include.NON_NULL)
@ApiModel(description = "Request to update a service resource.")
public class UpdateServiceResourceRequest {
    @ApiModelProperty(value = "The name of the service.", required = true)
    @NotBlank
    private String name;
    @ApiModelProperty(value = "The version of the service.", required = true)
    @NotBlank
    private String version;
    @ApiModelProperty(value = "The description of the service.")
    private String description;
    @ApiModelProperty(value = "The node instance definition for the service.", required = true)
    @NotNull
    @Valid
    private NodeInstanceDTO nodeInstance;
    @ApiModelProperty(value = "The list of locations.")
    private String[] locationIds;
    @ApiModelProperty(value = "Map capability name -> relationship type id that optionally defines a relationship type to use to perform the service side operations to connect to the service on a given capability")
    private Map<String, String> capabilitiesRelationshipTypes;
    @ApiModelProperty(value = "Map requirement name -> relationship type id that optionally defines a relationship type to use to perform the service side operations to connect to the service on a given requirement.")
    private Map<String, String> requirementsRelationshipTypes;
}