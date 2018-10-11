package alien4cloud.paas.wf;

import static alien4cloud.utils.AlienUtils.safe;
import static org.alien4cloud.tosca.normative.constants.NormativeWorkflowNameConstants.CANCEL;
import static org.alien4cloud.tosca.normative.constants.NormativeWorkflowNameConstants.RUN;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.alien4cloud.tosca.model.templates.NodeTemplate;
import org.alien4cloud.tosca.model.templates.RelationshipTemplate;
import org.alien4cloud.tosca.model.types.RelationshipType;
import org.alien4cloud.tosca.model.workflow.Workflow;
import org.alien4cloud.tosca.model.workflow.WorkflowStep;
import org.alien4cloud.tosca.model.workflow.declarative.DefaultDeclarativeWorkflows;
import org.alien4cloud.tosca.model.workflow.declarative.NodeDeclarativeWorkflow;
import org.alien4cloud.tosca.model.workflow.declarative.OperationDeclarativeWorkflow;
import org.alien4cloud.tosca.model.workflow.declarative.RelationshipDeclarativeWorkflow;
import org.alien4cloud.tosca.model.workflow.declarative.RelationshipOperationHost;
import org.alien4cloud.tosca.model.workflow.declarative.RelationshipWeaving;
import org.alien4cloud.tosca.model.workflow.declarative.RelationshipWeavingDeclarativeWorkflow;
import org.apache.commons.lang3.StringUtils;

import alien4cloud.paas.plan.ToscaNodeLifecycleConstants;
import alien4cloud.paas.plan.ToscaRelationshipLifecycleConstants;
import alien4cloud.paas.wf.util.Steps;
import alien4cloud.paas.wf.util.WorkflowUtils;

public class DefaultWorkflowBuilder extends AbstractWorkflowBuilder {

    private DefaultDeclarativeWorkflows defaultDeclarativeWorkflows;

    DefaultWorkflowBuilder(DefaultDeclarativeWorkflows defaultDeclarativeWorkflows) {
        this.defaultDeclarativeWorkflows = defaultDeclarativeWorkflows;
    }

    private void declareStepDependencies(OperationDeclarativeWorkflow stepDependencies, WorkflowStep currentStep, Steps nodeSteps) {
        if (stepDependencies == null) {
            // The step has no dependencies
            return;
        }
        // Based on the dependencies configuration, link steps
        safe(stepDependencies.getFollowingOperations()).forEach(followingOperation -> {
            // We suppose that the configuration is correct and all reference must exist
            WorkflowUtils.linkSteps(currentStep, nodeSteps.getOperationStep(followingOperation));
        });
        safe(stepDependencies.getPrecedingOperations()).forEach(precedingOperation -> {
            // We suppose that the configuration is correct and all reference must exist
            WorkflowUtils.linkSteps(nodeSteps.getOperationStep(precedingOperation), currentStep);
        });
        String followingState = stepDependencies.getFollowingState();
        if (StringUtils.isNotBlank(followingState)) {
            WorkflowUtils.linkSteps(currentStep, nodeSteps.getStateStep(followingState));
        }
        String precedingState = stepDependencies.getPrecedingState();
        if (StringUtils.isNotBlank(precedingState)) {
            WorkflowUtils.linkSteps(nodeSteps.getStateStep(precedingState), currentStep);
        }
    }

    private void declareWeaving(RelationshipWeaving weaving, Steps fromSteps, Steps toSteps) {
        if (weaving == null) {
            return;
        }
        safe(weaving.getStates())
                .forEach((stateName, stateDependencies) -> declareStepDependencies(stateDependencies, fromSteps.getStateStep(stateName), toSteps));
        safe(weaving.getOperations()).forEach(
                (operationName, operationDependencies) -> declareStepDependencies(operationDependencies, fromSteps.getOperationStep(operationName), toSteps));
    }

