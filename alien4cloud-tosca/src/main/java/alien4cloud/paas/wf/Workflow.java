package alien4cloud.paas.wf;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import lombok.Getter;
import lombok.Setter;

import org.apache.commons.collections4.CollectionUtils;

import alien4cloud.paas.wf.validation.AbstractWorkflowError;

@Getter
@Setter
public class Workflow {
    private String name;
    private String description;
    private boolean isStandard;
    private Set<String> hosts = new HashSet<String>();
    private List<AbstractWorkflowError> errors;

    /**
     * FIXME: Here we use a {@link LinkedHashMap} just to pass cfy3 provider generation plueprint tests !
     */
    private Map<String, AbstractStep> steps = new LinkedHashMap<String, AbstractStep>();

    public <S extends AbstractStep> S addStep(S step) {
        steps.put(step.getName(), step);
        return step;
    }

    public void clearErrors() {
        errors = new ArrayList<AbstractWorkflowError>();
    }

    public boolean hasErrors() {
        return CollectionUtils.isNotEmpty(errors);
    }

    public void addErrors(List<AbstractWorkflowError> errorsToAdd) {
        if (errors == null) {
            errors = new ArrayList<AbstractWorkflowError>(errorsToAdd);
        } else {
            errors.addAll(errorsToAdd);
        }
    }

}
