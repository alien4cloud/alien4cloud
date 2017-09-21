package alien4cloud.topology.task;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.Set;

@Getter
@Setter
@NoArgsConstructor
public class UnresolvablePredefinedInputsTask extends AbstractTask {

    private Set<String> unresolvableInputs;

    public UnresolvablePredefinedInputsTask(Set<String> unresolvableInputs) {
        this.unresolvableInputs = unresolvableInputs;
        this.setCode(TaskCode.UNRESOLVABLE_PREDEFINED_INPUTS);
    }
}
