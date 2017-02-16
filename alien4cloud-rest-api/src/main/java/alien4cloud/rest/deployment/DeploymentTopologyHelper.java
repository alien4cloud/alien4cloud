package alien4cloud.rest.deployment;

import java.util.Map;

import javax.inject.Inject;

import alien4cloud.model.service.ServiceResource;
import alien4cloud.service.ServiceResourceService;
import org.alien4cloud.tosca.topology.TopologyDTOBuilder;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

import alien4cloud.deployment.DeploymentTopologyValidationService;
import alien4cloud.deployment.matching.services.location.TopologyLocationUtils;
import alien4cloud.deployment.model.DeploymentConfiguration;
import alien4cloud.model.deployment.DeploymentTopology;
import alien4cloud.model.orchestrators.locations.LocationResourceTemplate;
import alien4cloud.orchestrators.locations.services.ILocationResourceService;
import alien4cloud.topology.TopologyDTO;
import alien4cloud.utils.ReflectionUtil;

@Component
public class DeploymentTopologyHelper implements IDeploymentTopologyHelper {
    @Inject
    @Lazy(true)
    private ILocationResourceService locationResourceService;
    @Inject
    private TopologyDTOBuilder topologyDTOBuilder;
    @Inject
    private DeploymentTopologyValidationService deploymentTopologyValidationService;
    @Inject
    private ServiceResourceService serviceResourceService;

    @Override
    public DeploymentTopologyDTO buildDeploymentTopologyDTO(DeploymentConfiguration deploymentConfiguration) {
        DeploymentTopology deploymentTopology = deploymentConfiguration.getDeploymentTopology();
        TopologyDTO topologyDTO = topologyDTOBuilder.buildTopologyDTO(deploymentTopology);
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