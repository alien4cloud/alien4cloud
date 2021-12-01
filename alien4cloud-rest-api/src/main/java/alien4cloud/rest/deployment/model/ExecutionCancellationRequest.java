package alien4cloud.rest.deployment.model;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@ToString
@NoArgsConstructor
public class ExecutionCancellationRequest {

    private String environmentId;
    private String executionId;
}
