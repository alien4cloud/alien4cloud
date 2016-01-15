package alien4cloud.deployment;

import java.util.Map;

import javax.inject.Inject;

import org.springframework.stereotype.Service;

import alien4cloud.model.deployment.Deployment;
import alien4cloud.model.deployment.DeploymentTopology;
import alien4cloud.model.orchestrators.locations.Location;
import alien4cloud.paas.model.PaaSTopology;
import alien4cloud.paas.model.PaaSTopologyDeploymentContext;
import alien4cloud.paas.plan.TopologyTreeBuilderService;
import alien4cloud.utils.TypeMap;

/**
 * Utility to build the deployment context.
 */
@Service
public class DeploymentContextService {
    @Inject
    private TopologyTreeBuilderService topologyTreeBuilderService;

    /**
     * Build a topology deployment context from a given topology and deployment.
     *
     * @param deployment The deployment object.
     * @param topology The topology that will be processed.
     * @return A PaaSTopologyDeploymentContext that contians
     */
    public PaaSTopologyDeploymentContext buildTopologyDeploymentContext(Deployment deployment, Map<String, Location> locations, DeploymentTopology topology) {
        return buildTopologyDeploymentContext(deployment, locations, topology, topologyTreeBuilderService.buildPaaSTopology(topology));
    }

    /**
     * Build a topology deployment context from a given topology and deployment and with a type cache.
     *
     * @param deployment The deployment object.
     * @param topology The topology that will be processed.
     * @param cache type cache
     * @return A PaaSTopologyDeploymentContext that contians
     */
    public PaaSTopologyDeploymentContext buildTopologyDeploymentContext(Deployment deployment, Map<String, Location> locations, DeploymentTopology topology,
            TypeMap cache) {
        return buildTopologyDeploymentContext(deployment, locations, topology, topologyTreeBuilderService.buildPaaSTopology(topology, cache));
    }

    private PaaSTopologyDeploymentContext buildTopologyDeploymentContext(Deployment deployment, Map<String, Location> locations, DeploymentTopology topology,
            PaaSTopology paaSTopology) {
        PaaSTopologyDeploymentContext topologyDeploymentContext = new PaaSTopologyDeploymentContext();
        topologyDeploymentContext.setLocations(locations);
        topologyDeploymentContext.setDeployment(deployment);
        topologyDeploymentContext.setPaaSTopology(paaSTopology);
        topologyDeploymentContext.setDeploymentTopology(topology);
        topologyDeploymentContext.setDeployment(deployment);
        return topologyDeploymentContext;
    }
}