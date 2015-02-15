package alien4cloud.paas.plan;

import static alien4cloud.tosca.normative.NormativeRelationshipConstants.ROOT;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Set;

import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;

import org.apache.commons.lang3.ArrayUtils;

import alien4cloud.model.components.Interface;
import alien4cloud.model.components.Operation;
import alien4cloud.paas.model.PaaSNodeTemplate;
import alien4cloud.paas.model.PaaSRelationshipTemplate;
import alien4cloud.utils.MapUtil;

import com.google.common.collect.Sets;

/**
 * Abstract class used to generate plans.
 */
public abstract class AbstractPlanGenerator {
    // last processed step.
    private WorkflowStep lastStep;

    /**
     * Generate a plan for a nodes hierarchy.
     *
     * @return The start workflow step.
     */
    public StartEvent generate(List<PaaSNodeTemplate> roots) {
        StartEvent startEvent = new StartEvent();
        lastStep = startEvent;
        parallel(roots);
        return startEvent;
    }

    /**
     * Generate a plan for a single node hierarchy.
     *
     * @param node The node for which to generate the plan.
     * @return The start workflow step.
     */
    public StartEvent generate(PaaSNodeTemplate node) {
        StartEvent startEvent = new StartEvent();
        lastStep = startEvent;
        generateNodeWorkflow(node);
        return startEvent;
    }

    protected abstract void generateNodeWorkflow(PaaSNodeTemplate node);

    /**
     * process generation of the given nodes in sequence.
     *
     * @param nodes The nodes to generate.
     */
    protected void sequencial(List<PaaSNodeTemplate> nodes) {
        for (PaaSNodeTemplate node : nodes) {
            generateNodeWorkflow(node);
        }
    }

    /**
     * process generation of the given nodes in parallel.
     *
     * @param nodes The nodes to generate.
     */
    protected void parallel(List<PaaSNodeTemplate> nodes) {
        WorkflowStep previousStep = lastStep;
        ParallelGateway gateway = new ParallelGateway();
        gateway.setPreviousStep(previousStep);
        for (PaaSNodeTemplate node : nodes) {
            lastStep = gateway;
            generateNodeWorkflow(node);
        }
        if (gateway.getParallelSteps().size() > 1) {
            lastStep = previousStep.setNextStep(gateway);
        } else if (gateway.getParallelSteps().size() == 1) {
            // just skip the gateway
            previousStep.setNextStep(gateway.getParallelSteps().get(0));
        } else {
            lastStep = previousStep;
        }
    }

    /**
     * Change the state of a node.
     *
     * @param id Id of the template for which to change the state.
     * @param state The new state for the node template.
     */
    protected void state(String id, String state) {
        next(new StateUpdateEvent(id, state));
    }

    /**
     * Call an operation on a node.
     *
     * @param nodeTemplate The node template on which to execute the operation.
     * @param interfaceName The interface name.
     * @param operationName The operation name.
     */
    protected void call(PaaSNodeTemplate nodeTemplate, String interfaceName, String operationName) {
        Interface lifecycle = getNodeInterface(nodeTemplate, interfaceName);
        callOperation(nodeTemplate.getCsarPath(), lifecycle, nodeTemplate.getId(), null, interfaceName, operationName);
    }

    /**
     * Call an operation on a relationship.
     *
     * @param relationshipTemplate The relationship template on which to execute the operation.
     * @param interfaceName The interface name.
     * @param operation The operation name.
     */
    protected void call(PaaSRelationshipTemplate relationshipTemplate, String interfaceName, String operation) {
        Interface lifecycle = getRelationshipInterface(relationshipTemplate, interfaceName);
        callOperation(relationshipTemplate.getCsarPath(), lifecycle, relationshipTemplate.getSource(), relationshipTemplate.getId(), interfaceName, operation);
    }

    /**
     * Trigger the execution of an operation on a relationship.
     *
     * @param relationshipTemplate The relationship template on which to trigger the execution of the operation.
     * @param interfaceName The interface name.
     * @param mainMember The main member of the relationship to process (source or target).
     * @param sideMember The other member of the relationship to process(target if the main is source, source if not)
     */
    protected void trigger(PaaSRelationshipTemplate relationshipTemplate, String interfaceName, RelationshipMember mainMember, RelationshipMember sideMember) {
        Interface interfaz = getRelationshipInterface(relationshipTemplate, interfaceName);
        triggerOperation(relationshipTemplate.getCsarPath(), interfaz, interfaceName, relationshipTemplate.getId(), mainMember, sideMember);
        // triggerOperation(relationshipTemplate.getCsarPath(), interfaz, nodeId, relationshipTemplate.getId(), interfaceName, operation, sideOperation);
    }

    /**
     * Wait for all target nodes of relationships (except if target and source are the same node) to reach the expected state.
     *
     * @param nodeTemplate The node that should wait for it's relationships target to reach the given state.
     * @param relationshipType The type that relationship must derive from (or be) in order for the wait to be applied.
     * @param states The states to reach.
     */
    protected void waitTarget(PaaSNodeTemplate nodeTemplate, String relationshipType, String... states) {
        wait(nodeTemplate, relationshipType, true, false, states);
    }

