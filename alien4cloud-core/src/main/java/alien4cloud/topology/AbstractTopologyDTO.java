package alien4cloud.topology;

import java.util.Map;
import java.util.Set;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import alien4cloud.model.components.IndexedCapabilityType;
import alien4cloud.model.components.IndexedDataType;
import alien4cloud.model.components.IndexedNodeType;
import alien4cloud.model.components.IndexedRelationshipType;
import alien4cloud.model.topology.Topology;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor(suppressConstructorProperties = true)
public class AbstractTopologyDTO<T extends Topology> {
    private T topology;
    private Map<String, IndexedNodeType> nodeTypes;
    private Map<String, IndexedRelationshipType> relationshipTypes;
    private Map<String, IndexedCapabilityType> capabilityTypes;
    private Map<String, IndexedDataType> dataTypes;

    // FIXME this is already in the topology, let's just remove that from the DTO as it create heavier and useless json
    private Map<String, Map<String, Set<String>>> outputCapabilityProperties;
}
