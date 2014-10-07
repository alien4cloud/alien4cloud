package alien4cloud.rest.topology;

import java.util.List;

import alien4cloud.rest.topology.task.TopologyTask;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * DTO that contains a boolean determining if a topology is deployable. If not, contains also a list of tasks of components to implement .
 * 
 * @author igor ngouagna
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuppressWarnings("PMD.UnusedPrivateField")
public class ValidTopologyDTO {
    private boolean isValid;
    private List<TopologyTask> taskList;

    public <T extends TopologyTask> void addToTaskList(List<T> taskListToAdd) {
        if (taskListToAdd == null) {
            return;
        }
        // nodeTypesTaskList = ArrayUtils.addAll(nodeTypesTaskList, taskListToAdd.toArray(new TopologyTask[taskListToAdd.size()]));
        if (taskList == null) {
            taskList = (List<TopologyTask>) taskListToAdd;
        } else {
            taskList.addAll(taskListToAdd);
        }

    }
}