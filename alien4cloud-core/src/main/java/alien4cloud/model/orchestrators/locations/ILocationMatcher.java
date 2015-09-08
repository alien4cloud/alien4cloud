package alien4cloud.model.orchestrators.locations;

import java.util.List;

import alien4cloud.model.topology.Topology;
import alien4cloud.paas.exception.LocationMatchingException;

/**
 * Locations matcher beans. From a topology, gives a list of matchable locations, and eventually matching reasons.
 */
public interface ILocationMatcher {

    /**
     * Match a topology to one or several locations
     *
     * @param topology
     * @return List od {@link LocationMatch} representing the locations on which the topology can be deployed
     * @throws LocationMatchingException
     */
    List<LocationMatch> match(Topology topology) throws LocationMatchingException;
}
