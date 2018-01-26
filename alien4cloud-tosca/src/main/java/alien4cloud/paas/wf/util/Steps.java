package alien4cloud.paas.wf.util;

import java.util.Map;
import java.util.stream.Collectors;

import org.alien4cloud.tosca.model.workflow.Workflow;
import org.alien4cloud.tosca.model.workflow.WorkflowStep;
import org.alien4cloud.tosca.model.workflow.activities.CallOperationWorkflowActivity;
import org.alien4cloud.tosca.model.workflow.activities.DelegateWorkflowActivity;
import org.alien4cloud.tosca.model.workflow.activities.SetStateWorkflowActivity;

/**
 * Wrapper to retrieve node steps based on operation name or state name
 */
public class Steps {
    private Map<String, WorkflowStep> operationSteps;
    private Map<String, WorkflowStep> stateSteps;
    private WorkflowStep delegateStep;

    private static Map<String, WorkflowStep> getNodeStateSteps(Workflow workflow, String nodeId) {
        // Get all state steps of the given node, then return a map of state name to workflow step
        return workflow.getSteps().values().stream()
                .filter(step -> WorkflowUtils.isNodeStep(step, nodeId) && step.getActivity() instanceof SetStateWorkflowActivity)
                .collect(Collectors.toMap(step -> ((SetStateWorkflowActivity) step.getActivity()).getStateName(), step -> step));
    }

    private static Map<String, WorkflowStep> getNodeOperationSteps(Workflow workflow, String nodeId) {
        // Get all operation steps of the given node, then return a map of operation name to workflow step
        return workflow.getSteps().values().stream()
                .filter(step -> WorkflowUtils.isNodeStep(step, nodeId) && (step.getActivity() instanceof CallOperationWorkflowActivity))
                .collect(Collectors.toMap(step -> ((CallOperationWorkflowActivity) step.getActivity()).getOperationName(), step -> step));
    }

    private static WorkflowStep getDelegateStep(Workflow workflow, String nodeId) {
        return workflow.getSteps().values().stream()
                .filter(step -> WorkflowUtils.isNodeStep(step, nodeId) && step.getActivity() instanceof DelegateWorkflowActivity).findFirst().orElse(null);
    }

    public Steps(Workflow workflow, String nodeId) {
        this.operationSteps = getNodeOperationSteps(workflow, nodeId);
        this.stateSteps = getNodeStateSteps(workflow, nodeId);
        this.delegateStep = getDelegateStep(workflow, nodeId);
    }

    public Steps(Map<String, WorkflowStep> operationSteps, Map<String, WorkflowStep> stateSteps, WorkflowStep delegateStep) {
        this.operationSteps = operationSteps;
        this.stateSteps = stateSteps;
        this.delegateStep = delegateStep;
    }

    public WorkflowStep getStateStep(String stateName) {
        WorkflowStep stateStep = stateSteps.get(stateName);
        if (stateStep == null) {
            return delegateStep;
        } else {
            return stateStep;
        }
    }

    public WorkflowStep getOperationStep(String operationName) {
        WorkflowStep operationStep = operationSteps.get(operationName);
        if (operationStep == null) {
            return delegateStep;
        } else {
            return operationStep;
        }
    }
}
