package alien4cloud.model.orchestrators.locations;

import java.util.List;

import alien4cloud.model.topology.Topology;

public class LocationMatchAuthorizationFilter extends AbstractLocationMatchFilterWithElector {

    private LocationMatchAuthorizationElector authorizationElector = new LocationMatchAuthorizationElector();

    @Override
    public void filter(List<LocationMatch> toFilter, Topology topology) {
        filterWith(toFilter, authorizationElector);
    }

}
