package alien4cloud.topology;

import java.util.Map;
import java.util.Set;

import alien4cloud.model.components.IndexedCapabilityType;
import alien4cloud.model.components.IndexedDataType;
import alien4cloud.model.components.IndexedNodeType;
import alien4cloud.model.components.IndexedRelationshipType;
import alien4cloud.model.topology.Topology;

/**
 * Topology DTO contains the topology and a map of the types used in the topology.
 * 
 */
public class TopologyDTO extends AbstractTopologyDTO<Topology> {

    public TopologyDTO(Topology topology, Map<String, IndexedNodeType> nodeTypes, Map<String, IndexedRelationshipType> relationshipTypes,
            Map<String, IndexedCapabilityType> capabilityTypes, Map<String, Map<String, Set<String>>> outputCapabilityProperties,
            Map<String, IndexedDataType> dataTypes) {
        super(topology, nodeTypes, relationshipTypes, capabilityTypes, outputCapabilityProperties, dataTypes);
    }

    public TopologyDTO() {
    }
}