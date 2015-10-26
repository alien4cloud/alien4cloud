package alien4cloud.json.deserializer;

import alien4cloud.model.components.IndexedInheritableToscaElement;
import alien4cloud.model.components.IndexedNodeType;
import alien4cloud.model.components.IndexedRelationshipType;
import alien4cloud.topology.task.TopologyTask;

/**
 * Custom deserializer to handle multiple {@link IndexedInheritableToscaElement} types in {@link TopologyTask}.
 */
public class TaskIndexedInheritableToscaElementDeserializer extends AbstractDiscriminatorPolymorphicDeserializer<IndexedInheritableToscaElement> {

    public TaskIndexedInheritableToscaElementDeserializer() {
        super(IndexedInheritableToscaElement.class);
        addToRegistry("capabilities", IndexedNodeType.class);
        addToRegistry("validSources", IndexedRelationshipType.class);
    }
}