    /**
     * Wait for all target nodes of relationships (only if target and source are the same node) to reach the expected state.
     *
     * @param nodeTemplate The node that should wait for it's relationships target to reach the given state.
     * @param relationshipType The type that relationship must derive from (or be) in order for the wait to be applied.
     * @param states The states to reach.
     */
    protected void waitMyself(PaaSNodeTemplate nodeTemplate, String relationshipType, String... states) {
        wait(nodeTemplate, relationshipType, true, true, states);
    }

    /**
     * Wait for all source nodes of relationships to reach the expected state.
     *
     * @param nodeTemplate The node that should wait for it's relationships source to reach the given state.
     * @param relationshipType The type that relationship must derive from (or be) in order for the wait to be applied.
     * @param states The states to reach.
     */
    protected void waitSource(PaaSNodeTemplate nodeTemplate, String relationshipType, String... states) {
        wait(nodeTemplate, relationshipType, false, false, states);
    }

    private void wait(PaaSNodeTemplate nodeTemplate, String relationshipType, boolean waitTarget, boolean myselfOnly, String... states) {
        Set<String> nodeDependencies = Sets.newHashSet();
        // for all relationships (processed in order)
        for (PaaSRelationshipTemplate relationshipTemplate : nodeTemplate.getRelationshipTemplates()) {
            boolean isCandidate = waitTarget ? relationshipTemplate.getSource().equals(nodeTemplate.getId()) : relationshipTemplate.getRelationshipTemplate()
                    .getTarget().equals(nodeTemplate.getId());
            String dependencyTarget = waitTarget ? relationshipTemplate.getRelationshipTemplate().getTarget() : relationshipTemplate.getSource();
            boolean waitMySelf = dependencyTarget.equals(nodeTemplate.getId());
            if (waitMySelf && !myselfOnly) {
                isCandidate = false;
            }
            if (myselfOnly && !waitMySelf) {
                isCandidate = false;
            }

            // if the sate is already reached by synchronous implementation we don't have to generate it here.
            if (isCandidate && relationshipTemplate.instanceOf(relationshipType) && !isStateSyncPrevious(dependencyTarget, states)) {
                nodeDependencies.add(dependencyTarget);
            }
        }

        // add the join gateway to wait for states before starting
        if (nodeDependencies.size() > 0) {
            next(createParallelJoinStateGateway(nodeDependencies, states));
        }
    }

    /**
     * If one of the given states is synchronously before in the workflow this returns true (if so there is no need to wait as the state MUST have been
     * reached).
     *
     * @param nodeId The id of the node that is supposed to reach one of the expected states.
     * @param states The expected states.
     * @return True if the state exists in the synchronous workflow line.
     */
    private boolean isStateSyncPrevious(String nodeId, String... states) {
        WorkflowStep previousStep = lastStep;
        while (previousStep != null) {
            if (previousStep instanceof StateUpdateEvent) {
                StateUpdateEvent sue = (StateUpdateEvent) previousStep;
                if (sue.getElementId().equals(nodeId) && ArrayUtils.contains(states, sue.getState())) {
                    return true;
                }
            }
            previousStep = previousStep.getPreviousStep();
        }
        return false;
    }

    /**
     * Call operations from the node relationships.
     *
     * @param nodeTemplate The node that is source or target of the relations.
     * @param interfaceName The interface that holds the operations.
     * @param sourceOperation The operation to be executed if the node is source.
     * @param targetOperation The operation to be executed if the node is target.
     */
    protected void callRelations(PaaSNodeTemplate nodeTemplate, String interfaceName, String sourceOperation, String targetOperation) {
        for (PaaSRelationshipTemplate relationshipTemplate : nodeTemplate.getRelationshipTemplates()) {
            if (relationshipTemplate.instanceOf(ROOT)) {
                if (relationshipTemplate.getSource().equals(nodeTemplate.getId())) {
                    call(relationshipTemplate, interfaceName, sourceOperation);
                } else {
                    call(relationshipTemplate, interfaceName, targetOperation);
                }
            }
        }
    }

    /**
     * Trigger the call of operations from the node relationships.
     *
     * @param nodeTemplate The node that is source or target of the relations.
     * @param interfaceName The interface that holds the operations.
     * @param sourceOperation The operation to be triggered if the node is source.
     * @param targetOperation The operation to be triggered if the node is target.
     */
    protected void triggerRelations(PaaSNodeTemplate nodeTemplate, String interfaceName, String sourceOperation, String targetOperation) {
        for (PaaSRelationshipTemplate relationshipTemplate : nodeTemplate.getRelationshipTemplates()) {
            if (relationshipTemplate.instanceOf(ROOT)) {
                RelationshipMember source = new RelationshipMember(relationshipTemplate.getSource(), sourceOperation);
                RelationshipMember target = new RelationshipMember(relationshipTemplate.getRelationshipTemplate().getTarget(), targetOperation);
                if (source.nodeId.equals(nodeTemplate.getId())) {
                    trigger(relationshipTemplate, interfaceName, source, target);
                } else {
                    trigger(relationshipTemplate, interfaceName, target, source);
                }
            }
        }
    }

