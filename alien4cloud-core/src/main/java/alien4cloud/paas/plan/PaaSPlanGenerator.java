package alien4cloud.paas.plan;

import static alien4cloud.paas.plan.PlanGeneratorConstants.*;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Set;

import alien4cloud.paas.AbstractPaaSProvider;
import alien4cloud.paas.model.PaaSNodeTemplate;
import alien4cloud.paas.model.PaaSRelationshipTemplate;
import alien4cloud.tosca.model.Interface;
import alien4cloud.tosca.model.Operation;
import alien4cloud.utils.MapUtil;

import com.google.common.collect.Sets;

/**
 * Generate the build plan based on normative types and interfaces.
 * 
 * @author luc boutier
 */
public final class PaaSPlanGenerator {
    private PaaSPlanGenerator() {
    }

    /**
     * Create a full build plan from a hierarchy of {@link PaaSNodeTemplate}.
     * 
     * @param roots The roots of the {@link PaaSNodeTemplate} hierarchy.
     * @return The start event for the flow.
     */
    public static StartEvent buildPlan(List<PaaSNodeTemplate> roots) {
        StartEvent startEvent = new StartEvent();
        ParallelGateway parallelGateway = new ParallelGateway();
        parallelGateway.setNextStep(new StopEvent());
        parallelGateway.setPreviousStep(startEvent);
        for (PaaSNodeTemplate nodeTemplate : roots) {
            WorkflowStep nodeLastStep = buildNodeCreationPlan(parallelGateway, nodeTemplate);
            buildNodeStartPlan(nodeLastStep, nodeTemplate);
        }
        startEvent.setNextStep(parallelGateway);
        return startEvent;
    }

    /**
     * Create the creation plan for a given node. The creation plan is a sub-plan of the build plan. It includes the creation and configuration steps for the
     * different nodes of the topology.
     *
     * @param nodeTemplate The node template for which to build a creation plan (this should be done on a root node in a topology).
     * @return The start event for the node creation plan.
     */
    public static StartEvent buildNodeCreationPlan(PaaSNodeTemplate nodeTemplate) {
        StartEvent startEvent = new StartEvent();
        buildNodeCreationPlan(startEvent, nodeTemplate).setNextStep(new StopEvent());
        return startEvent;
    }

    /**
     * Create the creation plan for a given node. The creation plan is a sub-plan of the build plan. It includes the creation and configuration steps for the
     * different nodes of the topology.
     * 
     * @param previousStep The step on which to append the first step of the node plan. This can be either
     * @param nodeTemplate The node template for which to build the flow.
     * @return The last step of the node creation plan. This allows to add some other steps (like the start plan).
     */
    private static WorkflowStep buildNodeCreationPlan(WorkflowStep previousStep, PaaSNodeTemplate nodeTemplate) {
        Interface lifecycle = getLifecycleInterface(nodeTemplate);
        WorkflowStep lastStep = previousStep;
        lastStep = addToPreviousStep(
                lastStep,
                createOperationActivity(nodeTemplate.getCsarPath(), lifecycle, nodeTemplate.getId(), null, NODE_LIFECYCLE_INTERFACE_NAME, CREATE_OPERATION_NAME));
        lastStep = addToPreviousStep(lastStep, new StateUpdateEvent(nodeTemplate.getId(), STATE_CREATED));
        lastStep = addToPreviousStep(
                lastStep,
                createOperationActivity(nodeTemplate.getCsarPath(), lifecycle, nodeTemplate.getId(), null, NODE_LIFECYCLE_INTERFACE_NAME,
                        CONFIGURE_OPERATION_NAME)).setNextStep(new StateUpdateEvent(nodeTemplate.getId(), STATE_CONFIGURED));

        for (PaaSNodeTemplate child : nodeTemplate.getChildren()) {
            lastStep = buildNodeCreationPlan(lastStep, child);
        }

        return lastStep;
    }

