package alien4cloud.topology.warning;

import alien4cloud.topology.task.AbstractTask;
import alien4cloud.topology.task.TaskCode;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@EqualsAndHashCode(callSuper = false)
public class IllegalOperationWarning extends AbstractTask {
    private String nodeTemplateName;
    private String interfaceName;
    private String operationName;
    private String serviceName;
    private String relationshipType;

    public IllegalOperationWarning(){
        setCode(TaskCode.FORBIDDEN_OPERATION);
    }
}
