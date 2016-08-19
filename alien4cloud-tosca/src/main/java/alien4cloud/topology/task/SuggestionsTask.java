package alien4cloud.topology.task;

import alien4cloud.model.components.IndexedNodeType;
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
    private IndexedNodeType[] suggestedNodeTypes;
}
