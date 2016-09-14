package alien4cloud.topology;

import java.util.Map;
import java.util.Set;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.alien4cloud.tosca.model.types.CapabilityType;
import org.alien4cloud.tosca.model.types.DataType;
import org.alien4cloud.tosca.model.types.NodeType;
import org.alien4cloud.tosca.model.types.RelationshipType;
import org.alien4cloud.tosca.model.templates.Topology;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor(suppressConstructorProperties = true)
public class AbstractTopologyDTO<T extends Topology> {
    private T topology;
    private Map<String, NodeType> nodeTypes;
    private Map<String, RelationshipType> relationshipTypes;
    private Map<String, CapabilityType> capabilityTypes;
    private Map<String, DataType> dataTypes;

    // FIXME this is already in the topology, let's just remove that from the DTO as it create heavier and useless json
    private Map<String, Map<String, Set<String>>> outputCapabilityProperties;
}
