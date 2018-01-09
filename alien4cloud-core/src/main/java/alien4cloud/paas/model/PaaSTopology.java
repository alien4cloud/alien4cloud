package alien4cloud.paas.model;

import java.util.List;
import java.util.Map;

import org.alien4cloud.tosca.model.types.CapabilityType;
import org.alien4cloud.tosca.model.types.DataType;
import org.alien4cloud.tosca.model.types.NodeType;
import org.alien4cloud.tosca.model.types.RelationshipType;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * The result of parsing of an Alien topology
 * 
 * @author Minh Khang VU
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class PaaSTopology {
    private List<PaaSNodeTemplate> computes;
    private List<PaaSNodeTemplate> networks;
    private List<PaaSNodeTemplate> volumes;
    private List<PaaSNodeTemplate> nonNatives;
    private Map<String, PaaSNodeTemplate> allNodes;
    private Map<String, List<PaaSNodeTemplate>> groups;

    private Map<String, NodeType> nodeTypes;
    private Map<String, RelationshipType> relationshipTypes;
    private Map<String, CapabilityType> capabilityTypes;
    private Map<String, DataType> dataTypes;

    public PaaSTopology(List<PaaSNodeTemplate> computes, List<PaaSNodeTemplate> networks, List<PaaSNodeTemplate> volumes, List<PaaSNodeTemplate> nonNatives,
            Map<String, PaaSNodeTemplate> allNodes, Map<String, List<PaaSNodeTemplate>> groups) {
        this.computes = computes;
        this.networks = networks;
        this.volumes = volumes;
        this.nonNatives = nonNatives;
        this.allNodes = allNodes;
        this.groups = groups;
    }
}
