package alien4cloud.paas.wf.model;

import lombok.Getter;
import lombok.Setter;

import java.util.List;
import java.util.Map;

/**
 * Created by xdegenne on 22/06/2018.
 */
@Getter
@Setter
public class WorkflowTestDescription {
    private String name;
    private String description;
    private WorkflowDescription initial;
    private WorkflowDescription expected;
}
