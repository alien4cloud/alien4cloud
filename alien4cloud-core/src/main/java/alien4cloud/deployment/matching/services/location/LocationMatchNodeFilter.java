package alien4cloud.deployment.matching.services.location;

import java.util.List;
import java.util.Map.Entry;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;

import org.springframework.stereotype.Component;

import alien4cloud.component.CSARRepositorySearchService;
import alien4cloud.model.deployment.matching.LocationMatch;
import alien4cloud.model.topology.NodeTemplate;
import alien4cloud.model.topology.Topology;
import alien4cloud.orchestrators.services.OrchestratorService;

@Component
public class LocationMatchNodeFilter extends AbstractLocationMatchFilterWithElector {
    @Resource
    private CSARRepositorySearchService csarSearchService;
    @Resource
    private OrchestratorService orchestratorService;

    private LocationMatchNodesArtifactsElector artifactsElector;

    @PostConstruct
    private void postConstruct() {
        artifactsElector = new LocationMatchNodesArtifactsElector(csarSearchService, orchestratorService);
    }

    @Override
    public void filter(List<LocationMatch> toFilter, Topology topology) {
        artifactsElector.setDependencies(topology.getDependencies());
        for (Entry<String, NodeTemplate> entry : topology.getNodeTemplates().entrySet()) {
            artifactsElector.setTemplate(entry.getValue());
            filterWith(toFilter, artifactsElector);
        }
    }

}
