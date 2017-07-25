package alien4cloud.topology.task;

/**
 * Log task is created just to dispatch a message to the UI.
 * The message will go through the translate filter so you can also provide a translation code.
 */
public class LogTask extends AbstractTask {
    private String level;
    private String message;

    public LogTask(String level, String message) {
        setCode(TaskCode.LOG);
        this.level = level;
        this.message = message;
    }
}
