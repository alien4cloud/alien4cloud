package alien4cloud.rest.deployment;

import java.util.Map;
import java.util.Set;

import com.google.common.collect.Maps;

import alien4cloud.deployment.model.DeploymentSubstitutionConfiguration;
import alien4cloud.model.components.IndexedCapabilityType;
import alien4cloud.model.components.IndexedDataType;
import alien4cloud.model.components.IndexedNodeType;
import alien4cloud.model.components.IndexedRelationshipType;
import alien4cloud.model.deployment.DeploymentTopology;
import alien4cloud.model.orchestrators.locations.LocationResourceTemplate;
import alien4cloud.topology.AbstractTopologyDTO;
import alien4cloud.topology.TopologyValidationResult;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class DeploymentTopologyDTO extends AbstractTopologyDTO<DeploymentTopology> {
    /**
     * groupName --> locationId
     */
    private Map<String, String> locationPolicies = Maps.newHashMap();

    /**
     * validation result of the deployment topology
     */
    private TopologyValidationResult validation;

    /**
     * template id --> location resource template
     **/
    private Map<String, LocationResourceTemplate> locationResourceTemplates;

    /**
     * Information about which node can be substituted by which orchestrator's location's resource
     */
    private DeploymentSubstitutionConfiguration availableSubstitutions;

    public DeploymentTopologyDTO(DeploymentTopology topology, Map<String, IndexedNodeType> nodeTypes, Map<String, IndexedRelationshipType> relationshipTypes,
            Map<String, IndexedCapabilityType> capabilityTypes, Map<String, Map<String, Set<String>>> outputCapabilityProperties,
            Map<String, IndexedDataType> dataTypes) {
        super(topology, nodeTypes, relationshipTypes, capabilityTypes, dataTypes, outputCapabilityProperties);
    }

    public DeploymentTopologyDTO() {
    }
}
