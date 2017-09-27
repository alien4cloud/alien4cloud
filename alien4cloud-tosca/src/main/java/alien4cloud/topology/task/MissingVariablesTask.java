package alien4cloud.topology.task;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.Set;

@Getter
@Setter
@NoArgsConstructor
public class MissingVariablesTask extends AbstractTask {

    private Set<String> variables;

    public MissingVariablesTask(Set<String> variables) {
        this.variables = variables;
        this.setCode(TaskCode.MISSING_VARIABLES);
    }
}
