package alien4cloud.orchestrators.events;

import alien4cloud.events.AlienEvent;
import lombok.Getter;

/**
 * Event dispatched after an orchestrator has been deleted.
 */
@Getter
public class AfterOrchestratorDeleted extends AlienEvent {
    private final String orchestratorId;

    public AfterOrchestratorDeleted(Object source, String orchestratorId) {
        super(source);
        this.orchestratorId = orchestratorId;
    }
}
