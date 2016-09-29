package org.alien4cloud.tosca.model.templates;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class LocationPlacementPolicy extends AbstractPolicy {
    public static final String LOCATION_PLACEMENT_POLICY = "tosca.policies.Placement.Location";
    public static final String LOCATION_ID_PROPERTY = "locationId";
    private String locationId;

    /**
     * Initialize a location placement policy with the id of a location.
     *
     * @param locationId The id of the location.
     */
    public LocationPlacementPolicy(String locationId) {
        this.locationId = locationId;
    }

    @Override
    public String getType() {
        return LOCATION_PLACEMENT_POLICY;
    }

    @Override
    public void setType(String type) {
        // for json serialization
    }
}