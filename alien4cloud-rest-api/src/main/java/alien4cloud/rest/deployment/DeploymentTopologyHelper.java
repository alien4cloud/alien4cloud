package alien4cloud.rest.deployment;

import java.util.Map;

import javax.inject.Inject;

import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

import alien4cloud.deployment.DeploymentTopologyValidationService;
import alien4cloud.deployment.matching.services.location.TopologyLocationUtils;
import alien4cloud.deployment.model.DeploymentConfiguration;
import alien4cloud.model.deployment.DeploymentTopology;
import alien4cloud.model.orchestrators.locations.LocationResourceTemplate;
import alien4cloud.orchestrators.locations.services.ILocationResourceService;
import alien4cloud.topology.TopologyDTO;
import alien4cloud.topology.TopologyService;
import alien4cloud.utils.ReflectionUtil;

@Component
public class DeploymentTopologyHelper implements IDeploymentTopologyHelper {

    @Inject
    @Lazy(true)
    private ILocationResourceService locationResourceService;
    @Inject
    private TopologyService topologyService;
    @Inject
    private DeploymentTopologyValidationService deploymentTopologyValidationService;

    @Override
    public DeploymentTopologyDTO buildDeploymentTopologyDTO(DeploymentConfiguration deploymentConfiguration) {
        DeploymentTopology deploymentTopology = deploymentConfiguration.getDeploymentTopology();
        TopologyDTO topologyDTO = topologyService.buildTopologyDTO(deploymentTopology);
        DeploymentTopologyDTO deploymentTopologyDTO = new DeploymentTopologyDTO();
        ReflectionUtil.mergeObject(topologyDTO, deploymentTopologyDTO);
        Map<String, String> locationIds = TopologyLocationUtils.getLocationIds(deploymentTopology);
        for (Map.Entry<String, String> locationIdsEntry : locationIds.entrySet()) {
            deploymentTopologyDTO.getLocationPolicies().put(locationIdsEntry.getKey(), locationIdsEntry.getValue());
        }
        deploymentTopologyDTO.setAvailableSubstitutions(deploymentConfiguration.getAvailableSubstitutions());
        deploymentTopologyDTO.setValidation(deploymentTopologyValidationService.validateDeploymentTopology(deploymentTopology));
        Map<String, LocationResourceTemplate> templates = locationResourceService.getMultiple(deploymentTopology.getSubstitutedNodes().values());
        // locationResourceService.setLocationResourcesPortabilityDefinition(templates.values(), true, topologyDTO.getTopology().getDependencies());
        deploymentTopologyDTO.setLocationResourceTemplates(templates);
        return deploymentTopologyDTO;
    }

}
