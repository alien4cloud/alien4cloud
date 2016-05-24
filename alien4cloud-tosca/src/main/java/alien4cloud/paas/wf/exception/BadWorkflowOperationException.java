package alien4cloud.paas.wf.exception;

/**
 * Some actions can not be done onto some kind of workflows.
 */
public class BadWorkflowOperationException extends WorkflowException {

    public BadWorkflowOperationException(String message) {
        super(message);
    }

}
