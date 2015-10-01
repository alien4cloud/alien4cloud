package alien4cloud.topology;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import org.apache.commons.collections4.CollectionUtils;

import alien4cloud.topology.task.AbstractTask;

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

    private List<AbstractTask> taskList;

    private List<AbstractTask> warningList;

    public <T extends AbstractTask> void addToTaskList(List<T> tasks) {
        if (CollectionUtils.isEmpty(tasks)) {
            return;
        }
        if (taskList == null) {
            taskList = Lists.newArrayList();
        }
        taskList.addAll(tasks);
    }

    public <T extends AbstractTask> void addToWarningList(List<T> warnings) {
        if (CollectionUtils.isEmpty(warnings)) {
            return;
        }
        if (warningList == null) {
            warningList = Lists.newArrayList();
        }
        warningList.addAll(warnings);
    }
}