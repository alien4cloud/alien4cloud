package alien4cloud.paas;

import java.util.List;
import java.util.Map;

import javax.annotation.Resource;

import lombok.Getter;
import alien4cloud.component.repository.CsarFileRepository;
import alien4cloud.model.application.DeploymentSetup;
import alien4cloud.paas.model.PaaSNodeTemplate;
import alien4cloud.paas.plan.TopologyTreeBuilderService;
import alien4cloud.tosca.container.model.topology.Topology;
import alien4cloud.tosca.container.services.csar.impl.CSARRepositorySearchService;

@Getter
public abstract class AbstractPaaSProvider implements IPaaSProvider {
    @Resource
    private CSARRepositorySearchService csarRepoSearch;
    @Resource
    private CsarFileRepository repository;
    @Resource
    private TopologyTreeBuilderService topologyTreeBuilderService;

    @Override
    public void deploy(String deploymentName, String deploymentId, Topology topology, DeploymentSetup deploymentSetup) {
        Map<String, PaaSNodeTemplate> nodeTemplates = topologyTreeBuilderService.buildPaaSNodeTemplate(topology);
        List<PaaSNodeTemplate> roots = topologyTreeBuilderService.getHostedOnTree(nodeTemplates);
        // Create the build plan
        doDeploy(deploymentName, deploymentId, topology, roots, nodeTemplates, deploymentSetup);
    }

    /**
     * Actually deploy a topology.
     *
     * @param deploymentName Human readable name of the deployment.
     * @param deploymentId Id of the topology under deployment.
     * @param topology The topology under deployment.
     * @param roots Root node templates for the hierarchy.
     * @param nodeTemplates Map of all the node templates in the topology with all required informations pre-linked.
     * @param deploymentSetup Deployment setup for the current deployment
     */
    protected abstract void doDeploy(String deploymentName, String deploymentId, Topology topology, List<PaaSNodeTemplate> roots,
            Map<String, PaaSNodeTemplate> nodeTemplates, DeploymentSetup deploymentSetup);
}
