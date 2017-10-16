package org.alien4cloud.alm.deployment.configuration.flow;

import java.util.List;

import org.slf4j.helpers.FormattingTuple;
import org.slf4j.helpers.MessageFormatter;

import com.google.common.collect.Lists;

import alien4cloud.topology.task.AbstractTask;
import alien4cloud.topology.task.LogTask;
import alien4cloud.topology.task.TaskCode;
import lombok.Getter;
import lombok.Setter;

/**
 * This object is given to the topology modifiers of the flow so they can notify user of infos, warnings or errors in the execution flow.
 *
 * Errors, warning and info may come from validations or from any internal processing logic.
 */
@Getter
@Setter
public class FlowExecutionLog {
    private List<AbstractTask> infos = Lists.newArrayList();
    private List<AbstractTask> warnings = Lists.newArrayList();
    private List<AbstractTask> errors = Lists.newArrayList();

    public boolean isValid() {
        return errors.isEmpty();
    }

    public void info(AbstractTask task) {
        infos.add(task);
    }

    public void info(String message) {
        infos.add(new LogTask(message));
    }

    public void info(String message, Object... args) {
        FormattingTuple ft = MessageFormatter.arrayFormat(message, args);
        infos.add(new LogTask(ft.getMessage()));
    }

    public void warn(AbstractTask task) {
        warnings.add(task);
    }

    public void warn(String message) {
        warnings.add(new LogTask(message));
    }

    public void warn(String message, Object... args) {
        FormattingTuple ft = MessageFormatter.arrayFormat(message, args);
        warnings.add(new LogTask(ft.getMessage()));
    }

    public void error(AbstractTask task) {
        errors.add(task);
    }

    public void error(String message) {
        errors.add(new LogTask(message));
    }

    public void error(String message, Object... args) {
        FormattingTuple ft = MessageFormatter.arrayFormat(message, args);
        errors.add(new LogTask(ft.getMessage()));
    }

    public void error(TaskCode taskCode, String message) {
        errors.add(new LogTask(taskCode, message));
    }
}