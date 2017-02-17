package alien4cloud.rest.orchestrator.model;

import lombok.Getter;
import lombok.Setter;

/**
 * only for subjects like {@link alien4cloud.security.Subject#USER}, {@link alien4cloud.security.Subject#GROUP}.<br/>
 * <p>
 * for {@link alien4cloud.security.Subject#APPLICATION} and {@link alien4cloud.security.Subject#ENVIRONMENT}, see {@link ApplicationEnvironmentAuthorizationDTO}
 * </p>
 */
@Getter
@Setter
public class SubjectsAuthorizationRequest extends AbstractAuthorizationBatchRequest {
    String[] create;

    String[] delete;
}
