package alien4cloud.model.orchestrators.locations;

import java.util.List;

import alien4cloud.model.topology.Topology;

public interface ILocationMatchFilter {
    void filter(List<LocationMatch> toFilter, Topology topology);
}
