package alien4cloud.topology.task;

import java.util.List;

import com.google.common.collect.Maps;

public class ScalableTask extends PropertiesTask {

    public ScalableTask(String nodeTemplateName) {
        this.setCode(TaskCode.SCALABLE_CAPABILITY_INVALID);
        this.setNodeTemplateName(nodeTemplateName);
        this.setProperties(Maps.<TaskLevel, List<String>> newHashMap());
    }
}
