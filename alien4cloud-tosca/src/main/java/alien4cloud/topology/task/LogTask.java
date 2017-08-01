package alien4cloud.topology.task;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Log task is created just to dispatch a message to the UI.
 * The message will go through the translate filter so you can also provide a translation code.
 */
@Getter
@Setter
@NoArgsConstructor
public class LogTask extends AbstractTask {
    private String message;

    public LogTask(String message) {
        setCode(TaskCode.LOG);
        this.message = message;
    }

    public LogTask(TaskCode code, String message) {
        setCode(code);
        this.message = message;
    }
}
