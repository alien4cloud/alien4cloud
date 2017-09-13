package alien4cloud.paas.wf;

import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.alien4cloud.tosca.model.definitions.Operation;
import org.alien4cloud.tosca.model.templates.NodeTemplate;
import org.alien4cloud.tosca.model.templates.RelationshipTemplate;
import org.alien4cloud.tosca.model.workflow.Workflow;
import org.alien4cloud.tosca.model.workflow.WorkflowStep;
import org.alien4cloud.tosca.model.workflow.activities.AbstractWorkflowActivity;
import org.elasticsearch.common.collect.Lists;
import org.elasticsearch.common.collect.Maps;

import alien4cloud.exception.AlreadyExistException;
import alien4cloud.paas.plan.ToscaNodeLifecycleConstants;
import alien4cloud.paas.wf.WorkflowsBuilderService.TopologyContext;
import alien4cloud.paas.wf.exception.BadWorkflowOperationException;
import alien4cloud.paas.wf.exception.InconsistentWorkflowException;
import alien4cloud.paas.wf.util.WorkflowUtils;

public abstract class AbstractWorkflowBuilder {

    public abstract void addNode(Workflow wf, String nodeId, TopologyContext toscaTypeFinder, boolean isCompute);

    public abstract void addRelationship(Workflow wf, String nodeId, NodeTemplate nodeTemplate, RelationshipTemplate RelationshipTemplate,
            TopologyContext toscaTypeFinder);

    public void removeEdge(Workflow wf, String from, String to) {
        WorkflowStep fromStep = wf.getSteps().get(from);
        if (fromStep == null) {
            throw new InconsistentWorkflowException(
                    String.format("Inconsistent workflow: a step nammed '%s' can not be found while it's referenced else where ...", from));
        }
        WorkflowStep toStep = wf.getSteps().get(to);
        if (toStep == null) {
            throw new InconsistentWorkflowException(
                    String.format("Inconsistent workflow: a step nammed '%s' can not be found while it's referenced else where ...", to));
        }
        fromStep.getOnSuccess().remove(to);
        toStep.getPrecedingSteps().remove(from);
    }

    public void connectStepFrom(Workflow wf, String stepId, String[] stepNames) {
        WorkflowStep to = wf.getSteps().get(stepId);
        if (to == null) {
            throw new InconsistentWorkflowException(
                    String.format("Inconsistent workflow: a step nammed '%s' can not be found while it's referenced else where ...", stepId));
        }
        for (String preceding : stepNames) {
            WorkflowStep precedingStep = wf.getSteps().get(preceding);
            if (precedingStep == null) {
                throw new InconsistentWorkflowException(
                        String.format("Inconsistent workflow: a step nammed '%s' can not be found while it's referenced else where ...", preceding));
            }
            WorkflowUtils.linkSteps(precedingStep, to);
        }
    }

    public void connectStepTo(Workflow wf, String stepId, String[] stepNames) {
        WorkflowStep from = wf.getSteps().get(stepId);
        if (from == null) {
            throw new InconsistentWorkflowException(
                    String.format("Inconsistent workflow: a step nammed '%s' can not be found while it's referenced else where ...", stepId));
        }
        for (String following : stepNames) {
            WorkflowStep followingStep = wf.getSteps().get(following);
            if (followingStep == null) {
                // TODO throw ex
            }
            WorkflowUtils.linkSteps(from, followingStep);
        }
    }

    protected WorkflowStep eventuallyAddStdOperationStep(Workflow wf, WorkflowStep lastStep, String nodeId, String operationName,
            TopologyContext topologyContext, boolean forceOperation) {
        NodeTemplate nodeTemplate = topologyContext.getTopology().getNodeTemplates().get(nodeId);
        // FIXME: should we browse hierarchy ?
        Operation operation = WorkflowUtils.getOperation(nodeTemplate.getType(), ToscaNodeLifecycleConstants.STANDARD, operationName, topologyContext);
        // for compute all std operations are added, for others, only those having artifacts
        if ((operation != null && operation.getImplementationArtifact() != null) || forceOperation) {
            lastStep = appendOperationStep(wf, lastStep, nodeId, ToscaNodeLifecycleConstants.STANDARD, operationName);
        }
        return lastStep;
    }

    protected NodeActivityStep appendStateStep(Workflow wf, WorkflowStep lastStep, String nodeId, String stateName) {
        NodeActivityStep step = WorkflowUtils.addStateStep(wf, nodeId, stateName);
        WorkflowUtils.linkSteps(lastStep, step);
        return step;
    }

