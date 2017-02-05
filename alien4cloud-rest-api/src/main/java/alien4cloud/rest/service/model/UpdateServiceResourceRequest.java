package alien4cloud.rest.service.model;

import org.alien4cloud.tosca.model.instances.NodeInstance;

import com.fasterxml.jackson.annotation.JsonInclude;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Getter;
import lombok.Setter;

/**
 * Update a Service Resource.
 */
@Getter
@Setter
@JsonInclude(JsonInclude.Include.NON_NULL)
@ApiModel(description = "Request to update a service resource.")
public class UpdateServiceResourceRequest {
    @ApiModelProperty(value = "The new name of the service or undefined if the update request should not update the service name.")
    private String name;
    @ApiModelProperty(value = "The new version of the service or undefined if the update request should not update the service version.")
    private String version;
    @ApiModelProperty(value = "The new description of the service or undefined if update request should not update the service description.")
    private String description;
    @ApiModelProperty(value = "The new node instance definition for the service or undefined if update request should not update the node instance definition.", notes = "In order to change the state (or another single attribute) it is better to use the advanced API for simpler usage and better performance. It is not authorized to change the type of the node if the version doesn't actually changes.")
    private NodeInstance nodeInstance;
    @ApiModelProperty(value = "The new list of location ids or undefined if update request should not update the service location ids.")
    private String[] locationIds;
}