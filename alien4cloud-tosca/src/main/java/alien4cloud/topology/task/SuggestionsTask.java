package alien4cloud.topology.task;

import org.alien4cloud.tosca.model.types.NodeType;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor(suppressConstructorProperties = true)
public class SuggestionsTask extends TopologyTask {
    // Array of suggested non abstract node types
    private NodeType[] suggestedNodeTypes;
}
