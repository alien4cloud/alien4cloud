package alien4cloud.topology;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import alien4cloud.topology.task.TopologyTask;

import com.google.common.collect.Lists;

/**
 * Validation result that contains a boolean determining if a topology is valid for deployment.
 * If not, contains also a list of tasks of components to implement .
 * 
 * @author igor ngouagna
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuppressWarnings("PMD.UnusedPrivateField")
public class TopologyValidationResult {

    private boolean isValid;

    private List<TopologyTask> taskList;

    private List<TopologyTask> warningList;

    public <T extends TopologyTask> void addToTaskList(List<T> tasks) {
        if (tasks == null) {
            return;
        }
        if (taskList == null) {
            taskList = Lists.newArrayList();
        }
        taskList.addAll(tasks);
    }

    public <T extends TopologyTask> void addToWarningList(List<T> warnings) {
        if (warnings == null) {
            return;
        }
        if (warningList == null) {
            warningList = Lists.newArrayList();
        }
        warningList.addAll(warnings);
    }
}