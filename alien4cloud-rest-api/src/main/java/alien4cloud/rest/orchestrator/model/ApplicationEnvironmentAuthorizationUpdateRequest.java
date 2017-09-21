package alien4cloud.rest.orchestrator.model;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ApplicationEnvironmentAuthorizationUpdateRequest extends AbstractAuthorizationBatchRequest {
    private String[] applicationsToDelete;
    private String[] environmentsToDelete;
    private String[] environmentTypesToDelete;
    private String[] applicationsToAdd;
    private String[] environmentsToAdd;
    private String[] environmentTypesToAdd;
}