    @Override
    public void addNode(Workflow workflow, String nodeId, TopologyContext topologyContext, boolean isCompute) {
        if (WorkflowUtils.isNativeOrSubstitutionNode(nodeId, topologyContext)) {
            // for a native node, we just add a sub-workflow step, except for 'run' workflow
            if (!workflow.getName().equals(RUN) && !workflow.getName().equals(CANCEL)) {
                WorkflowUtils.addDelegateWorkflowStep(workflow, nodeId);
            }
        } else {
            NodeDeclarativeWorkflow nodeDeclarativeWorkflow = defaultDeclarativeWorkflows.getNodeWorkflows().get(workflow.getName());
            // only trigger this method if it's a default workflow
            if (nodeDeclarativeWorkflow != null) {

                // Create all the states of the workflow at first
                Map<String, WorkflowStep> statesSteps = safe(nodeDeclarativeWorkflow.getStates()).entrySet().stream()
                        .collect(Collectors.toMap(Map.Entry::getKey, stateEntry -> WorkflowUtils.addStateStep(workflow, nodeId, stateEntry.getKey())));

                // Create all the operations of the workflow at first
                Map<String, WorkflowStep> operationSteps = safe(nodeDeclarativeWorkflow.getOperations()).entrySet().stream()
                        .collect(Collectors.toMap(Map.Entry::getKey, operationEntry -> addOperationStep(workflow, nodeId, operationEntry.getKey())));
                Steps steps = new Steps(operationSteps, statesSteps, null);
                // Declare dependencies on the states steps
                safe(nodeDeclarativeWorkflow.getStates())
                        .forEach((stateName, stateDependencies) -> declareStepDependencies(stateDependencies, steps.getStateStep(stateName), steps));

                // Declare dependencies on the operation steps
                safe(nodeDeclarativeWorkflow.getOperations()).forEach(
                        (operationName, operationDependencies) -> declareStepDependencies(operationDependencies, steps.getOperationStep(operationName), steps));
            }
        }
    }

    private static WorkflowStep addOperationStep(Workflow workflow, String nodeId, String operationFqn) {
        if (operationFqn.contains(".")) {
            String interfaceName = operationFqn.substring(0, operationFqn.lastIndexOf("."));
            String operationName = operationFqn.substring(operationFqn.lastIndexOf(".") + 1, operationFqn.length());
            return WorkflowUtils.addOperationStep(workflow, nodeId, interfaceName, operationName);
        } else {
            return WorkflowUtils.addOperationStep(workflow, nodeId, ToscaNodeLifecycleConstants.STANDARD_SHORT, operationFqn);
        }
    }

    private RelationshipWeavingDeclarativeWorkflow getRelationshipWeavingDeclarativeWorkflow(String relationshipTypeName, TopologyContext toscaTypeFinder,
            String workflowName) {
        RelationshipType indexedRelationshipType = toscaTypeFinder.findElement(RelationshipType.class, relationshipTypeName);
        List<String> typesToCheck = new ArrayList<>();
        typesToCheck.add(indexedRelationshipType.getElementId());
        if (indexedRelationshipType.getDerivedFrom() != null) {
            typesToCheck.addAll(indexedRelationshipType.getDerivedFrom());
        }
        Map<String, Map<String, RelationshipWeavingDeclarativeWorkflow>> weavingConfigsPerRelationshipType = defaultDeclarativeWorkflows
                .getRelationshipsWeaving();
        for (String typeToCheck : typesToCheck) {
            if (weavingConfigsPerRelationshipType.containsKey(typeToCheck)) {
                return weavingConfigsPerRelationshipType.get(typeToCheck).get(workflowName);
            }
        }
        // This will never happen if the declarative configuration has tosca.relationships.Root configured
        throw new IllegalStateException("Default declarative configuration for workflow must have tosca.relationships.Root configured");
    }

