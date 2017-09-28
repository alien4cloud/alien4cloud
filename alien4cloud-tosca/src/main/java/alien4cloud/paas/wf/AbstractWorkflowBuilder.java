package alien4cloud.paas.wf;

import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.alien4cloud.tosca.model.templates.NodeTemplate;
import org.alien4cloud.tosca.model.templates.RelationshipTemplate;
import org.alien4cloud.tosca.model.workflow.NodeWorkflowStep;
import org.alien4cloud.tosca.model.workflow.Workflow;
import org.alien4cloud.tosca.model.workflow.WorkflowStep;
import org.alien4cloud.tosca.model.workflow.activities.AbstractWorkflowActivity;
import org.alien4cloud.tosca.model.workflow.activities.DelegateWorkflowActivity;
import org.elasticsearch.common.collect.Lists;
import org.elasticsearch.common.collect.Maps;

import alien4cloud.exception.AlreadyExistException;
import alien4cloud.paas.wf.WorkflowsBuilderService.TopologyContext;
import alien4cloud.paas.wf.exception.BadWorkflowOperationException;
import alien4cloud.paas.wf.exception.InconsistentWorkflowException;
import alien4cloud.paas.wf.util.WorkflowUtils;

public abstract class AbstractWorkflowBuilder {

    public abstract void addNode(Workflow wf, String nodeId, TopologyContext toscaTypeFinder, boolean isCompute);

