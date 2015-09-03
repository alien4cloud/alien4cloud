package alien4cloud.rest.topology;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import alien4cloud.paas.wf.AbstractActivity;

@Getter
@Setter
@NoArgsConstructor
@SuppressWarnings("PMD.UnusedPrivateField")
public class TopologyWorkflowAddActivityRequest {

    /**
     * If specified the step will be added near to this step.
     */
    private String relatedStepId;

    /**
     * If relatedStepId is specified, indicates if the step will be added before the related step (or after).
     */
    private boolean before;

    private AbstractActivity activity;
}