    private void next(WorkflowStep nextStep) {
        nextStep.setPreviousStep(lastStep);
        if (lastStep instanceof ParallelGateway) {
            ((ParallelGateway) lastStep).addParallelStep(nextStep);
        } else {
            lastStep.setNextStep(nextStep);
        }
        lastStep = nextStep;
    }

    private WorkflowStep createParallelJoinStateGateway(Set<String> targets, String[] expectedStates) {
        String[] keys = targets.toArray(new String[targets.size()]);
        String[][] values = new String[keys.length][];
        for (int i = 0; i < values.length; i++) {
            values[i] = expectedStates;
        }
        return new ParallelJoinStateGateway(MapUtil.newHashMap(keys, values));
    }

    private void callOperation(Path csarPath, Interface interfaz, String nodeTemplateId, String relationshipId, String interfaceName, String operationName) {
        Operation operation = operationName != null ? interfaz.getOperations().get(operationName) : null;
        if (operation == null || operation.getImplementationArtifact() == null) {
            // if there is no implementation for the requested operation we just don't generate a step.
            return;
        }

        OperationCallActivity activity = new OperationCallActivity();
        fillOperationCallActivity(activity, csarPath, nodeTemplateId, relationshipId, interfaceName, operationName, operation);
        next(activity);
    }

    private void triggerOperation(final Path csarPath, final Interface interfaz, final String interfaceName, final String relationshipId,
            final RelationshipMember mainMember, final RelationshipMember sideMember) {

        final Operation operation = interfaz.getOperations().get(mainMember.operation);
        final Operation sideOperation = interfaz.getOperations().get(sideMember.operation);
        final boolean processMain = !(operation == null || operation.getImplementationArtifact() == null);
        final boolean processSide = !(sideOperation == null || sideOperation.getImplementationArtifact() == null);
        if (!processMain && !processSide) {
            // if there is no implementation neither for the requested operation, nor for the side one, we just don't generate a step.
            return;
        }

        RelationshipTriggerEvent activity = new RelationshipTriggerEvent();
        activity.setCsarPath(csarPath);
        activity.setRelationshipId(relationshipId);
        activity.setInterfaceName(interfaceName);
        activity.setNodeTemplateId(mainMember.nodeId);
        activity.setSideNodeTemplateId(sideMember.nodeId);

        if (processMain) {
            activity.setOperationName(mainMember.operation);
        }

        if (processSide) {
            activity.setSideOperationName(sideMember.operation);
            activity.setSideOperationImplementationArtifact(sideOperation.getImplementationArtifact());
            activity.setSideInputParameters(sideOperation.getInputParameters());
        }

        next(activity);
    }

    private void fillOperationCallActivity(OperationCallActivity activity, Path csarPath, String nodeTemplateId, String relationshipId, String interfaceName,
            String operationName, Operation operation) {
        activity.setCsarPath(csarPath);
        activity.setInterfaceName(interfaceName);
        activity.setOperationName(operationName);
        activity.setNodeTemplateId(nodeTemplateId);
        activity.setRelationshipId(relationshipId);
        activity.setImplementationArtifact(operation.getImplementationArtifact());
        activity.setInputParameters(operation.getInputParameters());
    }

    private static Interface getNodeInterface(PaaSNodeTemplate nodeTemplate, String interfaceName) {
        Interface interfaz = getInterface(interfaceName, nodeTemplate.getIndexedToscaElement().getInterfaces());
        if (interfaz == null) {
            throw new IllegalArgumentException("Plan cannot be generated as required interface <" + interfaceName + "> has not been found on node <"
                    + nodeTemplate.getNodeTemplate().getName() + "> from type <" + nodeTemplate.getNodeTemplate().getType() + ">.");
        }
        return interfaz;
    }

    private static Interface getRelationshipInterface(PaaSRelationshipTemplate relationshipTemplate, String interfaceName) {
        Interface interfaz = getInterface(interfaceName, relationshipTemplate.getIndexedToscaElement().getInterfaces());
        if (interfaz == null) {
            throw new IllegalArgumentException("Plan cannot be generated as required interface <" + interfaceName + "> has not been found on relationship <"
                    + relationshipTemplate.getId() + "> from type <" + relationshipTemplate.getRelationshipTemplate().getType() + "> from source node <"
                    + relationshipTemplate.getSource() + ">.");
        }
        return interfaz;
    }

    private static Interface getInterface(String interfaceName, Map<String, Interface> interfaces) {
        return interfaces == null ? null : interfaces.get(interfaceName);
    }

    @NoArgsConstructor
    @AllArgsConstructor
    protected class RelationshipMember {
        String nodeId;
        String operation;
    }
}