    @Override
    public void addRelationship(Workflow workflow, String nodeId, NodeTemplate nodeTemplate, String relationshipName, RelationshipTemplate relationshipTemplate,
            TopologyContext topologyContext) {
        boolean sourceIsNative = WorkflowUtils.isNativeOrSubstitutionNode(nodeId, topologyContext);
        boolean targetIsNative = WorkflowUtils.isNativeOrSubstitutionNode(relationshipTemplate.getTarget(), topologyContext);
        if (!sourceIsNative || !targetIsNative) {
            // source or target is native or abstract
            // for native types we don't care about relation ships in workflows
            RelationshipDeclarativeWorkflow relationshipDeclarativeWorkflow = defaultDeclarativeWorkflows.getRelationshipWorkflows().get(workflow.getName());

            Steps sourceSteps = new Steps(workflow, nodeId);
            Steps targetSteps = new Steps(workflow, relationshipTemplate.getTarget());

            // only trigger this method if it's a default workflow
            if (relationshipDeclarativeWorkflow != null) {
                Map<String, WorkflowStep> relationshipOperationSteps = safe(relationshipDeclarativeWorkflow.getOperations()).entrySet().stream()
                        .filter(operationEntry -> !targetIsNative || operationEntry.getValue().getOperationHost() == RelationshipOperationHost.SOURCE)
                        .filter(operationEntry -> !sourceIsNative || operationEntry.getValue().getOperationHost() == RelationshipOperationHost.TARGET)
                        .collect(Collectors.toMap(Map.Entry::getKey,
                                operationEntry -> WorkflowUtils.addRelationshipOperationStep(workflow, nodeId, relationshipTemplate.getName(),
                                        ToscaRelationshipLifecycleConstants.CONFIGURE_SHORT, operationEntry.getKey(),
                                        operationEntry.getValue().getOperationHost().toString())));


                safe(relationshipDeclarativeWorkflow.getOperations()).forEach((relationshipOperationName, relationshipOperationDependencies) -> {
                    WorkflowStep currentStep = relationshipOperationSteps.get(relationshipOperationName);
                    if (currentStep != null) {
                        // It might be filtered if source or target is native
                        declareStepDependencies(relationshipOperationDependencies.getSource(), currentStep, sourceSteps);
                        declareStepDependencies(relationshipOperationDependencies.getTarget(), currentStep, targetSteps);
                        declareStepDependencies(relationshipOperationDependencies, currentStep,
                                new Steps(relationshipOperationSteps, Collections.emptyMap(), null));
                    }
                });
            }

            RelationshipWeavingDeclarativeWorkflow relationshipWeavingDeclarativeWorkflow = getRelationshipWeavingDeclarativeWorkflow(
                    relationshipTemplate.getType(), topologyContext, workflow.getName());
            if (relationshipWeavingDeclarativeWorkflow != null) {
                declareWeaving(relationshipWeavingDeclarativeWorkflow.getSource(), sourceSteps, targetSteps);
                declareWeaving(relationshipWeavingDeclarativeWorkflow.getTarget(), targetSteps, sourceSteps);
            }

        } else {
            // both source and target are native then the relationship does not have any operation implemented
            // we will just try to declare weaving between source node operations and target node operations
            Steps sourceSteps = new Steps(workflow, nodeId);
            Steps targetSteps = new Steps(workflow, relationshipTemplate.getTarget());
            RelationshipWeavingDeclarativeWorkflow relationshipWeavingDeclarativeWorkflow = getRelationshipWeavingDeclarativeWorkflow(
                    relationshipTemplate.getType(), topologyContext, workflow.getName());
            if (relationshipWeavingDeclarativeWorkflow != null) {
                declareWeaving(relationshipWeavingDeclarativeWorkflow.getSource(), sourceSteps, targetSteps);
                declareWeaving(relationshipWeavingDeclarativeWorkflow.getTarget(), targetSteps, sourceSteps);
            }
        }
    }
}
