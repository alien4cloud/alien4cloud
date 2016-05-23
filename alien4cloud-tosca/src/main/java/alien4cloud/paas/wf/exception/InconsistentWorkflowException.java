package alien4cloud.paas.wf.exception;

/**
 * The workflow is not consistent when some steps reference non existing steps (ie. when following or preceding ids refer to a step that can not be found in
 * the workflow).
 */
public class InconsistentWorkflowException extends WorkflowException {

    public InconsistentWorkflowException(String message) {
        super(message);
    }

}