    private static WorkflowStep createOperationActivity(Path csarPath, Interface interfaz, String nodeTemplateId, String relationshipId, String interfaceName,
            String operationName) {
        OperationCallActivity activity = new OperationCallActivity();
        activity.setCsarPath(csarPath);
        activity.setInterfaceName(interfaceName);
        activity.setOperationName(operationName);
        activity.setNodeTemplateId(nodeTemplateId);
        activity.setRelationshipId(relationshipId);
        // get the info from the type.
        Operation operation = interfaz.getOperations().get(operationName);
        if (operation != null && operation.getImplementationArtifact() != null) {
            activity.setImplementationArtifact(operation.getImplementationArtifact());
        }
        return activity;
    }

    private static WorkflowStep createParallelJoinStateGateway(Set<String> targets, String[] expectedStates) {
        String[] keys = targets.toArray(new String[targets.size()]);
        String[][] values = new String[keys.length][];
        for (int i = 0; i < values.length; i++) {
            values[i] = expectedStates;
        }
        return new ParallelJoinStateGateway(MapUtil.newHashMap(keys, values));
    }

    /**
     * Create the creation plan for a given node. The creation plan is a sub-plan of the build plan. It includes the creation and configuration steps for the
     * different nodes of the topology.
     * 
     * @param nodeTemplate The node template for which to build a creation plan (this should be done on a root node in a topology).
     * @return The start event for the node creation plan.
     */
    public static StartEvent buildNodeStartPlan(PaaSNodeTemplate nodeTemplate) {
        StartEvent startEvent = new StartEvent();
        buildNodeStartPlan(startEvent, nodeTemplate).setNextStep(new StopEvent());
        return startEvent;
    }

    private static WorkflowStep buildNodeStartPlan(WorkflowStep previousStep, PaaSNodeTemplate nodeTemplate) {
        // check the dependencies in order to manage startup of components in the correct order.
        Interface lifecycle = getLifecycleInterface(nodeTemplate);
        WorkflowStep lastStep = previousStep;

        lastStep = buildDependencyWait(nodeTemplate, lastStep);
        // set that contains ids of the nodes that are connected to the current node. Required to wait for all nodes to be started before executing post
        // relationship configurations.
        Set<String> connectedNodeTemplateIds = Sets.newHashSet();
        for (PaaSRelationshipTemplate relationshipTemplate : nodeTemplate.getRelationshipTemplates()) {
            lastStep = buildRelationshipPreStartPlan(nodeTemplate, relationshipTemplate, lastStep, connectedNodeTemplateIds);
        }

        // start the node
        lastStep = addToPreviousStep(lastStep,
                createOperationActivity(nodeTemplate.getCsarPath(), lifecycle, nodeTemplate.getId(), null, NODE_LIFECYCLE_INTERFACE_NAME, START_OPERATION_NAME));
        lastStep = addToPreviousStep(lastStep, new StateUpdateEvent(nodeTemplate.getId(), STATE_STARTED));

        // cleanup if start event is performed synchronously before
        Set<String> cleanConnectedNodeTemplateIds = Sets.newHashSet();
        for (String connectedNodeTemplateId : connectedNodeTemplateIds) {
            if (!isStartEventPreviousStep(lastStep, connectedNodeTemplateId)) {
                cleanConnectedNodeTemplateIds.add(connectedNodeTemplateId);
            }
        }
        // add the join gateway to wait for states before post-connecting
        if (cleanConnectedNodeTemplateIds.size() > 0) {
            lastStep = addToPreviousStep(lastStep, createParallelJoinStateGateway(cleanConnectedNodeTemplateIds, new String[] { STATE_STARTED }));
        }
        // relationship postSource/ postTarget
        for (PaaSRelationshipTemplate relationshipTemplate : nodeTemplate.getRelationshipTemplates()) {
            lastStep = buildRelationshipPostStartPlan(nodeTemplate, relationshipTemplate, lastStep);
        }

        // process sub-nodes
        ParallelGateway gateway = new ParallelGateway();
        gateway.setPreviousStep(lastStep);
        for (PaaSNodeTemplate child : nodeTemplate.getChildren()) {
            // start the child nodes
            buildNodeStartPlan(gateway, child);
        }
        if (gateway.getParallelSteps().size() > 0) {
            return lastStep.setNextStep(gateway);
        }
        return lastStep;
    }

