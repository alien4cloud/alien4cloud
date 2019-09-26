package alien4cloud.orchestrators.events;

import alien4cloud.events.AlienEvent;
import alien4cloud.model.orchestrators.OrchestratorConfiguration;
import lombok.Getter;

/**
 * Event dispatched after an orchestrator configuration had changed.
 */
@Getter
public class OnOrchestratorConfigurationChanged extends AlienEvent {
    private final String orchestratorId;
    private final OrchestratorConfiguration configuration;

    public OnOrchestratorConfigurationChanged(Object source, String orchestratorId, OrchestratorConfiguration configuration) {
        super(source);
        this.orchestratorId = orchestratorId;
        this.configuration = configuration;
    }
}
