package alien4cloud.paas.wf;

import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.elasticsearch.common.collect.Lists;

import alien4cloud.exception.AlreadyExistException;
import alien4cloud.model.components.Interface;
import alien4cloud.model.components.Operation;
import alien4cloud.paas.model.PaaSNodeTemplate;
import alien4cloud.paas.model.PaaSRelationshipTemplate;
import alien4cloud.paas.model.PaaSTopology;
import alien4cloud.paas.plan.ToscaNodeLifecycleConstants;
import alien4cloud.paas.wf.exception.InconsistentWorkflowException;
import alien4cloud.paas.wf.util.WorkflowUtils;

public abstract class AbstractWorkflowBuilder {

    public abstract void addNode(Workflow wf, PaaSTopology paaSTopology, PaaSNodeTemplate paaSNodeTemplate, boolean isCompute);

    public abstract void addRelationship(Workflow wf, PaaSTopology paaSTopology, PaaSNodeTemplate paaSNodeTemplate,
            PaaSRelationshipTemplate pasSRelationshipTemplate);

    public void removeEdge(Workflow wf, PaaSTopology paaSTopology, String from, String to) {
        AbstractStep fromStep = wf.getSteps().get(from);
        if (fromStep == null) {
            throw new InconsistentWorkflowException(String.format(
                    "Inconsistent workflow: a step nammed '%s' can not be found while it's referenced else where ...", from));
        }
        AbstractStep toStep = wf.getSteps().get(to);
        if (toStep == null) {
            throw new InconsistentWorkflowException(String.format(
                    "Inconsistent workflow: a step nammed '%s' can not be found while it's referenced else where ...", to));
        }
        fromStep.getFollowingSteps().remove(to);
        toStep.getPrecedingSteps().remove(from);
    }

    /**
     * Compute the wf in order to ensure that all step are tagged with the hostId property.
     * <p/>
     * The hostId is the first (and normally unique) compute found in the ascendency.
     */
    public void fillHostId(Workflow wf, PaaSTopology paaSTopology) {
        wf.getHosts().clear();
        for (AbstractStep step : wf.getSteps().values()) {
            if (step instanceof NodeActivityStep) {
                NodeActivityStep dstep = (NodeActivityStep) step;
                dstep.setHostId(null);
            }
        }
        for (PaaSNodeTemplate paaSNodeTemplate : paaSTopology.getComputes()) {
            String hostId = paaSNodeTemplate.getId();
            Set<String> allChildren = getAllChildrenHierarchy(paaSNodeTemplate);
            for (AbstractStep step : wf.getSteps().values()) {
                if (step instanceof NodeActivityStep) {
                    NodeActivityStep dstep = (NodeActivityStep) step;
                    if (allChildren.contains(dstep.getNodeId())) {
                        dstep.setHostId(hostId);
                        if (!wf.getHosts().contains(hostId)) {
                            wf.getHosts().add(hostId);
                        }
                    }
                }
            }
        }
    }

    public void connectStepFrom(Workflow wf, PaaSTopology paaSTopology, String stepId, String[] stepNames) {
        AbstractStep to = wf.getSteps().get(stepId);
        if (to == null) {
            throw new InconsistentWorkflowException(String.format(
                    "Inconsistent workflow: a step nammed '%s' can not be found while it's referenced else where ...", stepId));
        }
        for (String preceding : stepNames) {
            AbstractStep precedingStep = wf.getSteps().get(preceding);
            if (precedingStep == null) {
                throw new InconsistentWorkflowException(String.format(
                        "Inconsistent workflow: a step nammed '%s' can not be found while it's referenced else where ...", preceding));
            }
            WorkflowUtils.linkSteps(precedingStep, to);
        }
    }

    public void connectStepTo(Workflow wf, PaaSTopology paaSTopology, String stepId, String[] stepNames) {
        AbstractStep from = wf.getSteps().get(stepId);
        if (from == null) {
            throw new InconsistentWorkflowException(String.format(
                    "Inconsistent workflow: a step nammed '%s' can not be found while it's referenced else where ...", stepId));
        }
        for (String following : stepNames) {
            AbstractStep followingStep = wf.getSteps().get(following);
            if (followingStep == null) {
                // TODO throw ex
            }
            WorkflowUtils.linkSteps(from, followingStep);
        }
    }

    private Set<String> getAllChildrenHierarchy(PaaSNodeTemplate paaSNodeTemplate) {
        Set<String> nodeIds = new HashSet<String>();
        recursivelyPopulateChildrenHierarchy(paaSNodeTemplate, nodeIds);
        return nodeIds;
    }

