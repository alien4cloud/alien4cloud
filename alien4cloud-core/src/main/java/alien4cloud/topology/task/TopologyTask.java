package alien4cloud.topology.task;

import alien4cloud.model.components.IndexedInheritableToscaElement;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * 
 * Represent one task to do to have a deployable topology
 * 
 * @author 'Igor Ngouagna'
 * 
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuppressWarnings("PMD.UnusedPrivateField")
public class TopologyTask {
    // task code
    private TaskCode code;
    // Name of the node template that needs to be fixed.
    private String nodeTemplateName;
    // related component
    private IndexedInheritableToscaElement component;
}
