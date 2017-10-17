package alien4cloud.rest.deployment;

import java.util.Map;

import com.google.common.collect.Maps;

import alien4cloud.deployment.model.DeploymentSubstitutionConfiguration;
import alien4cloud.model.deployment.DeploymentTopology;
import alien4cloud.model.orchestrators.locations.LocationResourceTemplate;
import alien4cloud.model.orchestrators.locations.PolicyLocationResourceTemplate;
import alien4cloud.topology.AbstractTopologyDTO;
import alien4cloud.topology.TopologyValidationResult;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class DeploymentTopologyDTO extends AbstractTopologyDTO<DeploymentTopology> {
    /** groupName --> locationId. */
    private Map<String, String> locationPolicies = Maps.newHashMap();

    /** validation result of the deployment topology. */
    private TopologyValidationResult validation;

    /** node template id --> location resource template. **/
    private Map<String, LocationResourceTemplate> locationResourceTemplates;

    /** policy template id --> policy location resource template. **/
    private Map<String, PolicyLocationResourceTemplate> policyLocationResourceTemplates;

    /** Information about which node can be substituted by which orchestrator's location's resource. */
    private DeploymentSubstitutionConfiguration availableSubstitutions;

    public DeploymentTopologyDTO() {
    }
}
