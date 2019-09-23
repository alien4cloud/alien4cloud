package alien4cloud.orchestrators.events;

import alien4cloud.events.AlienEvent;
import alien4cloud.model.orchestrators.Orchestrator;
import lombok.Getter;

/**
 * Event dispatched after an orchestrator has been enabled.
 */
@Getter
public class AfterOrchestratorEnabled extends AlienEvent {
    private final Orchestrator orchestrator;

    public AfterOrchestratorEnabled(Object source, Orchestrator orchestrator) {
        super(source);
        this.orchestrator = orchestrator;
    }
}
