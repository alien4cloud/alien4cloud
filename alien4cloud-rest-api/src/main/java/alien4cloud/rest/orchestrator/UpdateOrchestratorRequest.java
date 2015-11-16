package alien4cloud.rest.orchestrator;

import lombok.Getter;
import lombok.Setter;

import io.swagger.annotations.ApiModel;

/**
 * Update request for an orchestrator.
 */
@Getter
@Setter
@ApiModel(value = "Orchestrator update request.", description = "A request object to pass when updating an orchestrator. Contains updatable fields."
        + " a topology deployment. An orchestrator may manage one or multiple locations.")
public class UpdateOrchestratorRequest {
    private String name;
    private String deploymentNamePattern;
}