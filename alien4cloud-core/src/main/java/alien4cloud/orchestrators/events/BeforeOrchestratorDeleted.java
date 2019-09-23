package alien4cloud.orchestrators.events;

import alien4cloud.events.AlienEvent;
import lombok.Getter;

/**
 * Event dispatched before deleting an orchestrator.
 */
@Getter
public class BeforeOrchestratorDeleted extends AlienEvent {
    private final String orchestratorId;

    public BeforeOrchestratorDeleted(Object source, String orchestratorId) {
        super(source);
        this.orchestratorId = orchestratorId;
    }
}
