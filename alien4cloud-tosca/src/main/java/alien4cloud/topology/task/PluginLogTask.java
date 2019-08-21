package alien4cloud.topology.task;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@ToString
public class PluginLogTask extends AbstractTask {

    public PluginLogTask() {
        setCode(TaskCode.VALIDATION_PLUGIN);
    }

    public PluginLogTask(String plugin, String message) {
            this.plugin = plugin;
            this.message = message;
    }

    public String plugin;

    public String message;
}
