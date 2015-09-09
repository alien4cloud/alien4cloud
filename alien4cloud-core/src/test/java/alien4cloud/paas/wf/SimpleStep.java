package alien4cloud.paas.wf;

public class SimpleStep extends AbstractStep {

    public SimpleStep(String name) {
        super();
        super.setName(name);
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
