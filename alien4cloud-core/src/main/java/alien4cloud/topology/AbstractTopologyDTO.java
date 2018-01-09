package alien4cloud.topology;

import java.util.Map;

import org.alien4cloud.tosca.model.templates.Topology;
import org.alien4cloud.tosca.model.types.CapabilityType;
import org.alien4cloud.tosca.model.types.DataType;
import org.alien4cloud.tosca.model.types.NodeType;
import org.alien4cloud.tosca.model.types.PolicyType;
import org.alien4cloud.tosca.model.types.RelationshipType;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class AbstractTopologyDTO<T extends Topology> {
    private T topology;
    // elementId -> Type
    private Map<String, NodeType> nodeTypes;
    private Map<String, RelationshipType> relationshipTypes;
    private Map<String, CapabilityType> capabilityTypes;
    private Map<String, DataType> dataTypes;
    private Map<String, PolicyType> policyTypes;
}
