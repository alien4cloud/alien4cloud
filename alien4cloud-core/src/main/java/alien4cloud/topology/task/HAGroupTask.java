package alien4cloud.topology.task;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import alien4cloud.model.components.IndexedInheritableToscaElement;
import alien4cloud.paas.ha.AllocationErrorCode;

@Getter
@Setter
@NoArgsConstructor
@SuppressWarnings("PMD.UnusedPrivateField")
public class HAGroupTask extends TopologyTask {

    private AllocationErrorCode errorCode;

    private String groupId;

    public HAGroupTask(TaskCode code, String nodeTemplateName, IndexedInheritableToscaElement component, String groupId, AllocationErrorCode errorCode) {
        super(code, nodeTemplateName, component);
        this.groupId = groupId;
        this.errorCode = errorCode;
    }
}
