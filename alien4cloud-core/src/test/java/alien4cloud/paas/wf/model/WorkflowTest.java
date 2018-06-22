package alien4cloud.paas.wf.model;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.alien4cloud.tosca.model.workflow.Workflow;

/**
 * Created by xdegenne on 22/06/2018.
 */
@AllArgsConstructor
@Getter
public class WorkflowTest {
    private Workflow initial;
    private Workflow expected;
}
