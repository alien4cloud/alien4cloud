package alien4cloud.rest.orchestrator.model;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public abstract class AbstractAuthorizationBatchRequest {
    protected String[] resources;
}
