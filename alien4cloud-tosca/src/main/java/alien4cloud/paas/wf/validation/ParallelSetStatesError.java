package alien4cloud.paas.wf.validation;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Some state steps are run in parallel and should not.
 * 
 * TODO: which one ?
 */
@Getter
@Setter
@AllArgsConstructor(suppressConstructorProperties = true)
@NoArgsConstructor
public class ParallelSetStatesError extends AbstractWorkflowError {
    private String nodeId;
}
