package alien4cloud.deployment.matching.services.location;

import java.util.List;

import alien4cloud.model.deployment.matching.LocationMatch;
import alien4cloud.model.topology.Topology;

public class LocationMatchAuthorizationFilter extends AbstractLocationMatchFilterWithElector {

    private LocationMatchAuthorizationElector authorizationElector = new LocationMatchAuthorizationElector();

    @Override
    public void filter(List<LocationMatch> toFilter, Topology topology) {
        filterWith(toFilter, authorizationElector);
    }

}
