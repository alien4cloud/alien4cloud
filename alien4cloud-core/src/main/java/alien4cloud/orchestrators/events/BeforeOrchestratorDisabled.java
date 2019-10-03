package alien4cloud.orchestrators.events;

import alien4cloud.events.AlienEvent;
import alien4cloud.model.orchestrators.Orchestrator;
import lombok.Getter;

/**
 * Event dispatched before disabling an orchestrator.
 */
@Getter
public class BeforeOrchestratorDisabled extends AlienEvent {
    private final Orchestrator orchestrator;

    public BeforeOrchestratorDisabled(Object source, Orchestrator orchestrator) {
        super(source);
        this.orchestrator = orchestrator;
    }
}