    protected NodeActivityStep insertStateStep(Workflow wf, WorkflowStep lastStep, String nodeId, String stateName) {
        NodeActivityStep step = WorkflowUtils.addStateStep(wf, nodeId, stateName);
        WorkflowUtils.linkSteps(step, lastStep);
        return step;
    }

    protected NodeActivityStep addActivityStep(Workflow wf, String nodeId, AbstractWorkflowActivity activity) {
        NodeActivityStep step = new NodeActivityStep();
        step.setNodeId(nodeId);
        step.setActivity(activity);
        step.setName(WorkflowUtils.buildStepName(wf, step, 0));
        wf.addStep(step);
        return step;
    }

    protected NodeActivityStep appendOperationStep(Workflow wf, WorkflowStep lastStep, String nodeId, String interfaceName, String operationName) {
        NodeActivityStep step = WorkflowUtils.addOperationStep(wf, nodeId, interfaceName, operationName);
        WorkflowUtils.linkSteps(lastStep, step);
        return step;
    }

    protected NodeActivityStep insertOperationStep(Workflow wf, WorkflowStep previousStep, String nodeId, String interfaceName, String operationName) {
        NodeActivityStep step = WorkflowUtils.addOperationStep(wf, nodeId, interfaceName, operationName);
        WorkflowUtils.linkSteps(step, previousStep);
        return step;
    }

    protected void unlinkSteps(WorkflowStep from, WorkflowStep to) {
        from.removeFollowing(to.getName());
        to.removePreceding(from.getName());
    }

    protected boolean isOperationStep(NodeActivityStep defaultStep, String interfaceName, String operationName) {
        if (defaultStep.getActivity() instanceof OperationCallActivity) {
            OperationCallActivity oet = (OperationCallActivity) defaultStep.getActivity();
            if (oet.getInterfaceName().equals(interfaceName) && oet.getOperationName().equals(operationName)) {
                return true;
            }
        }
        return false;
    }

    /**
     * @param wf
     * @param relatedStepId if specified, the step will be added near this one (maybe before)
     * @param before if true, the step will be added before the relatedStepId
     * @param activity
     */
    public void addActivity(Workflow wf, String relatedStepId, boolean before, AbstractWorkflowActivity activity, TopologyContext topologyContext) {
        if (WorkflowUtils.isNativeOrSubstitutionNode(activity.getNodeId(), topologyContext)) {
            throw new BadWorkflowOperationException("Activity can not be added for abstract nodes");
        }
        if (relatedStepId != null) {
            if (before) {
                // insert
                insertActivityStep(wf, relatedStepId, activity);
            } else {
                // append
                appendActivityStep(wf, relatedStepId, activity);
            }
        } else {
            addActivityStep(wf, activity.getNodeId(), activity);
        }
    }

    public void insertActivityStep(Workflow wf, String stepId, AbstractActivity activity) {
        WorkflowStep lastStep = wf.getSteps().get(stepId);
        String stepBeforeId = null;
        if (lastStep.getPrecedingSteps() != null && lastStep.getPrecedingSteps().size() == 1) {
            stepBeforeId = lastStep.getPrecedingSteps().iterator().next();
        }
        NodeActivityStep insertedStep = addActivityStep(wf, activity.getNodeId(), activity);
        WorkflowUtils.linkSteps(insertedStep, lastStep);
        if (stepBeforeId != null) {
            WorkflowStep stepBefore = wf.getSteps().get(stepBeforeId);
            unlinkSteps(stepBefore, lastStep);
            WorkflowUtils.linkSteps(stepBefore, insertedStep);
        }
    }

    public void appendActivityStep(Workflow wf, String stepId, AbstractActivity activity) {
        WorkflowStep lastStep = wf.getSteps().get(stepId);
        String stepAfterId = null;
        if (lastStep.getFollowingSteps() != null && lastStep.getFollowingSteps().size() == 1) {
            stepAfterId = lastStep.getFollowingSteps().iterator().next();
        }
        NodeActivityStep insertedStep = addActivityStep(wf, activity.getNodeId(), activity);
        WorkflowUtils.linkSteps(lastStep, insertedStep);
        if (stepAfterId != null) {
            WorkflowStep stepAfter = wf.getSteps().get(stepAfterId);
            unlinkSteps(lastStep, stepAfter);
            WorkflowUtils.linkSteps(insertedStep, stepAfter);
        }
    }

