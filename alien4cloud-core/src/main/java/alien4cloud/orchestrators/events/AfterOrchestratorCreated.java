package alien4cloud.orchestrators.events;

import alien4cloud.events.AlienEvent;
import alien4cloud.model.orchestrators.Orchestrator;
import lombok.Getter;

/**
 * Event dispatched after an Orchestrator has been created.
 */
@Getter
public class AfterOrchestratorCreated extends AlienEvent {
    private final Orchestrator orchestrator;

    public AfterOrchestratorCreated(Object source, Orchestrator orchestrator) {
        super(source);
        this.orchestrator = orchestrator;
    }
}
