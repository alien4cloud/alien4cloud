package alien4cloud.paas.wf;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;

import lombok.Getter;
import lombok.Setter;

/**
 * A path is actually an ordered list of steps. We use a hash implem to ease 'contains' query.
 */
@Getter
@Setter
public class Path extends LinkedHashSet<AbstractStep> {

    boolean cycle;

    /**
     * The step responsible of the loop.
     */
    private AbstractStep loopingStep;
    
    public Path() {
        super();
    }

    public Path(Collection<? extends AbstractStep> c) {
        super(c);
    }

    public List<String> getStepNames() {
        List<String> stepNames = new ArrayList<String>();
        for (AbstractStep step : this) {
            stepNames.add(step.getName());
        }
        if (loopingStep != null) {
            stepNames.add(loopingStep.getName());
        }
        return stepNames;
    }

    @Override
    public String toString() {
        return "Path [cycle=" + cycle + ", loopingStep=" + loopingStep + ", path:" + super.toString() + "]";
    }

}
