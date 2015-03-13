package alien4cloud.rest.topology;

import java.util.Map;
import java.util.Set;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import alien4cloud.model.components.IndexedCapabilityType;
import alien4cloud.model.components.IndexedNodeType;
import alien4cloud.model.components.IndexedRelationshipType;
import alien4cloud.model.topology.Topology;

/**
 * Topology DTO contains the topology and a map of the types used in the topology.
 * 
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuppressWarnings("PMD.UnusedPrivateField")
public class TopologyDTO {
    private Topology topology;
    private Map<String, IndexedNodeType> nodeTypes;
    private Map<String, IndexedRelationshipType> relationshipTypes;
    private Map<String, IndexedCapabilityType> capabilityTypes;
    private Map<String, Map<String, Set<String>>> outputCapabilityProperties;
}