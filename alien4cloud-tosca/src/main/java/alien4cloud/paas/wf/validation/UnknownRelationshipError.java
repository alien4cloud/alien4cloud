package alien4cloud.paas.wf.validation;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor(suppressConstructorProperties = true)
@NoArgsConstructor
public class UnknownRelationshipError extends AbstractWorkflowError {
    private String stepId;
    private String nodeId;
    private String relationshipId;

    @Override
    public String toString() {
        return "UnknownNodeError: <" + nodeId + "> is not known in the context (step <" + stepId + ">)";
    }
}
