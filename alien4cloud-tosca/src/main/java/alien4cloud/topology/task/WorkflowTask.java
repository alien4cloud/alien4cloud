package alien4cloud.topology.task;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor(suppressConstructorProperties = true)
public class WorkflowTask extends AbstractTask {
    private String workflowName;
    private int errorCount;
}
