package alien4cloud.rest.deployment;

import java.util.Map;
import java.util.Set;

import lombok.Getter;
import lombok.Setter;
import alien4cloud.model.components.IndexedCapabilityType;
import alien4cloud.model.components.IndexedNodeType;
import alien4cloud.model.components.IndexedRelationshipType;
import alien4cloud.model.deployment.DeploymentTopology;
import alien4cloud.topology.AbstractTopologyDTO;

import com.google.common.collect.Maps;

@Getter
@Setter
public class DeploymentTopologyDTO extends AbstractTopologyDTO<DeploymentTopology> {
    /** groupeName --> locationId */
    private Map<String, String> locationPolicies = Maps.newHashMap();

    public DeploymentTopologyDTO(DeploymentTopology topology, Map<String, IndexedNodeType> nodeTypes, Map<String, IndexedRelationshipType> relationshipTypes,
            Map<String, IndexedCapabilityType> capabilityTypes, Map<String, Map<String, Set<String>>> outputCapabilityProperties, String yaml) {
        super(topology, nodeTypes, relationshipTypes, capabilityTypes, outputCapabilityProperties, yaml);
    }

    public DeploymentTopologyDTO() {
    }
}