    public abstract void addRelationship(Workflow wf, String nodeId, NodeTemplate nodeTemplate, RelationshipTemplate relationshipTemplate,
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

    void connectStepFrom(Workflow wf, String stepId, String[] stepNames) {
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

    void connectStepTo(Workflow wf, String stepId, String[] stepNames) {
        WorkflowStep from = wf.getSteps().get(stepId);
        if (from == null) {
            throw new InconsistentWorkflowException(
                    String.format("Inconsistent workflow: a step nammed '%s' can not be found while it's referenced else where ...", stepId));
        }
        for (String following : stepNames) {
            WorkflowStep followingStep = wf.getSteps().get(following);
            WorkflowUtils.linkSteps(from, followingStep);
        }
    }

    private WorkflowStep addActivityStep(Workflow wf, String nodeId, AbstractWorkflowActivity activity) {
        WorkflowStep step = new NodeWorkflowStep();
        step.setTarget(nodeId);
        step.setActivity(activity);
        step.setName(WorkflowUtils.buildStepName(wf, step, 0));
        wf.addStep(step);
        return step;
    }

    private void unlinkSteps(WorkflowStep from, WorkflowStep to) {
        from.removeFollowing(to.getName());
        to.removePreceding(from.getName());
    }

    /**
     * @param wf the workflow to add activity
     * @param relatedStepId if specified, the step will be added near this one (maybe before)
     * @param before if true, the step will be added before the relatedStepId
     * @param activity the activity to be added
     */
    void addActivity(Workflow wf, String relatedStepId, boolean before, AbstractWorkflowActivity activity, TopologyContext topologyContext) {
        if (WorkflowUtils.isNativeOrSubstitutionNode(activity.getTarget(), topologyContext)) {
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
            addActivityStep(wf, activity.getTarget(), activity);
        }
    }

    private void insertActivityStep(Workflow wf, String stepId, AbstractWorkflowActivity activity) {
        WorkflowStep lastStep = wf.getSteps().get(stepId);
        String stepBeforeId = null;
        if (lastStep.getPrecedingSteps() != null && lastStep.getPrecedingSteps().size() == 1) {
            stepBeforeId = lastStep.getPrecedingSteps().iterator().next();
        }
        WorkflowStep insertedStep = addActivityStep(wf, activity.getTarget(), activity);
        WorkflowUtils.linkSteps(insertedStep, lastStep);
        if (stepBeforeId != null) {
            WorkflowStep stepBefore = wf.getSteps().get(stepBeforeId);
            unlinkSteps(stepBefore, lastStep);
            WorkflowUtils.linkSteps(stepBefore, insertedStep);
        }
    }

    private void appendActivityStep(Workflow wf, String stepId, AbstractWorkflowActivity activity) {
        WorkflowStep lastStep = wf.getSteps().get(stepId);
        String stepAfterId = null;
        if (lastStep.getOnSuccess() != null && lastStep.getOnSuccess().size() == 1) {
            stepAfterId = lastStep.getOnSuccess().iterator().next();
        }
        WorkflowStep insertedStep = addActivityStep(wf, activity.getTarget(), activity);
        WorkflowUtils.linkSteps(lastStep, insertedStep);
        if (stepAfterId != null) {
            WorkflowStep stepAfter = wf.getSteps().get(stepAfterId);
            unlinkSteps(lastStep, stepAfter);
            WorkflowUtils.linkSteps(insertedStep, stepAfter);
        }
    }

    void removeStep(Workflow wf, String stepId, boolean force) {
        WorkflowStep step = wf.getSteps().remove(stepId);
        if (step == null) {
            throw new InconsistentWorkflowException(
                    String.format("Inconsistent workflow: a step nammed '%s' can not be found while it's referenced else where ...", stepId));
        }
        if (!force && step.getActivity() instanceof DelegateWorkflowActivity) {
            throw new BadWorkflowOperationException("Native steps can not be removed from workflow");
        }
        if (step.getPrecedingSteps() != null) {
            if (step.getOnSuccess() != null) {
                // connect all preceding to all following
                for (String precedingId : step.getPrecedingSteps()) {
                    WorkflowStep preceding = wf.getSteps().get(precedingId);
                    for (String followingId : step.getOnSuccess()) {
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
        if (step.getOnSuccess() != null) {
            for (Object followingId : step.getOnSuccess().toArray()) {
                WorkflowStep following = wf.getSteps().get(followingId);
                unlinkSteps(step, following);
            }
        }
    }

    public void renameStep(Workflow wf, String stepId, String newStepName) {
        if (wf.getSteps().containsKey(newStepName)) {
            throw new AlreadyExistException(String.format("A step named ''%s'' already exists in workflow '%s'", newStepName, wf.getName()));
        }
        WorkflowStep step = wf.getSteps().remove(stepId);
        step.setName(newStepName);
        wf.getSteps().put(newStepName, step);
        // now explore the links
        if (step.getPrecedingSteps() != null) {
            for (String precedingId : step.getPrecedingSteps()) {
                WorkflowStep precedingStep = wf.getSteps().get(precedingId);
                precedingStep.getOnSuccess().remove(stepId);
                precedingStep.getOnSuccess().add(newStepName);
            }
        }
        if (step.getOnSuccess() != null) {
            for (String followingId : step.getOnSuccess()) {
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
            if (step.getTarget().equals(nodeName)) {
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
     * @param wf the workflow
     * @param relationshipTarget the relationship to remove
     */
    void removeRelationship(Workflow wf, String nodeId, String relationshipName, String relationshipTarget) {
        for (WorkflowStep step : wf.getSteps().values()) {
            if (step.getTarget().equals(nodeId)) {
                if (step.getOnSuccess() != null) {
                    for (String followingId : step.getOnSuccess().toArray(new String[step.getOnSuccess().size()])) {
                        WorkflowStep followingStep = wf.getSteps().get(followingId);
                        // If the following step is a step of the target of the relationship
                        // or a step of the relationship it-self then remove the link
                        if (WorkflowUtils.isNodeStep(followingStep, relationshipTarget)
                                || WorkflowUtils.isRelationshipStep(followingStep, nodeId, relationshipName)) {
                            unlinkSteps(step, followingStep);
                        }
                    }
                }
                if (step.getPrecedingSteps() != null) {
                    for (String precedingId : step.getPrecedingSteps().toArray(new String[step.getPrecedingSteps().size()])) {
                        // If the preceding step is a step of the target of the relationship
                        // or a step of the relationship it-self then remove the link
                        WorkflowStep precedingStep = wf.getSteps().get(precedingId);
                        if (WorkflowUtils.isNodeStep(precedingStep, relationshipTarget)
                                || WorkflowUtils.isRelationshipStep(precedingStep, nodeId, relationshipName)) {
                            unlinkSteps(precedingStep, step);
                        }
                    }
                }
            }
        }
        // Remove relationships steps
        Iterator<Entry<String, WorkflowStep>> stepsIterator = wf.getSteps().entrySet().iterator();
        while (stepsIterator.hasNext()) {
            WorkflowStep step = stepsIterator.next().getValue();
            if (WorkflowUtils.isRelationshipStep(step, nodeId, relationshipName)) {
                stepsIterator.remove();
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
        if (step.getOnSuccess() == null || step.getOnSuccess().size() == 0) {
            return result;
        }
        Object followings[] = step.getOnSuccess().toArray();
        for (Object followingId : followings) {
            WorkflowStep followingStep = wf.getSteps().get(followingId);
            unlinkSteps(step, followingStep);
            result.add(followingStep);
        }
        return result;
    }

    void renameNode(Workflow wf, String oldName, String newName) {
        if (wf.getSteps() != null) {
            for (WorkflowStep step : wf.getSteps().values()) {
                if (step.getTarget().equals(oldName)) {
                    step.setTarget(newName);
                }
            }
        }
    }

    Workflow reinit(Workflow wf, TopologyContext toscaTypeFinder) {
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
