package alien4cloud.rest.service.model;

import javax.validation.constraints.Pattern;
import javax.validation.constraints.Size;

import alien4cloud.rest.utils.PatchNotBlankValidator;
import alien4cloud.rest.utils.PatchNotBlankValidator.PatchNotBlank;
import alien4cloud.rest.utils.PatchNotNullValidator;
import org.alien4cloud.tosca.model.instances.NodeInstance;

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
public class PatchServiceResourceRequest {
    @ApiModelProperty(value = "The new name of the service or undefined if the update request should not update the service name.")
    @PatchNotBlank
    private String name;
    @ApiModelProperty(value = "The new version of the service or undefined if the update request should not update the service version.")
    @PatchNotBlank
    private String version;
    @ApiModelProperty(value = "The new description of the service or undefined if update request should not update the service description.")
    private String description;
    @ApiModelProperty(value = "The new node instance definition for the service or undefined if update request should not update the node instance definition.", notes = "In order to change the state (or another single attribute) it is better to use the advanced API for simpler usage and better performance. It is not authorized to change the type of the node if the version doesn't actually changes.")
    private NodeInstanceDTO nodeInstance;
    @ApiModelProperty(value = "The new list of location ids or undefined if update request should not update the service location ids.")
    private String[] locationIds;
    @ApiModelProperty(value = "Map capability name -> relationship type id that optionally defines a relationship type to use to perform the service side operations to connect to the service on a given capability")
    private Map<String, String> capabilitiesRelationshipTypes;
    @ApiModelProperty(value = "Map requirement name -> relationship type id that optionally defines a relationship type to use to perform the service side operations to connect to the service on a given requirement.")
    private Map<String, String> requirementsRelationshipTypes;
}