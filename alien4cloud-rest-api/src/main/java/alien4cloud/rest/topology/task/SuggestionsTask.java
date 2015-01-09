package alien4cloud.rest.topology.task;

import alien4cloud.model.components.IndexedNodeType;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuppressWarnings("PMD.UnusedPrivateField")
public class SuggestionsTask extends TopologyTask {
    // Array of suggested non abstract node types
    private IndexedNodeType[] suggestedNodeTypes;
}
