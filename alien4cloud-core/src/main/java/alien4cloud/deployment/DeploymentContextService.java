package alien4cloud.deployment;

import alien4cloud.deployment.model.SecretProviderConfigurationAndCredentials;
import alien4cloud.model.deployment.Deployment;
import alien4cloud.model.deployment.DeploymentTopology;
import alien4cloud.model.orchestrators.locations.Location;
import alien4cloud.paas.model.PaaSTopology;
import alien4cloud.paas.model.PaaSTopologyDeploymentContext;
import alien4cloud.paas.plan.TopologyTreeBuilderService;
import org.springframework.stereotype.Service;

import javax.inject.Inject;
import java.util.Map;

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
     * @return A PaaSTopologyDeploymentContext matching the input topology.
     */
    public PaaSTopologyDeploymentContext buildTopologyDeploymentContext(SecretProviderConfigurationAndCredentials secretProviderConfigurationAndCredentials, Deployment deployment, Map<String, Location> locations, DeploymentTopology topology) {
        PaaSTopology paaSTopology = topologyTreeBuilderService.buildPaaSTopology(topology);
        PaaSTopologyDeploymentContext topologyDeploymentContext = new PaaSTopologyDeploymentContext();
        topologyDeploymentContext.setLocations(locations);
        topologyDeploymentContext.setDeployment(deployment);
        topologyDeploymentContext.setPaaSTopology(paaSTopology);
        topologyDeploymentContext.setDeploymentTopology(topology);
        topologyDeploymentContext.setDeployment(deployment);
        topologyDeploymentContext.setSecretProviderConfigurationAndCredentials(secretProviderConfigurationAndCredentials);
        return topologyDeploymentContext;
    }
}