    private void recursivelyPopulateChildrenHierarchy(PaaSNodeTemplate paaSNodeTemplate, Set<String> nodeIds) {
        nodeIds.add(paaSNodeTemplate.getId());
        List<PaaSNodeTemplate> children = paaSNodeTemplate.getChildren();
        if (children != null) {
            for (PaaSNodeTemplate child : children) {
                recursivelyPopulateChildrenHierarchy(child, nodeIds);
            }
        }
    }

    protected AbstractStep eventuallyAddStdOperationStep(Workflow wf, AbstractStep lastStep, PaaSNodeTemplate paaSNodeTemplate, String operationName,
            boolean isCompute) {
        Interface lifecycle = WorkflowUtils.getNodeInterface(paaSNodeTemplate, ToscaNodeLifecycleConstants.STANDARD);
        Operation operation = lifecycle.getOperations().get(operationName);
        // for compute all std operations are added, for others, only those having artifacts
        if ((operation != null && operation.getImplementationArtifact() != null) || isCompute) {
            lastStep = appendOperationStep(wf, lastStep, paaSNodeTemplate, ToscaNodeLifecycleConstants.STANDARD, operationName);
        }
        return lastStep;
    }

    protected NodeActivityStep appendStateStep(Workflow wf, AbstractStep lastStep, PaaSNodeTemplate paaSNodeTemplate, String stateName) {
        NodeActivityStep step = addStateStep(wf, paaSNodeTemplate, stateName);
        WorkflowUtils.linkSteps(lastStep, step);
        return step;
    }

    protected NodeActivityStep insertStateStep(Workflow wf, AbstractStep lastStep, PaaSNodeTemplate paaSNodeTemplate, String stateName) {
        NodeActivityStep step = addStateStep(wf, paaSNodeTemplate, stateName);
        WorkflowUtils.linkSteps(step, lastStep);
        return step;
    }

    protected NodeActivityStep addStateStep(Workflow wf, PaaSNodeTemplate paaSNodeTemplate, String stateName) {
        SetStateActivity task = new SetStateActivity();
        task.setStateName(stateName);
        task.setNodeId(paaSNodeTemplate.getId());
        NodeActivityStep step = new NodeActivityStep();
        step.setNodeId(paaSNodeTemplate.getId());
        step.setActivity(task);
        step.setName(WorkflowUtils.buildStepName(wf, step, 0));
        wf.addStep(step);
        return step;
    }

    protected NodeActivityStep addActivityStep(Workflow wf, PaaSNodeTemplate paaSNodeTemplate, AbstractActivity activity) {
        NodeActivityStep step = new NodeActivityStep();
        step.setNodeId(paaSNodeTemplate.getId());
        step.setActivity(activity);
        step.setName(WorkflowUtils.buildStepName(wf, step, 0));
        wf.addStep(step);
        return step;
    }

    protected NodeActivityStep appendOperationStep(Workflow wf, AbstractStep lastStep, PaaSNodeTemplate paaSNodeTemplate, String interfaceName, String operationName) {
        NodeActivityStep step = addOperationStep(wf, paaSNodeTemplate, interfaceName, operationName);
        WorkflowUtils.linkSteps(lastStep, step);
        return step;
    }

    protected NodeActivityStep addOperationStep(Workflow wf, PaaSNodeTemplate paaSNodeTemplate, String interfaceName,
            String operationName) {
        OperationCallActivity task = new OperationCallActivity();
        task.setInterfaceName(interfaceName);
        task.setOperationName(operationName);
        task.setNodeId(paaSNodeTemplate.getId());
        NodeActivityStep step = new NodeActivityStep();
        step.setNodeId(paaSNodeTemplate.getId());
        step.setActivity(task);
        step.setName(WorkflowUtils.buildStepName(wf, step, 0));
        wf.addStep(step);
        return step;
    }

    protected NodeActivityStep insertOperationStep(Workflow wf, AbstractStep previousStep, PaaSNodeTemplate paaSNodeTemplate, String interfaceName,
            String operationName) {
        NodeActivityStep step = addOperationStep(wf, paaSNodeTemplate, interfaceName, operationName);
        WorkflowUtils.linkSteps(step, previousStep);
        return step;
    }

    protected void unlinkSteps(AbstractStep from, AbstractStep to) {
        from.removeFollowing(to.getName());
        to.removePreceding(from.getName());
    }

    protected NodeActivityStep getStateStepByNode(Workflow wf, String nodeName, String stateName) {
        for (AbstractStep step : wf.getSteps().values()) {
            if (step instanceof NodeActivityStep) {
                NodeActivityStep defaultStep = (NodeActivityStep) step;
                if (defaultStep.getActivity().getNodeId().equals(nodeName) && isStateStep(defaultStep, stateName)) {
                    return defaultStep;
                }
            }
        }
        return null;
    }

