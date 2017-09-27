package alien4cloud.paas.wf;

import org.alien4cloud.tosca.model.workflow.WorkflowStep;

public class SimpleStep extends WorkflowStep {

    public SimpleStep(String name) {
        super();
        super.setName(name);
        super.setTarget(name);
    }

    @Override
    public String toString() {
        return getStepAsString();
    }

    @Override
    public String getStepAsString() {
        return getName();
    }

}