    private static WorkflowStep buildDependencyWait(PaaSNodeTemplate currentNode, WorkflowStep previousStep) {
        // set that contains ids of the nodes from wich the current node depends. This will be used to make sure we wait for these nodes to be started before we
        // actually can start.
        Set<String> nodeDependencies = Sets.newHashSet();

        for (PaaSRelationshipTemplate relationshipTemplate : currentNode.getRelationshipTemplates()) {
            if (relationshipTemplate.instanceOf(AbstractPaaSProvider.DEPENDS_ON) && relationshipTemplate.getSource().equals(currentNode.getId())) {
                if (!isStartEventPreviousStep(previousStep, relationshipTemplate.getRelationshipTemplate().getTarget())) {
                    nodeDependencies.add(relationshipTemplate.getRelationshipTemplate().getTarget());
                }
            }
        }

        // add the join gateway to wait for states before starting
        if (nodeDependencies.size() > 0) {
            return addToPreviousStep(previousStep, createParallelJoinStateGateway(nodeDependencies, new String[] { STATE_STARTED }));
        }
        return previousStep;
    }

    private static boolean isStartEventPreviousStep(WorkflowStep currentStep, String nodeId) {
        WorkflowStep previousStep = currentStep;
        while (previousStep != null) {
            if (previousStep instanceof StateUpdateEvent) {
                StateUpdateEvent sue = (StateUpdateEvent) previousStep;
                if (sue.getElementId().equals(nodeId) && sue.getState().equals(STATE_STARTED)) {
                    return true;
                }
            }
            previousStep = previousStep.getPreviousStep();
        }
        return false;
    }

    /**
     * Create the worflow for a relationship before the node actually started.
     * 
     * @param currentNode The node that is currently under process.
     * @param relationshipTemplate The relationship template for which to generate lifecycle.
     * @param previousStep The step that executes before the relationship lifecycle.
     * @param connectedNodeTemplateIds The set of all nodes that are connected to the current node.
     * @return the last step added to the workflow.
     */
    private static WorkflowStep buildRelationshipPreStartPlan(PaaSNodeTemplate currentNode, PaaSRelationshipTemplate relationshipTemplate,
            WorkflowStep previousStep, Set<String> connectedNodeTemplateIds) {
        WorkflowStep lastStep = previousStep;

        if (relationshipTemplate.instanceOf(AbstractPaaSProvider.CONNECTS_TO) || relationshipTemplate.instanceOf(AbstractPaaSProvider.HOSTED_ON)) {
            Interface configure = getLifecycleInterface(relationshipTemplate);

            // get the interface for the relationship
            if (relationshipTemplate.getSource().equals(currentNode.getId())) {
                // Source of the relationship so lets execute source
                lastStep = addToPreviousStep(
                        lastStep,
                        createOperationActivity(relationshipTemplate.getCsarPath(), configure, relationshipTemplate.getSource(), relationshipTemplate.getId(),
                                RELATIONSHIP_LIFECYCLE_INTERFACE_NAME, PRE_CONFIGURE_SOURCE));
                connectedNodeTemplateIds.add(relationshipTemplate.getRelationshipTemplate().getTarget());
            } else {
                lastStep = addToPreviousStep(
                        lastStep,
                        createOperationActivity(relationshipTemplate.getCsarPath(), configure, relationshipTemplate.getSource(), relationshipTemplate.getId(),
                                RELATIONSHIP_LIFECYCLE_INTERFACE_NAME, PRE_CONFIGURE_TARGET));
                connectedNodeTemplateIds.add(relationshipTemplate.getSource());
            }
        }
        return lastStep;
    }

