package alien4cloud.events;

import java.util.Map;
import java.util.Set;

import lombok.Getter;
import alien4cloud.model.application.ApplicationEnvironment;

@Getter
public class DeleteEnvironmentEvent extends AlienEvent {

    private static final long serialVersionUID = -1126617350064097857L;

    private ApplicationEnvironment applicationEnvironment;

    Map<String, Set<String>> orchestratorIdsAndOrchestratorDeploymentId;

    public DeleteEnvironmentEvent(Object source, ApplicationEnvironment applicationEnvironment, Map<String, Set<String>> orchestratorIdsAndOrchestratorDeploymentId) {
        super(source);
        this.applicationEnvironment = applicationEnvironment;
        this.orchestratorIdsAndOrchestratorDeploymentId = orchestratorIdsAndOrchestratorDeploymentId;
    }

}
