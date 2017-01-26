package alien4cloud.rest.orchestrator.model;

import alien4cloud.model.application.Application;
import alien4cloud.model.application.ApplicationEnvironment;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

/**
 * This DTO represents an authorization given to a given application / environment.
 * When environments is empty, this means that the whole application has persmission (all it's environments).
 */
@Getter
@Setter
public class ApplicationEnvironmentAuthorizationDTO {

    private Application application;

    private List<ApplicationEnvironment> environments;

}
