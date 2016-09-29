package alien4cloud.json.deserializer;

import org.alien4cloud.tosca.model.types.AbstractInheritableToscaType;
import org.alien4cloud.tosca.model.types.NodeType;
import org.alien4cloud.tosca.model.types.RelationshipType;
import alien4cloud.topology.task.TopologyTask;

/**
 * Custom deserializer to handle multiple {@link AbstractInheritableToscaType} types in {@link TopologyTask}.
 */
public class TaskIndexedInheritableToscaElementDeserializer extends AbstractDiscriminatorPolymorphicDeserializer<AbstractInheritableToscaType> {

    public TaskIndexedInheritableToscaElementDeserializer() {
        super(AbstractInheritableToscaType.class);
        addToRegistry("capabilities", NodeType.class);
        addToRegistry("validSources", RelationshipType.class);
    }
}