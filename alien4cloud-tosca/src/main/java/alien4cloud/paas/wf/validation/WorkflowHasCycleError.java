package alien4cloud.paas.wf.validation;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor(suppressConstructorProperties = true)
@NoArgsConstructor
public class WorkflowHasCycleError extends AbstractWorkflowError {

    /**
     * The cycle described as a succession of ordered step names (each is connected to the step after).
     * <p>
     * For example : [A, B, C, A]:
     * 
     * <pre>
     *   A ----- B
     *     \   /
     *       C
     * </pre>
     */
    private List<String> cycle;

    @Override
    public String toString() {
        return "WorkflowHasCycleError [cycle=" + cycle + "]";
    }

}
