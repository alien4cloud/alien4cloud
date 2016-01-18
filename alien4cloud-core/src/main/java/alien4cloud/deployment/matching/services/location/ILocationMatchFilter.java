package alien4cloud.deployment.matching.services.location;

import java.util.List;

import alien4cloud.model.deployment.matching.ILocationMatch;
import alien4cloud.model.topology.Topology;

public interface ILocationMatchFilter {
    void filter(List<ILocationMatch> toFilter, Topology topology);
}
