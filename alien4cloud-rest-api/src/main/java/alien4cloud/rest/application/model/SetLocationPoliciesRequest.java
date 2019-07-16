package alien4cloud.rest.application.model;

import java.util.Map;

import alien4cloud.utils.AlienConstants;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@ApiModel(description = "Request to set locations policies for a deployment.")
public class SetLocationPoliciesRequest {
    @ApiModelProperty(value = "Id of the Orchestrator managing the locations on which we want to deploy.", required = true, dataType = "string")
    private String orchestratorId;
    /** map of matching between groups and locations: key = groupeName, value = locationId */
    @ApiModelProperty(value = "Locations settings for groups. key = groupeName, value = locationId.", required = true)
    private Map<String, String> groupsToLocations;
}
