package alien4cloud.paas.plan;

import lombok.Getter;
import lombok.Setter;

/**
 * Update the state of an element.
 * 
 * @author luc boutier
 */
@Getter
@Setter
@SuppressWarnings("PMD.UnusedPrivateField")
public class StateUpdateEvent extends AbstractWorkflowStep {
    private final String elementId;
    private final String state;

    public StateUpdateEvent(String elementId, String state) {
        this.elementId = elementId;
        this.state = state;
    }
}