    /**
     * Create the worflow for a relationship before the node actually started.
     * 
     * @param currentNode The node that is currently under process.
     * @param relationshipTemplate The relationship template for which to generate lifecycle.
     * @param previousStep The step that executes before the relationship lifecycle.
     * @return the last step added to the workflow.
     */
    private static WorkflowStep buildRelationshipPostStartPlan(PaaSNodeTemplate currentNode, PaaSRelationshipTemplate relationshipTemplate,
            WorkflowStep previousStep) {
        WorkflowStep lastStep = previousStep;

        if (relationshipTemplate.instanceOf(AbstractPaaSProvider.CONNECTS_TO) || relationshipTemplate.instanceOf(AbstractPaaSProvider.HOSTED_ON)) {
            Interface configure = getLifecycleInterface(relationshipTemplate);
            // get the interface for the relationship
            if (relationshipTemplate.getSource().equals(currentNode.getId())) {
                // Source of the relationship so lets execute source
                lastStep = lastStep.setNextStep(createOperationActivity(relationshipTemplate.getCsarPath(), configure, relationshipTemplate.getSource(),
                        relationshipTemplate.getId(), RELATIONSHIP_LIFECYCLE_INTERFACE_NAME, POST_CONFIGURE_SOURCE));
                lastStep = lastStep.setNextStep(createOperationActivity(relationshipTemplate.getCsarPath(), configure, relationshipTemplate.getSource(),
                        relationshipTemplate.getId(), RELATIONSHIP_LIFECYCLE_INTERFACE_NAME, ADD_TARGET));
            } else {
                lastStep = lastStep.setNextStep(createOperationActivity(relationshipTemplate.getCsarPath(), configure, relationshipTemplate.getSource(),
                        relationshipTemplate.getId(), RELATIONSHIP_LIFECYCLE_INTERFACE_NAME, POST_CONFIGURE_TARGET));
            }
        }
        return lastStep;
    }

    /**
     * Create the creation plan for a given node. The creation plan is a sub-plan of the build plan. It includes the creation and configuration steps for the
     * different nodes of the topology.
     * 
     * @param nodeTemplate The node template for which to build a creation plan (this should be done on a root node in a topology).
     * @return The start event for the node creation plan.
     */
    public static StartEvent buildNodeStopPlan(PaaSNodeTemplate nodeTemplate) {
        StartEvent startEvent = new StartEvent();
        buildNodeStopPlan(startEvent, nodeTemplate).setNextStep(new StopEvent());
        return startEvent;
    }

    private static WorkflowStep buildNodeStopPlan(WorkflowStep previousStep, PaaSNodeTemplate nodeTemplate) {
        // check the dependencies in order to manage startup of components in the correct order.
        Interface lifecycle = getLifecycleInterface(nodeTemplate);

        WorkflowStep lastStep = previousStep;

        for (PaaSRelationshipTemplate relationshipTemplate : nodeTemplate.getRelationshipTemplates()) {
            lastStep = buildRelationshipStopPlan(nodeTemplate, relationshipTemplate, lastStep);
        }

        // stop the node
        lastStep = lastStep.setNextStep(createOperationActivity(nodeTemplate.getCsarPath(), lifecycle, nodeTemplate.getId(), null,
                NODE_LIFECYCLE_INTERFACE_NAME, STOP_OPERATION_NAME));
        lastStep = lastStep.setNextStep(new StateUpdateEvent(nodeTemplate.getId(), STATE_STOPPED));

        // process sub-nodes
        ParallelGateway gateway = new ParallelGateway();
        gateway.setPreviousStep(lastStep);
        for (PaaSNodeTemplate child : nodeTemplate.getChildren()) {
            // start the child nodes
            buildNodeStopPlan(gateway, child);
        }
        if (gateway.getParallelSteps().size() > 0) {
            return lastStep.setNextStep(gateway);
        }
        return lastStep;
    }