    public void removeStep(Workflow wf, String stepId, boolean force) {
        WorkflowStep step = wf.getSteps().remove(stepId);
        if (step == null) {
            throw new InconsistentWorkflowException(
                    String.format("Inconsistent workflow: a step nammed '%s' can not be found while it's referenced else where ...", stepId));
        }
        if (!force && step instanceof NodeActivityStep && ((NodeActivityStep) step).getActivity() instanceof DelegateWorkflowActivity) {
            throw new BadWorkflowOperationException("Native steps can not be removed from workflow");
        }
        if (step.getPrecedingSteps() != null) {
            if (step.getFollowingSteps() != null) {
                // connect all preceding to all following
                for (String precedingId : step.getPrecedingSteps()) {
                    WorkflowStep preceding = wf.getSteps().get(precedingId);
                    for (String followingId : step.getFollowingSteps()) {
                        WorkflowStep following = wf.getSteps().get(followingId);
                        WorkflowUtils.linkSteps(preceding, following);
                    }
                }
            }
            for (Object precedingId : step.getPrecedingSteps().toArray()) {
                WorkflowStep preceding = wf.getSteps().get(precedingId);
                unlinkSteps(preceding, step);
            }
        }
        if (step.getFollowingSteps() != null) {
            for (Object followingId : step.getFollowingSteps().toArray()) {
                WorkflowStep following = wf.getSteps().get(followingId);
                unlinkSteps(step, following);
            }
        }
    }

    public void renameStep(Workflow wf, String stepId, String newStepName) {
        if (wf.getSteps().containsKey(newStepName)) {
            throw new AlreadyExistException(String.format("A step named ''{0}'' already exists in workflow '%s'", newStepName, wf.getName()));
        }
        WorkflowStep step = wf.getSteps().remove(stepId);
        step.setName(newStepName);
        wf.getSteps().put(newStepName, step);
        // now explore the links
        if (step.getPrecedingSteps() != null) {
            for (String precedingId : step.getPrecedingSteps()) {
                WorkflowStep precedingStep = wf.getSteps().get(precedingId);
                precedingStep.getFollowingSteps().remove(stepId);
                precedingStep.getFollowingSteps().add(newStepName);
            }
        }
        if (step.getFollowingSteps() != null) {
            for (String followingId : step.getFollowingSteps()) {
                WorkflowStep followingStep = wf.getSteps().get(followingId);
                followingStep.getPrecedingSteps().remove(stepId);
                followingStep.getPrecedingSteps().add(newStepName);
            }
        }
    }

    public void removeNode(Workflow wf, String nodeName) {
        WorkflowStep[] steps = new WorkflowStep[wf.getSteps().size()];
        steps = wf.getSteps().values().toArray(steps);
        for (WorkflowStep step : steps) {
            if (step instanceof NodeActivityStep && ((NodeActivityStep) step).getNodeId().equals(nodeName)) {
                removeStep(wf, step.getName(), true);
            }
        }
    }

    /**
     * When a relationship is removed, we remove all links between src and target.
     * <p>
     * TODO : a better implem should be to just remove the links that rely to this relationship. But to do this, we have to associate the link with the
     * relationship (when the link is created consecutively to a relationship add).
     * 
     * @param wf
     * @param relationhipTarget
     */
    public void removeRelationship(Workflow wf, String nodeId, String relationhipTarget) {
        Iterator<WorkflowStep> steps = wf.getSteps().values().iterator();
        while (steps.hasNext()) {
            WorkflowStep step = steps.next();
            if (step instanceof NodeActivityStep && ((NodeActivityStep) step).getNodeId().equals(nodeId)) {
                if (step.getFollowingSteps() != null) {
                    Object[] followingStepIds = step.getFollowingSteps().toArray();
                    for (Object followingId : followingStepIds) {
                        WorkflowStep followingStep = wf.getSteps().get(followingId);
                        if (followingStep instanceof NodeActivityStep && ((NodeActivityStep) followingStep).getNodeId().equals(relationhipTarget)) {
                            unlinkSteps(step, followingStep);
                        }
                    }
                }
                if (step.getPrecedingSteps() != null) {
                    Object precedings[] = step.getPrecedingSteps().toArray();
                    for (Object precedingId : precedings) {
                        WorkflowStep precedingStep = wf.getSteps().get(precedingId);
                        if (precedingStep instanceof NodeActivityStep && ((NodeActivityStep) precedingStep).getNodeId().equals(relationhipTarget)) {
                            unlinkSteps(precedingStep, step);
                        }
                    }
                }
            }
        }
    }

