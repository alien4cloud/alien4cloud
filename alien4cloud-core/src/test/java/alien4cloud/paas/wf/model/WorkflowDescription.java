package alien4cloud.paas.wf.model;

import lombok.Getter;
import lombok.Setter;
import org.alien4cloud.tosca.model.workflow.declarative.NodeDeclarativeWorkflow;

import java.util.List;
import java.util.Map;

/**
 * Created by xdegenne on 22/06/2018.
 */
@Getter
@Setter
public class WorkflowDescription {
    private Map<String, Step> steps;
}
