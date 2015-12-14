package alien4cloud.rest.application.model;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

import java.util.Map;

import lombok.Getter;
import lombok.Setter;
import alien4cloud.common.AlienConstants;

@Getter
@Setter
@ApiModel(description = "Request to set locations policies fro a deployment.")
public class SetLocationPoliciesRequest {
    @ApiModelProperty(value = "Id of the Orchestratrator managing the locations on which we want to deploy.", required = true, dataType = "string")
    private String orchestratorId;
    /** map of matching between groups and locations: key = groupeName, value = locationId */
    @ApiModelProperty(value = "Locations settings for groups. key = groupeName, value = locationId. Note that for now, the only groupe name valid is "
            + AlienConstants.GROUP_ALL + ", as we do not yet support multiple locations policies settings.", required = true)
    private Map<String, String> groupsToLocations;
}
