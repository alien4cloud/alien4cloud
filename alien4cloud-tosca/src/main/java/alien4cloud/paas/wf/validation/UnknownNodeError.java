package alien4cloud.paas.wf.validation;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * The referenced node doesn't exists.
 */
@Getter
@Setter
@AllArgsConstructor(suppressConstructorProperties = true)
@NoArgsConstructor
public class UnknownNodeError extends AbstractWorkflowError {
    private String stepId;
    private String nodeId;

    @Override
    public String toString() {
        return "UnknownNodeError: <" + nodeId + "> is not known in the context (step <" + stepId + ">)";
    }

}
