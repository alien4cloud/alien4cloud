package alien4cloud.rest.deployment;

import java.util.Map;

import lombok.Getter;
import lombok.Setter;
import alien4cloud.model.deployment.DeploymentTopology;

import com.google.common.collect.Maps;

@Getter
@Setter
public class DeploymentTopologyDTO {
    private DeploymentTopology deploymentTopology;
    /** groupeName --> locationId */
    private Map<String, String> locationPolicies = Maps.newHashMap();
}
