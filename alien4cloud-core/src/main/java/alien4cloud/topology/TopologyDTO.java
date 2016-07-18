package alien4cloud.topology;

import java.util.List;
import java.util.Map;
import java.util.Set;

import alien4cloud.model.components.IndexedCapabilityType;
import alien4cloud.model.components.IndexedDataType;
import alien4cloud.model.components.IndexedNodeType;
import alien4cloud.model.components.IndexedRelationshipType;
import alien4cloud.model.topology.Topology;
import alien4cloud.utils.TreeNode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.alien4cloud.tosca.editor.operations.AbstractEditorOperation;

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

    public TopologyDTO(Topology topology, Map<String, IndexedNodeType> nodeTypes, Map<String, IndexedRelationshipType> relationshipTypes,
            Map<String, IndexedCapabilityType> capabilityTypes, Map<String, Map<String, Set<String>>> outputCapabilityProperties,
            Map<String, IndexedDataType> dataTypes) {
        super(topology, nodeTypes, relationshipTypes, capabilityTypes, dataTypes, outputCapabilityProperties);
    }
}