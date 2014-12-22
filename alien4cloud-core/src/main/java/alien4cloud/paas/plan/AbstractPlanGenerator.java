package alien4cloud.paas.plan;

import static alien4cloud.paas.plan.ToscaRelationshipLifecycleConstants.ROOT;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang3.ArrayUtils;

import alien4cloud.paas.model.PaaSNodeTemplate;
import alien4cloud.paas.model.PaaSRelationshipTemplate;
import alien4cloud.tosca.model.Interface;
import alien4cloud.tosca.model.Operation;
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
        for(PaaSNodeTemplate node : nodes) {
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
     * Wait for all target nodes of relationships to reach the expected state.
     *
     * @param nodeTemplate The node that should wait for it's relationships target to reach the given state.
     * @param relationshipType The type that relationship must derive from (or be) in order for the wait to be applied.
     * @param states The states to reach.
     */
    protected void waitTarget(PaaSNodeTemplate nodeTemplate, String relationshipType, String... states) {
        wait(nodeTemplate, relationshipType, true, states);
    }

    /**
     * Wait for all source nodes of relationships to reach the expected state.
     *
     * @param nodeTemplate The node that should wait for it's relationships source to reach the given state.
     * @param relationshipType The type that relationship must derive from (or be) in order for the wait to be applied.
     * @param states The states to reach.
     */
    protected void waitSource(PaaSNodeTemplate nodeTemplate, String relationshipType, String... states) {
        wait(nodeTemplate, relationshipType, false, states);
    }

    private void wait(PaaSNodeTemplate nodeTemplate, String relationshipType, boolean waitTarget, String... states) {
        Set<String> nodeDependencies = Sets.newHashSet();
        // for all relationships (processed in order)
        for (PaaSRelationshipTemplate relationshipTemplate : nodeTemplate.getRelationshipTemplates()) {
            boolean isCandidate = waitTarget ? relationshipTemplate.getSource().equals(nodeTemplate.getId()) : relationshipTemplate.getRelationshipTemplate()
                    .getTarget().equals(nodeTemplate.getId());
            String dependencyTarget = waitTarget ? relationshipTemplate.getRelationshipTemplate().getTarget() : relationshipTemplate.getSource();

            // if the sate is already reached by synchronous implementation we don't have to generate it here.
            if (relationshipTemplate.instanceOf(relationshipType) && isCandidate && !isStateSyncPrevious(dependencyTarget, states)) {
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
     * @param sourceOperation The operation to be triggered if the node is source.
     * @param targetOperation The operation to be triggered if the node is target.
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
        Operation operation = interfaz.getOperations().get(operationName);
        if (operation == null || operation.getImplementationArtifact() == null) {
            // if there is no implementation for the requested operation we just don't generate a step.
            return;
        }

        OperationCallActivity activity = new OperationCallActivity();
        activity.setCsarPath(csarPath);
        activity.setInterfaceName(interfaceName);
        activity.setOperationName(operationName);
        activity.setNodeTemplateId(nodeTemplateId);
        activity.setRelationshipId(relationshipId);
        activity.setImplementationArtifact(operation.getImplementationArtifact());
        activity.setInputParameters(operation.getInputParameters());
        next(activity);
    }

    private static Interface getNodeInterface(PaaSNodeTemplate nodeTemplate, String interfaceName) {
        Interface interfaz = getInterface(interfaceName, nodeTemplate.getIndexedNodeType().getInterfaces());
        if (interfaz == null) {
            throw new IllegalArgumentException("Plan cannot be generated as required interface <" + interfaceName + "> has not been found on node <"
                    + nodeTemplate.getNodeTemplate().getName() + "> from type <" + nodeTemplate.getNodeTemplate().getType() + ">.");
        }
        return interfaz;
    }

    private static Interface getRelationshipInterface(PaaSRelationshipTemplate relationshipTemplate, String interfaceName) {
        Interface interfaz = getInterface(interfaceName, relationshipTemplate.getIndexedRelationshipType().getInterfaces());
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
}