    /**
     * Swap steps means:
     * <ul>
     * <li>The connection between step and target is inverted.
     * <li>All step's predecessors become predecessors of target & vice versa
     * <li>All step's followers become followers of target & vice versa
     * </ul>
     * That's all folks !
     */
    public void swapSteps(Workflow wf, String stepId, String targetId) {
        WorkflowStep step = wf.getSteps().get(stepId);
        WorkflowStep target = wf.getSteps().get(targetId);
        unlinkSteps(step, target);
        List<WorkflowStep> stepPredecessors = removePredecessors(wf, step);
        List<WorkflowStep> stepFollowers = removeFollowers(wf, step);
        List<WorkflowStep> targetPredecessors = removePredecessors(wf, target);
        List<WorkflowStep> targetFollowers = removeFollowers(wf, target);
        associateFollowers(step, targetFollowers);
        associateFollowers(target, stepFollowers);
        associatePredecessors(step, targetPredecessors);
        associatePredecessors(target, stepPredecessors);
        WorkflowUtils.linkSteps(target, step);
    }

    private void associatePredecessors(WorkflowStep step, List<WorkflowStep> stepPredecessors) {
        for (WorkflowStep predecessor : stepPredecessors) {
            WorkflowUtils.linkSteps(predecessor, step);
        }
    }

    private void associateFollowers(WorkflowStep step, List<WorkflowStep> stepFollowers) {
        for (WorkflowStep follower : stepFollowers) {
            WorkflowUtils.linkSteps(step, follower);
        }
    }

    private List<WorkflowStep> removePredecessors(Workflow wf, WorkflowStep step) {
        List<WorkflowStep> result = Lists.newArrayList();
        if (step.getPrecedingSteps() == null || step.getPrecedingSteps().size() == 0) {
            return result;
        }
        Object precedings[] = step.getPrecedingSteps().toArray();
        for (Object precedingId : precedings) {
            WorkflowStep precedingStep = wf.getSteps().get(precedingId);
            unlinkSteps(precedingStep, step);
            result.add(precedingStep);
        }
        return result;
    }

    private List<WorkflowStep> removeFollowers(Workflow wf, WorkflowStep step) {
        List<WorkflowStep> result = Lists.newArrayList();
        if (step.getFollowingSteps() == null || step.getFollowingSteps().size() == 0) {
            return result;
        }
        Object followings[] = step.getFollowingSteps().toArray();
        for (Object followingId : followings) {
            WorkflowStep followingStep = wf.getSteps().get(followingId);
            unlinkSteps(step, followingStep);
            result.add(followingStep);
        }
        return result;
    }

    public void renameNode(Workflow wf, String oldName, String newName) {
        if (wf.getSteps() != null) {
            for (WorkflowStep step : wf.getSteps().values()) {
                if (step instanceof NodeActivityStep && ((NodeActivityStep) step).getNodeId().equals(oldName)) {
                    ((NodeActivityStep) step).setNodeId(newName);
                    ((NodeActivityStep) step).getActivity().setNodeId(newName);
                }
            }
        }
    }

    public Workflow reinit(Workflow wf, TopologyContext toscaTypeFinder) {
        Map<String, WorkflowStep> steps = Maps.newHashMap();
        wf.setSteps(steps);
        if (toscaTypeFinder.getTopology().getNodeTemplates() != null) {
            // first stage : add the nodes
            for (Entry<String, NodeTemplate> entry : toscaTypeFinder.getTopology().getNodeTemplates().entrySet()) {
                String nodeId = entry.getKey();
                boolean forceOperation = WorkflowUtils.isComputeOrVolume(nodeId, toscaTypeFinder);
                addNode(wf, nodeId, toscaTypeFinder, forceOperation);
            }
            // second stage : add the relationships
            for (Entry<String, NodeTemplate> entry : toscaTypeFinder.getTopology().getNodeTemplates().entrySet()) {
                String nodeId = entry.getKey();
                if (entry.getValue().getRelationships() != null) {
                    for (RelationshipTemplate relationshipTemplate : entry.getValue().getRelationships().values()) {
                        addRelationship(wf, nodeId, entry.getValue(), relationshipTemplate, toscaTypeFinder);
                    }
                }
            }
        }
        return wf;
    }

}
