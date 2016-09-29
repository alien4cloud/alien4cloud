package alien4cloud.topology;

import java.util.List;
import java.util.Map;
import java.util.Set;

import org.alien4cloud.tosca.editor.operations.AbstractEditorOperation;
import org.alien4cloud.tosca.model.templates.Topology;
import org.alien4cloud.tosca.model.types.CapabilityType;
import org.alien4cloud.tosca.model.types.DataType;
import org.alien4cloud.tosca.model.types.NodeType;
import org.alien4cloud.tosca.model.types.RelationshipType;

import alien4cloud.utils.TreeNode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Topology DTO contains the topology and a map of the types used in the topology.
 * 
 */
@Getter
@Setter
@NoArgsConstructor
public class TopologyDTO extends AbstractTopologyDTO<Topology> {
    private TreeNode archiveContentTree;
    private int lastOperationIndex;
    private List<AbstractEditorOperation> operations;
    private String delegateType;

    public TopologyDTO(Topology topology, Map<String, NodeType> nodeTypes, Map<String, RelationshipType> relationshipTypes,
            Map<String, CapabilityType> capabilityTypes, Map<String, Map<String, Set<String>>> outputCapabilityProperties, Map<String, DataType> dataTypes) {
        super(topology, nodeTypes, relationshipTypes, capabilityTypes, dataTypes, outputCapabilityProperties);
    }
}