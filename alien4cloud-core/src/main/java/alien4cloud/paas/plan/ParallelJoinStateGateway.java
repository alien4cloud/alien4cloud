package alien4cloud.paas.plan;

import java.util.Map;

import lombok.Getter;
import lombok.Setter;

/**
 * Perform Join based on elements states before going to the next step.
 * 
 * @author luc boutier
 */
@Getter
@Setter
@SuppressWarnings("PMD.UnusedPrivateField")
public class ParallelJoinStateGateway extends AbstractWorkflowStep {
    // elementId -> Accepteds STATE
    private final Map<String, String[]> validStatesPerElementMap;

    public ParallelJoinStateGateway(Map<String, String[]> validStatesPerElementMap) {
        this.validStatesPerElementMap = validStatesPerElementMap;
    }
}