package alien4cloud.deployment.matching.services.location;

import java.util.List;
import java.util.Map.Entry;

import alien4cloud.model.deployment.matching.LocationMatch;
import alien4cloud.model.topology.NodeTemplate;
import alien4cloud.model.topology.Topology;

public class LocationMatchNodeFilter extends AbstractLocationMatchFilterWithElector {

    private LocationMatchNodesArtifactsElector artifactsElector = new LocationMatchNodesArtifactsElector();

    @Override
    public void filter(List<LocationMatch> toFilter, Topology topology) {
        artifactsElector.setDependencies(topology.getDependencies());
        filterOnSupportedArtifacts(toFilter, topology);
    }

    private void filterOnSupportedArtifacts(List<LocationMatch> toFilter, Topology topology) {
        LocationMatchNodesArtifactsElector artifactsElector = new LocationMatchNodesArtifactsElector();
        artifactsElector.setDependencies(topology.getDependencies());
        for (Entry<String, NodeTemplate> entry : topology.getNodeTemplates().entrySet()) {
            artifactsElector.setTemplate(entry.getValue());
            filterWith(toFilter, artifactsElector);
        }
    }

}
