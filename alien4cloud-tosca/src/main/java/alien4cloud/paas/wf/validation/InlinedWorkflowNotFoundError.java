package alien4cloud.paas.wf.validation;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor(suppressConstructorProperties = true)
@NoArgsConstructor
public class InlinedWorkflowNotFoundError extends AbstractWorkflowError {
    private String stepId;
    private String inlinedWorkflow;
}
