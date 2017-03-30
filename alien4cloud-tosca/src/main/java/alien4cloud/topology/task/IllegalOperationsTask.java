package alien4cloud.topology.task;

import alien4cloud.topology.warning.IllegalOperationWarning;
import lombok.Getter;
import lombok.Setter;

import java.util.LinkedList;
import java.util.List;

@Getter
@Setter
public class IllegalOperationsTask extends AbstractTask {
    private List<IllegalOperationWarning> illegalOperations;

    public IllegalOperationsTask(){
        setCode(TaskCode.FORBIDDEN_OPERATION);
        this.illegalOperations = new LinkedList<>();
    }
}
