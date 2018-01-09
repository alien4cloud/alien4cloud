package alien4cloud.topology;

import java.util.List;

import org.apache.commons.collections4.CollectionUtils;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.google.common.collect.Lists;

import alien4cloud.json.deserializer.TaskDeserializer;
import alien4cloud.topology.task.AbstractTask;
import alien4cloud.utils.jackson.ConditionalAttributes;
import alien4cloud.utils.jackson.ConditionalOnAttribute;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

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
public class TopologyValidationResult {

    private boolean isValid;

    @ConditionalOnAttribute(ConditionalAttributes.REST)
    @JsonDeserialize(contentUsing = TaskDeserializer.class)
    private List<AbstractTask> taskList;

    @ConditionalOnAttribute(ConditionalAttributes.REST)
    @JsonDeserialize(contentUsing = TaskDeserializer.class)
    private List<AbstractTask> warningList;

    @ConditionalOnAttribute(ConditionalAttributes.REST)
    @JsonDeserialize(contentUsing = TaskDeserializer.class)
    private List<AbstractTask> infoList;

    public <T extends AbstractTask> void addTasks(List<T> tasks) {
        if (CollectionUtils.isEmpty(tasks)) {
            return;
        }
        if (taskList == null) {
            taskList = Lists.newArrayList();
        }
        taskList.addAll(tasks);
    }

    public <T extends AbstractTask> void addTask(T task) {
        if (task == null) {
            return;
        }
        if (taskList == null) {
            taskList = Lists.newArrayList();
        }
        taskList.add(task);
    }

    public <T extends AbstractTask> void addWarnings(List<T> warnings) {
        if (CollectionUtils.isEmpty(warnings)) {
            return;
        }
        if (warningList == null) {
            warningList = Lists.newArrayList();
        }
        warningList.addAll(warnings);
    }

    public <T extends AbstractTask> void addWarning(T warning) {
        if (warning == null) {
            return;
        }
        if (warningList == null) {
            warningList = Lists.newArrayList();
        }
        warningList.add(warning);
    }

    public <T extends AbstractTask> void addInfo(T info) {
        if (info == null) {
            return;
        }
        if (infoList == null) {
            infoList = Lists.newArrayList();
        }
        infoList.add(info);
    }
}