    protected boolean isStateStep(NodeActivityStep defaultStep, String stateName) {
        if (defaultStep.getActivity() instanceof SetStateActivity && ((SetStateActivity) defaultStep.getActivity()).getStateName().equals(stateName)) {
            return true;
        }
        return false;
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

    public void addActivity(Workflow wf, PaaSTopology paaSTopology, String relatedStepId, boolean before, AbstractActivity activity) {
        if (relatedStepId != null) {
            if (before) {
                // insert
                insertActivityStep(wf, paaSTopology, relatedStepId, activity);
            } else {
                // append
                appendActivityStep(wf, paaSTopology, relatedStepId, activity);
            }
        } else {
            PaaSNodeTemplate paaSNodeTemplate = paaSTopology.getAllNodes().get(activity.getNodeId());
            addActivityStep(wf, paaSNodeTemplate, activity);
        }
    }

    public void insertActivityStep(Workflow wf, PaaSTopology paaSTopology, String stepId, AbstractActivity activity) {
        AbstractStep lastStep = wf.getSteps().get(stepId);
        String stepBeforeId = null;
        if (lastStep.getPrecedingSteps() != null && lastStep.getPrecedingSteps().size() == 1) {
            stepBeforeId = lastStep.getPrecedingSteps().iterator().next();
        }
        PaaSNodeTemplate paaSNodeTemplate = paaSTopology.getAllNodes().get(activity.getNodeId());
        NodeActivityStep insertedStep = addActivityStep(wf, paaSNodeTemplate, activity);
        WorkflowUtils.linkSteps(insertedStep, lastStep);
        if (stepBeforeId != null) {
            AbstractStep stepBefore = wf.getSteps().get(stepBeforeId);
            unlinkSteps(stepBefore, lastStep);
            WorkflowUtils.linkSteps(stepBefore, insertedStep);
        }
    }

    public void appendActivityStep(Workflow wf, PaaSTopology paaSTopology, String stepId, AbstractActivity activity) {
        AbstractStep lastStep = wf.getSteps().get(stepId);
        PaaSNodeTemplate paaSNodeTemplate = paaSTopology.getAllNodes().get(activity.getNodeId());
        String stepAfterId = null;
        if (lastStep.getFollowingSteps() != null && lastStep.getFollowingSteps().size() == 1) {
            stepAfterId = lastStep.getFollowingSteps().iterator().next();
        }
        NodeActivityStep insertedStep = addActivityStep(wf, paaSNodeTemplate, activity);
        WorkflowUtils.linkSteps(lastStep, insertedStep);
        if (stepAfterId != null) {
            AbstractStep stepAfter = wf.getSteps().get(stepAfterId);
            unlinkSteps(lastStep, stepAfter);
            WorkflowUtils.linkSteps(insertedStep, stepAfter);
        }
    }

    public void removeStep(Workflow wf, PaaSTopology paaSTopology, String stepId) {
        AbstractStep step = wf.getSteps().remove(stepId);
        if (step == null) {
            throw new InconsistentWorkflowException(String.format(
                    "Inconsistent workflow: a step nammed '%s' can not be found while it's referenced else where ...", stepId));
        }
        if (step.getPrecedingSteps() != null) {
            if (step.getFollowingSteps() != null) {
                // connect all preceding to all following
                for (String precedingId : step.getPrecedingSteps()) {
                    AbstractStep preceding = wf.getSteps().get(precedingId);
                    for (String followingId : step.getFollowingSteps()) {
                        AbstractStep following = wf.getSteps().get(followingId);
                        WorkflowUtils.linkSteps(preceding, following);
                    }
                }
            }
            for (Object precedingId : step.getPrecedingSteps().toArray()) {
                AbstractStep preceding = wf.getSteps().get(precedingId);
                unlinkSteps(preceding, step);
            }
        }
        if (step.getFollowingSteps() != null) {
            for (Object followingId : step.getFollowingSteps().toArray()) {
                AbstractStep following = wf.getSteps().get(followingId);
                unlinkSteps(step, following);
            }
        }
    }

    public void renameStep(Workflow wf, PaaSTopology paaSTopology, String stepId, String newStepName) {
        if (wf.getSteps().containsKey(newStepName)) {
            throw new AlreadyExistException(String.format("A step nammed ''{0}'' already exists", newStepName));
        }
        AbstractStep step = wf.getSteps().remove(stepId);
        step.setName(newStepName);
        wf.getSteps().put(newStepName, step);
        // now explore the links
        if (step.getPrecedingSteps() != null) {
            for (String precedingId : step.getPrecedingSteps()) {
                AbstractStep precedingStep = wf.getSteps().get(precedingId);
                precedingStep.getFollowingSteps().remove(stepId);
                precedingStep.getFollowingSteps().add(newStepName);
            }
        }
        if (step.getFollowingSteps() != null) {
            for (String followingId : step.getFollowingSteps()) {
                AbstractStep followingStep = wf.getSteps().get(followingId);
                followingStep.getPrecedingSteps().remove(stepId);
                followingStep.getPrecedingSteps().add(newStepName);
            }
        }
    }

    public void removeNode(Workflow wf, PaaSTopology paaSTopology, String nodeName) {
        AbstractStep[] steps = new AbstractStep[wf.getSteps().size()];
        steps = wf.getSteps().values().toArray(steps);
        for (AbstractStep step : steps) {
            if (step instanceof NodeActivityStep && ((NodeActivityStep) step).getNodeId().equals(nodeName)) {
                removeStep(wf, paaSTopology, step.getName());
            }
        }
    }

    /**
     * When a relationship is remove, we remove all links between src and target.
     * <p>
     * TODO : a better implem should be to just remove the links that rely to this relationship. But to do this, we have to associate the link with the
     * relationship (when the link is created consecutively to a relationship add).
     * 
     * @param wf
     * @param paaSTopology
     * @param paaSNodeTemplate
     * @param relationhipTarget
     */
    public void removeRelationship(Workflow wf, PaaSTopology paaSTopology, PaaSNodeTemplate paaSNodeTemplate, String relationhipTarget) {
        Iterator<AbstractStep> steps = wf.getSteps().values().iterator();
        while(steps.hasNext()) {
            AbstractStep step = steps.next();
            if (step instanceof NodeActivityStep && ((NodeActivityStep)step).getNodeId().equals(paaSNodeTemplate.getId())) {
                if (step.getFollowingSteps() != null) {
                    for (String followingId : step.getFollowingSteps()) {
                        AbstractStep followingStep = wf.getSteps().get(followingId);
                        if (followingStep instanceof NodeActivityStep && ((NodeActivityStep) followingStep).getNodeId().equals(relationhipTarget)) {
                            unlinkSteps(step, followingStep);
                        }
                    }
                }
                if (step.getPrecedingSteps() != null) {
                    Object precedings[] = step.getPrecedingSteps().toArray();
                    for (Object precedingId : precedings) {
                        AbstractStep precedingStep = wf.getSteps().get(precedingId);
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
    public void swapSteps(Workflow wf, PaaSTopology paaSTopology, String stepId, String targetId) {
        AbstractStep step = wf.getSteps().get(stepId);
        AbstractStep target = wf.getSteps().get(targetId);
        unlinkSteps(step, target);
        List<AbstractStep> stepPredecessors = removePredecessors(wf, step);
        List<AbstractStep> stepFollowers = removeFollowers(wf, step);
        List<AbstractStep> targetPredecessors = removePredecessors(wf, target);
        List<AbstractStep> targetFollowers = removeFollowers(wf, target);
        associateFollowers(step, targetFollowers);
        associateFollowers(target, stepFollowers);
        associatePredecessors(step, targetPredecessors);
        associatePredecessors(target, stepPredecessors);
        WorkflowUtils.linkSteps(target, step);
    }

    private void associatePredecessors(AbstractStep step, List<AbstractStep> stepPredecessors) {
        for (AbstractStep predecessor : stepPredecessors) {
            WorkflowUtils.linkSteps(predecessor, step);
        }
    }

    private void associateFollowers(AbstractStep step, List<AbstractStep> stepFollowers) {
        for (AbstractStep follower : stepFollowers) {
            WorkflowUtils.linkSteps(step, follower);
        }
    }

    private List<AbstractStep> removePredecessors(Workflow wf, AbstractStep step) {
        List<AbstractStep> result = Lists.newArrayList();
        if (step.getPrecedingSteps() == null || step.getPrecedingSteps().size() == 0) {
            return result;
        }
        Object precedings[] = step.getPrecedingSteps().toArray();
        for (Object precedingId : precedings) {
            AbstractStep precedingStep = wf.getSteps().get(precedingId);
            unlinkSteps(precedingStep, step);
            result.add(precedingStep);
        }
        return result;
    }

    private List<AbstractStep> removeFollowers(Workflow wf, AbstractStep step) {
        List<AbstractStep> result = Lists.newArrayList();
        if (step.getFollowingSteps() == null || step.getFollowingSteps().size() == 0) {
            return result;
        }
        Object followings[] = step.getFollowingSteps().toArray();
        for (Object followingId : followings) {
            AbstractStep followingStep = wf.getSteps().get(followingId);
            unlinkSteps(step, followingStep);
            result.add(followingStep);
        }
        return result;
    }

}
