package alien4cloud.rest.application.model;

import java.util.Map;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class SetLocationPoliciesRequest {
    private String environmentId;
    /** map of matching between groups and locations: key = groupeName, value = locationId */
    private Map<String, String> groupsToLocations;
}
