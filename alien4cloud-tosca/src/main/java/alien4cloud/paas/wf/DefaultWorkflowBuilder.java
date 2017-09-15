package alien4cloud.paas.wf;

import static alien4cloud.utils.AlienUtils.safe;

import java.util.Collections;
import java.util.Map;
import java.util.stream.Collectors;

import org.alien4cloud.tosca.model.templates.NodeTemplate;
import org.alien4cloud.tosca.model.templates.RelationshipTemplate;
import org.alien4cloud.tosca.model.workflow.Workflow;
import org.alien4cloud.tosca.model.workflow.WorkflowStep;
import org.alien4cloud.tosca.model.workflow.activities.CallOperationWorkflowActivity;
import org.alien4cloud.tosca.model.workflow.activities.SetStateWorkflowActivity;
import org.alien4cloud.tosca.model.workflow.declarative.DefaultDeclarativeWorkflows;
import org.alien4cloud.tosca.model.workflow.declarative.NodeDeclarativeWorkflow;
import org.alien4cloud.tosca.model.workflow.declarative.OperationDeclarativeWorkflow;
import org.alien4cloud.tosca.model.workflow.declarative.RelationshipDeclarativeWorkflow;
import org.alien4cloud.tosca.model.workflow.declarative.RelationshipWeaving;
import org.apache.commons.lang3.StringUtils;

import alien4cloud.paas.plan.ToscaNodeLifecycleConstants;
import alien4cloud.paas.plan.ToscaRelationshipLifecycleConstants;
import alien4cloud.paas.wf.util.WorkflowUtils;

public class DefaultWorkflowBuilder extends AbstractWorkflowBuilder {

    private DefaultDeclarativeWorkflows defaultDeclarativeWorkflows;

    public DefaultWorkflowBuilder(DefaultDeclarativeWorkflows defaultDeclarativeWorkflows) {
        this.defaultDeclarativeWorkflows = defaultDeclarativeWorkflows;
    }

    private void declareStepDependencies(OperationDeclarativeWorkflow stepDependencies, WorkflowStep currentStep, Map<String, WorkflowStep> statesSteps,
            Map<String, WorkflowStep> operationSteps) {
        if (stepDependencies == null) {
            // The step has no dependencies
            return;
        }
        // Based on the dependencies configuration, link steps
        safe(stepDependencies.getFollowingOperations()).forEach(followingOperation -> {
            // We suppose that the configuration is correct and all reference must exist
            WorkflowUtils.linkSteps(currentStep, operationSteps.get(followingOperation));
        });
        safe(stepDependencies.getPrecedingOperations()).forEach(precedingOperation -> {
            // We suppose that the configuration is correct and all reference must exist
            WorkflowUtils.linkSteps(operationSteps.get(precedingOperation), currentStep);
        });
        String followingState = stepDependencies.getFollowingState();
        if (StringUtils.isNotBlank(followingState)) {
            WorkflowUtils.linkSteps(currentStep, statesSteps.get(followingState));
        }
        String precedingState = stepDependencies.getPrecedingState();
        if (StringUtils.isNotBlank(precedingState)) {
            WorkflowUtils.linkSteps(statesSteps.get(precedingState), currentStep);
        }
    }

    private Map<String, WorkflowStep> getNodeStateSteps(Workflow workflow, String nodeId) {
        // Get all state steps of the given node, then return a map of state name to workflow step
        return workflow.getSteps().values().stream()
                .filter(step -> step.getTarget().equals(nodeId) && StringUtils.isEmpty(step.getTargetRelationship())
                        && step.getActivity() instanceof SetStateWorkflowActivity)
                .collect(Collectors.toMap(step -> ((SetStateWorkflowActivity) step.getActivity()).getStateName(), step -> step));
    }

    private Map<String, WorkflowStep> getNodeOperationSteps(Workflow workflow, String nodeId) {
        // Get all operation steps of the given node, then return a map of operation name to workflow step
        return workflow.getSteps().values().stream()
                .filter(step -> step.getTarget().equals(nodeId) && StringUtils.isEmpty(step.getTargetRelationship())
                        && step.getActivity() instanceof CallOperationWorkflowActivity)
                .collect(Collectors.toMap(step -> ((CallOperationWorkflowActivity) step.getActivity()).getOperationName(), step -> step));
    }

    private void declareWeaving(RelationshipWeaving weaving, Map<String, WorkflowStep> fromStateSteps, Map<String, WorkflowStep> fromOperationSteps,
            Map<String, WorkflowStep> toStateSteps, Map<String, WorkflowStep> toOperationSteps) {
        if (weaving == null) {
            return;
        }
        safe(weaving.getStates()).forEach(
                (stateName, stateDependencies) -> declareStepDependencies(stateDependencies, fromStateSteps.get(stateName), toStateSteps, toOperationSteps));
        safe(weaving.getOperations()).forEach((operationName, operationDependencies) -> declareStepDependencies(operationDependencies,
                fromOperationSteps.get(operationName), toStateSteps, toOperationSteps));
    }

