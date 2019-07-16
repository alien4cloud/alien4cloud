package alien4cloud.topology.task;

import lombok.Getter;
import lombok.Setter;

/**
 * Task generated to express that some nodes lacks location selection.
 */
@Getter
@Setter
public class LocationPolicyTask extends AbstractTask {
    // Name of the node template that needs to be fixed.
    private String groupName;

    public LocationPolicyTask() {
        this.groupName = "";
        this.setCode(TaskCode.LOCATION_POLICY);
    }

    public LocationPolicyTask(String groupName) {
        this.groupName = groupName;
        this.setCode(TaskCode.LOCATION_POLICY);
    }
}
