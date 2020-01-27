package alien4cloud.paas.wf.util;

import lombok.Getter;
import org.alien4cloud.tosca.model.workflow.Workflow;

public abstract class AbstractGraphTraversal implements GraphTraversal {

    @Getter
    protected final  Workflow workflow;

    protected final SubGraphFilter filter;

    protected AbstractGraphTraversal(Workflow workflow,SubGraphFilter filter) {
        this.workflow = workflow;
        this.filter = filter;
    }
}
