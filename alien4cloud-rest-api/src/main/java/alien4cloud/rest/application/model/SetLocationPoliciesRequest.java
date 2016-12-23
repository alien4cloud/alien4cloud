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
    @ApiModelProperty(value = "Id of the Orchestratrator managing the locations on which we want to deploy.", required = true, dataType = "string")
    private String orchestratorId;
    /** map of matching between groups and locations: key = groupeName, value = locationId */
    @ApiModelProperty(value = "Locations settings for groups. key = groupeName, value = locationId. Note that for now, the only groupe name valid is "
            + AlienConstants.GROUP_ALL + ", as we do not yet support multiple locations policies settings.", required = true)
    private Map<String, String> groupsToLocations;
}
