package alien4cloud.rest.wizard.model;

import lombok.Getter;
import lombok.Setter;
import org.alien4cloud.tosca.model.types.NodeType;

@Getter
@Setter
public class TopologyGraphNode {
    private String id;
    private String label;
    private NodeType nodeType;
}
