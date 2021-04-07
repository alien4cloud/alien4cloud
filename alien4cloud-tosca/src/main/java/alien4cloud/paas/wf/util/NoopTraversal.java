package alien4cloud.paas.wf.util;

import org.alien4cloud.tosca.model.workflow.Workflow;

public class NoopTraversal extends AbstractGraphTraversal {

    protected NoopTraversal(Workflow workflow, SubGraphFilter filter)  {
        super(workflow,filter);
    }

    @Override
    public void browse() {
        // No Operation
    }

    public static NoopTraversal builder(Workflow workflow, SubGraphFilter filter) {
        return new NoopTraversal(workflow,filter);
    }

}
