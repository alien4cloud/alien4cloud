package alien4cloud.deployment.matching.services.location;

import java.util.List;

import alien4cloud.model.deployment.matching.ILocationMatch;
import org.alien4cloud.tosca.model.templates.Topology;

public class LocationMatchAuthorizationFilter extends AbstractLocationMatchFilterWithElector {

    private LocationMatchAuthorizationElector authorizationElector = new LocationMatchAuthorizationElector();

    @Override
    public void filter(List<ILocationMatch> toFilter, Topology topology) {
        filterWith(toFilter, authorizationElector);
    }

}
