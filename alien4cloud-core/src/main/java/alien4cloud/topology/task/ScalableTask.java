package alien4cloud.topology.task;

public class ScalableTask extends PropertiesTask {

    public ScalableTask(String nodeTemplateName) {
        this.setCode(TaskCode.SCALABLE_CAPABILITY_INVALID);
        this.setNodeTemplateName(nodeTemplateName);
    }
}