    @Override
    public void addNode(Workflow wf, String nodeId, WorkflowsBuilderService.TopologyContext toscaTypeFinder, boolean isCompute) {
        if (WorkflowUtils.isNativeOrSubstitutionNode(nodeId, toscaTypeFinder)) {
            // for a native node, we just add a sub-workflow step
            WorkflowUtils.addDelegateWorkflowStep(wf, nodeId);
        } else {
            NodeDeclarativeWorkflow nodeDeclarativeWorkflow = defaultDeclarativeWorkflows.getNodeWorkflows().get(wf.getName());
            // only trigger this method if it's a default workflow
            if (nodeDeclarativeWorkflow != null) {

                // Create all the states of the workflow at first
                Map<String, WorkflowStep> statesSteps = safe(nodeDeclarativeWorkflow.getStates()).entrySet().stream()
                        .collect(Collectors.toMap(Map.Entry::getKey, stateEntry -> WorkflowUtils.addStateStep(wf, nodeId, stateEntry.getKey())));

                // Create all the operations of the workflow at first
                Map<String, WorkflowStep> operationSteps = safe(nodeDeclarativeWorkflow.getOperations()).entrySet().stream().collect(Collectors.toMap(
                        Map.Entry::getKey,
                        operationEntry -> WorkflowUtils.addOperationStep(wf, nodeId, ToscaNodeLifecycleConstants.STANDARD_SHORT, operationEntry.getKey())));

                // Declare dependencies on the states steps
                safe(nodeDeclarativeWorkflow.getStates()).forEach(
                        (stateName, stateDependencies) -> declareStepDependencies(stateDependencies, statesSteps.get(stateName), statesSteps, operationSteps));

                // Declare dependencies on the operation steps
                safe(nodeDeclarativeWorkflow.getOperations()).forEach((operationName, operationDependencies) -> declareStepDependencies(operationDependencies,
                        operationSteps.get(operationName), statesSteps, operationSteps));
            }
        }
    }

    @Override
    public void addRelationship(Workflow wf, String nodeId, NodeTemplate nodeTemplate, RelationshipTemplate relationshipTemplate,
            WorkflowsBuilderService.TopologyContext toscaTypeFinder) {
        if (!WorkflowUtils.isNativeOrSubstitutionNode(nodeId, toscaTypeFinder)) {
            // for native types we don't care about relation ships in workflows
            RelationshipDeclarativeWorkflow relationshipDeclarativeWorkflow = defaultDeclarativeWorkflows.getRelationshipWorkflows().get(wf.getName());
            // only trigger this method if it's a default workflow
            if (relationshipDeclarativeWorkflow != null) {
                Map<String, WorkflowStep> relationshipOperationSteps = safe(relationshipDeclarativeWorkflow.getOperations()).entrySet().stream()
                        .collect(Collectors.toMap(Map.Entry::getKey, operationEntry -> WorkflowUtils.addRelationshipOperationStep(wf, nodeId,
                                relationshipTemplate.getName(), ToscaRelationshipLifecycleConstants.CONFIGURE_SHORT, operationEntry.getKey())));
                Map<String, WorkflowStep> sourceStateSteps = getNodeStateSteps(wf, nodeId);
                Map<String, WorkflowStep> targetStateSteps = getNodeStateSteps(wf, relationshipTemplate.getTarget());

                Map<String, WorkflowStep> sourceOperationsSteps = getNodeOperationSteps(wf, nodeId);
                Map<String, WorkflowStep> targetOperationSteps = getNodeOperationSteps(wf, relationshipTemplate.getTarget());

                safe(relationshipDeclarativeWorkflow.getOperations()).forEach((relationshipOperationName, relationshipOperationDependencies) -> {
                    WorkflowStep currentStep = relationshipOperationSteps.get(relationshipOperationName);
                    declareStepDependencies(relationshipOperationDependencies.getSource(), currentStep, sourceStateSteps, sourceOperationsSteps);
                    declareStepDependencies(relationshipOperationDependencies.getTarget(), currentStep, targetStateSteps, targetOperationSteps);
                    declareStepDependencies(relationshipOperationDependencies, currentStep, Collections.emptyMap(), relationshipOperationSteps);
                });
                RelationshipWeaving sourceWeaving = defaultDeclarativeWorkflows.getRelationshipsWeaving().get(wf.getName()).getSource();
                declareWeaving(sourceWeaving, sourceStateSteps, sourceOperationsSteps, targetStateSteps, targetOperationSteps);
                RelationshipWeaving targetWeaving = defaultDeclarativeWorkflows.getRelationshipsWeaving().get(wf.getName()).getTarget();
                declareWeaving(targetWeaving, targetStateSteps, targetOperationSteps, sourceStateSteps, sourceOperationsSteps);
            }
        }
    }
}