    /**
     * Create the stopping workflow for the relationship.
     * 
     * TODO Currently we just call the removeTarget script on the target node. We may have to switch the call to the source as this may make more sense.
     * However this cause issues in the way we can get the instance information.
     * Note that this could be actually managed by registering on cloudify events on target node deletion. This would support also failure management and would
     * be better.
     * 
     * @param currentNode The node that is currently under process.
     * @param relationshipTemplate The relationship template for which to generate lifecycle.
     * @param previousStep The step that executes before the relationship.
     * @return the last step added to the workflow.
     */
    private static WorkflowStep buildRelationshipStopPlan(PaaSNodeTemplate currentNode, PaaSRelationshipTemplate relationshipTemplate, WorkflowStep previousStep) {
        WorkflowStep lastStep = previousStep;
        if (relationshipTemplate.instanceOf(AbstractPaaSProvider.CONNECTS_TO)) {
            Interface configure = getLifecycleInterface(relationshipTemplate);

            if (relationshipTemplate.getSource().equals(currentNode.getId())) {
                // Source of the relationship so lets execute remove target
                lastStep = lastStep.setNextStep(createOperationActivity(relationshipTemplate.getCsarPath(), configure, relationshipTemplate.getSource(),
                        relationshipTemplate.getId(), PlanGeneratorConstants.RELATIONSHIP_LIFECYCLE_INTERFACE_NAME, REMOVE_TARGET));
            }
        }
        return lastStep;
    }

    /***
     * Add the next step to the previous step. In case previous step is a parallel gateway the step will no be set as the next step but as a parallel step.
     * 
     * @param previousStep The previous step. If a parallel gateway, the next step will be added to the parallel steps list, if not append as the next step.
     * @param nextStep The step to be placed as a next step for the previous step.
     * @return The next step.
     */
    private static WorkflowStep addToPreviousStep(WorkflowStep previousStep, WorkflowStep nextStep) {
        nextStep.setPreviousStep(previousStep);
        if (previousStep instanceof ParallelGateway) {
            ((ParallelGateway) previousStep).addParallelStep(nextStep);
        } else {
            previousStep.setNextStep(nextStep);
        }
        return nextStep;
    }

    /**
     * <p>
     * Get the tosca lifecycle for a node.
     * </p>
     * <p>
     * Note that if the node has no lifecycle interface we throw an IllegalArgumentException as the plan cannot be generated if nodes doesn't uses this
     * interface.
     * </p>
     * 
     * @param nodeTemplate The node template for which to get the lifecycle interface.
     * @return The lifecycle interface as defined on the node.
     */
    private static Interface getLifecycleInterface(PaaSNodeTemplate nodeTemplate) {
        Interface lifecycle = getInterface(PlanGeneratorConstants.NODE_LIFECYCLE_INTERFACE_NAME, nodeTemplate.getIndexedNodeType().getInterfaces());
        if (lifecycle == null) {
            throw new IllegalArgumentException("Plan cannot be generated for topologies that contains nodes that doesn't inherit from tosca.nodes.Root.");
        }
        return lifecycle;
    }

    /**
     * <p>
     * Get the tosca lifecycle (configure) for a relationship.
     * </p>
     * <p>
     * Note that if the relationship has no lifecycle interface we throw an IllegalArgumentException as the plan cannot be generated if relationships doesn't
     * uses this interface.
     * </p>
     * 
     * @param relationshipTemplate The relationship template for which to get the lifecycle interface.
     * @return The lifecycle interface as defined on the relationship.
     */
    private static Interface getLifecycleInterface(PaaSRelationshipTemplate relationshipTemplate) {
        Interface configure = getInterface(RELATIONSHIP_LIFECYCLE_INTERFACE_NAME, relationshipTemplate.getIndexedRelationshipType().getInterfaces());
        if (configure == null) {
            throw new IllegalArgumentException(
                    "Plan cannot be generated for topologies that contains relationship that doesn't inherit from tosca.relationships.Root.");
        }
        return configure;
    }

    private static Interface getInterface(String targetName, Map<String, Interface> interfaces) {
        if (interfaces == null) {
            return null;
        }
        return interfaces.get(targetName);
    }
}