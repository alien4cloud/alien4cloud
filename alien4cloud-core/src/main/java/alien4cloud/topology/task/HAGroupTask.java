package alien4cloud.topology.task;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import alien4cloud.paas.policies.AllocationErrorCode;

@Getter
@Setter
@NoArgsConstructor
@SuppressWarnings("PMD.UnusedPrivateField")
public class HAGroupTask extends TopologyTask {

    private AllocationErrorCode errorCode;

    private String groupId;

    public HAGroupTask(String nodeTemplateName, String groupId, AllocationErrorCode errorCode) {
        super(nodeTemplateName, null);
        this.setCode(TaskCode.HA_INVALID);
        this.groupId = groupId;
        this.errorCode = errorCode;
    }
}
