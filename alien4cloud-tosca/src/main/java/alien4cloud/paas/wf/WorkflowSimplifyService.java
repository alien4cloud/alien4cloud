package alien4cloud.paas.wf;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import javax.annotation.Resource;

import org.alien4cloud.tosca.model.workflow.Workflow;
import org.alien4cloud.tosca.model.workflow.WorkflowStep;
import org.alien4cloud.tosca.model.workflow.activities.SetStateWorkflowActivity;
import org.alien4cloud.tosca.model.workflow.declarative.DefaultDeclarativeWorkflows;
import org.alien4cloud.tosca.model.workflow.declarative.NodeOperationDeclarativeWorkflow;
import org.springframework.stereotype.Component;

import alien4cloud.paas.plan.ToscaNodeLifecycleConstants;
import alien4cloud.paas.wf.util.NodeSubGraphFilter;
import alien4cloud.paas.wf.util.SimpleGraphConsumer;
import alien4cloud.paas.wf.util.SubGraph;
import alien4cloud.paas.wf.util.SubGraphFilter;
import alien4cloud.paas.wf.util.WorkflowGraphUtils;
import alien4cloud.paas.wf.util.WorkflowStepWeightComparator;
import alien4cloud.paas.wf.util.WorkflowUtils;
import alien4cloud.tosca.parser.ToscaParser;
import alien4cloud.utils.AlienUtils;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
public class WorkflowSimplifyService {

    @Resource
    private WorkflowsBuilderService workflowsBuilderService;

    private interface DoWithNodeCallBack {
        void doWithNode(SubGraph subGraph, Workflow workflow);
    }

    @Getter
    private static class ComputeNodesWeightsGraphConsumer extends SimpleGraphConsumer {

        private Map<String, Integer> allNodeWeights = new HashMap<>();

        @Override
        public boolean onNewPath(List<WorkflowStep> path) {
            WorkflowStep currentNode = path.get(path.size() - 1);
            int parentWeight;
            if (path.size() > 1) {
                parentWeight = allNodeWeights.get(path.get(path.size() - 2).getName());
            } else {
                parentWeight = 0;
            }
            Integer currentWeight = allNodeWeights.computeIfAbsent(currentNode.getName(), k -> 0);
            Integer newComputedWeight = Math.max(parentWeight + 1, currentWeight);
            allNodeWeights.put(currentNode.getName(), newComputedWeight);
            return true;
        }
    }

    @Getter
    private static class LastPathGraphConsumer extends SimpleGraphConsumer {

        private List<WorkflowStep> lastPath;

        @Override
        public boolean onNewPath(List<WorkflowStep> path) {
            lastPath = path;
            return true;
        }
    }

    /**
     * Simplify all the workflows which have no custom modifications
     * @param tc Topology Context
     */
    public void simplifyWorkflow(TopologyContext tc) {
        simplifyWorkflow(tc, tc.getTopology().getWorkflows().keySet());
    }

    /**
     * Simplify only the workflows given inside the white list
     * and which have no custom modifications
     *
     * @param tc Topology Context
     * @param whiteList list of workflow names
     */
    public void simplifyWorkflow(TopologyContext tc, Set<String> whiteList) {
        // 1. Flatten the workflow
        doWithNode(tc, (subGraph, workflow) -> flattenWorkflow(tc, subGraph), whiteList);

        // 2. Remove unnecessary steps
        doWithNode(tc, (subGraph, workflow) -> removeUnnecessarySteps(tc, workflow, subGraph), whiteList);

        if (ToscaParser.ALIEN_DSL_200.equals(tc.getDSLVersion())) {
            reentrantSimplifyWorklow(tc, whiteList);
        }
    }

    /**
     * These simplifiers can be run on any workflow, even if modified.
     *
     * @param tc
     * @param whiteList
     */
    public void reentrantSimplifyWorklow(TopologyContext tc, Set<String> whiteList) {
        // 3. Remove the orphan nodes
        doWithNode(tc, ((subGraph, workflow) -> {
            DefaultDeclarativeWorkflows dwf = workflowsBuilderService.getDeclarativeWorkflows(tc.getDSLVersion());
            removeOrphanSetStateSteps(dwf, workflow);
        }), whiteList);

        // 4. Remove useless edges
        doWithNode(tc, ((subGraph, workflow) -> removeUselessEdges(workflow)), whiteList);
    }

    protected void removeUselessEdges(Workflow wf) {
		List<WorkflowStep[]> blacklists = new ArrayList<>();
        Collection<WorkflowStep> steps = wf.getSteps().values();
        steps.forEach(step -> {
            // 1. If the current node has more than one preceding node, kick off the work
            if (step.getPrecedingSteps().size() > 1) {
                // 2. For each preceding node, get all the precedences of this node
                Map<String, Set<String>> precedencesMap = new HashMap<>();
                step.getPrecedingSteps().forEach(preName -> {
                    Set<String> precedences = WorkflowUtils.findAllPrecedences(steps, preName);
                    precedencesMap.put(preName, precedences);
                });

                // 3. For each preceding node,
                // if the precedent node is contained in any other precedences ancestor set,
                // remove the connection (between precedent and current)
                step.getPrecedingSteps().forEach(preName -> {
					Set<String> otherStepNames = new HashSet<>(step.getPrecedingSteps());
					otherStepNames.remove(preName);
					if (containedInOtherPaths(precedencesMap, preName, otherStepNames)) {
						// Add the edge between precedent and current to blacklist
						blacklists.add(new WorkflowStep[] { WorkflowUtils.findStep(steps, preName), step });
					}
                });
            }
        });
        // 4. Remove the edges in blacklist
		blacklists.forEach(pair -> WorkflowUtils.removeEdge(pair[0], pair[1]));
    }

    private boolean containedInOtherPaths(Map<String, Set<String>> precedencesMap, String step, Set<String> otherSteps) {
		for (String otherPreStep : otherSteps) {
			Set<String> otherPaths = precedencesMap.get(otherPreStep);
			if (otherPaths.contains(step)) {
				return true;
			}
		}
		return false;
	}

    protected void removeOrphanSetStateSteps(DefaultDeclarativeWorkflows dwf, Workflow workflow) {
        // 1. Find all the set state operation pairs
        Map<String, String> pairs = new HashMap<>();
        Map<String, NodeOperationDeclarativeWorkflow> standardOps = dwf.getNodeWorkflows().get(workflow.getName()).getOperations();
        standardOps.values().forEach(a -> pairs.put(a.getPrecedingState(), a.getFollowingState()));

        // 2. Iterate the current workflow to find the matched pairs
        // and then remove them
        Collection<WorkflowStep> steps = workflow.getSteps().values();
        List<String> blackListSteps = new ArrayList<>();
        steps.stream()
                .filter(step -> step.getActivity() instanceof SetStateWorkflowActivity)
                .filter(step -> {
                    String stateName = ((SetStateWorkflowActivity) step.getActivity()).getStateName();
                    // deleting & deleted should not be blacklisted
                    // (Cfy wants nodes to be in state deleted in order to be able to delete deployment without force)
                    return !(ToscaNodeLifecycleConstants.DELETING.equals(stateName) || ToscaNodeLifecycleConstants.DELETED.equals(stateName));
                })
                .forEach(step -> {
                    if (step.getPrecedingSteps().size() <= 1 && step.getOnSuccess().size() == 1) {
                        WorkflowStep nextStep = WorkflowUtils.findSteps(steps, step.getOnSuccess()).get(0);
                        WorkflowStep preStep = step.getPrecedingSteps().size() == 0 ? null : WorkflowUtils.findSteps(steps, step.getPrecedingSteps()).get(0);
                        if (isPairStep(step, nextStep, pairs)) {
                            blackListSteps.add(step.getName());
                            blackListSteps.add(nextStep.getName());
                            // reconnect the pre steps and the following steps of the second step in pairs
                            if (preStep != null) {
                                preStep.removeFollowing(step.getName());
                                step.removePreceding(preStep.getName());
                            }
                            nextStep.getOnSuccess().forEach(name -> {
                                WorkflowStep nextNextStep = WorkflowUtils.findStep(steps, name);
                                if (nextNextStep != null) {
                                    nextNextStep.removePreceding(nextStep.getName());
                                    WorkflowUtils.linkSteps(preStep, nextNextStep);
                                }
                            });
                            nextStep.getOnSuccess().clear();
                        }
                    }
                });

        // 3. Remove the black list of state operations
        blackListSteps.forEach(name -> workflow.getSteps().remove(name));
    }

    private boolean isPairStep(WorkflowStep step, WorkflowStep nextStep, Map<String, String> pairs) {
        if (nextStep == null || !(nextStep.getActivity() instanceof SetStateWorkflowActivity)) {
            return false;
        }
        String currentState = ((SetStateWorkflowActivity) step.getActivity()).getStateName();
        String nextState = ((SetStateWorkflowActivity) nextStep.getActivity()).getStateName();
        String expectedState = pairs.get(currentState);
        return nextState.equals(expectedState);
    }

    private void removeUnnecessarySteps(TopologyContext topologyContext, Workflow workflow, SubGraph subGraph) {
        LastPathGraphConsumer consumer = new LastPathGraphConsumer();
        subGraph.browse(consumer);
        if (consumer.getAllNodes().isEmpty()) {
            // This is really strange as we have a node template without any workflow step
            return;
        }
        Set<String> allStepIds = consumer.getAllNodes().keySet();
        List<WorkflowStep> sortedByWeightsSteps = consumer.getLastPath();
        List<Integer> nonEmptyIndexes = new ArrayList<>();
        LinkedHashSet<Integer> emptyIndexes = new LinkedHashSet<>();
        int lastIndexWithOutgoingLinks = -1;
        int firstIndexWithOutgoingLinks = -1;
        for (int i = 0; i < sortedByWeightsSteps.size(); i++) {
            WorkflowStep step = sortedByWeightsSteps.get(i);
            if (!allStepIds.containsAll(step.getOnSuccess())) {
                lastIndexWithOutgoingLinks = i;
                if (firstIndexWithOutgoingLinks == -1) {
                    firstIndexWithOutgoingLinks = i;
                }
            }
            if (!WorkflowGraphUtils.isStepEmpty(step, topologyContext)) {
                nonEmptyIndexes.add(i);
            } else {
                emptyIndexes.add(i);
            }
        }
        Map<Integer, Integer> emptyIndexToFollowingSubstituteMap = new HashMap<>();
        Map<Integer, Integer> emptyIndexToPrecedingSubstituteMap = new HashMap<>();
        for (Integer emptyIndex : emptyIndexes) {
            Optional<Integer> substitutePrecedingIndex = nonEmptyIndexes.stream().filter(nonEmptyIndex -> nonEmptyIndex > emptyIndex).findFirst();
            Optional<Integer> substituteFollowingIndex = nonEmptyIndexes.stream().filter(nonEmptyIndex -> nonEmptyIndex < emptyIndex)
                    .reduce((first, second) -> second);
            substitutePrecedingIndex.ifPresent(index -> emptyIndexToPrecedingSubstituteMap.put(emptyIndex, index));
            substituteFollowingIndex.ifPresent(index -> emptyIndexToFollowingSubstituteMap.put(emptyIndex, index));
        }
        for (int index = 0; index < sortedByWeightsSteps.size(); index++) {
            WorkflowStep step = sortedByWeightsSteps.get(index);
            boolean stepIsEmpty = emptyIndexes.contains(index);
            Integer followingSubstitutedIndex = emptyIndexToFollowingSubstituteMap.get(index);
            Integer precedingSubstitutedIndex = emptyIndexToPrecedingSubstituteMap.get(index);
            if (stepIsEmpty) {
                if (followingSubstitutedIndex == null && !allStepIds.containsAll(step.getOnSuccess())) {
                    // the step is empty but no substitute for following links, it means that before the step there are no non empty steps
                    if (firstIndexWithOutgoingLinks >= 0 && firstIndexWithOutgoingLinks <= index) {
                        // the step or before the step, outgoing links exist out of the sub graph so we should not remove it, because it will impact the
                        // structure of the graph
                        emptyIndexes.remove(index);
                        continue;
                    }
                }
                if (precedingSubstitutedIndex == null && !allStepIds.containsAll(step.getPrecedingSteps())) {
                    // the step is empty but no substitute for preceding links, it means that after the step there are no non empty steps
                    if (lastIndexWithOutgoingLinks >= index) {
                        // the step or after the step, outgoing links exist out of the sub graph so we should not remove it, because it will impact the
                        // structure of the graph
                        emptyIndexes.remove(index);
                        continue;
                    }
                }
                // Empty so the step will be removed, so unlink all
                for (String following : step.getOnSuccess()) {
                    workflow.getSteps().get(following).removePreceding(step.getName());
                }
                for (String preceding : step.getPrecedingSteps()) {
                    workflow.getSteps().get(preceding).removeFollowing(step.getName());
                }
                if (followingSubstitutedIndex != null) {
                    WorkflowStep substitutedFollowingStep = sortedByWeightsSteps.get(followingSubstitutedIndex);
                    for (String following : step.getOnSuccess()) {
                        workflow.getSteps().get(following).addPreceding(substitutedFollowingStep.getName());
                    }
                    // Copy all links to the substituted node
                    substitutedFollowingStep.addAllFollowings(step.getOnSuccess());
                }
                if (precedingSubstitutedIndex != null) {
                    WorkflowStep substitutedPrecedingStep = sortedByWeightsSteps.get(precedingSubstitutedIndex);
                    for (String preceding : step.getPrecedingSteps()) {
                        workflow.getSteps().get(preceding).addFollowing(substitutedPrecedingStep.getName());
                    }
                    substitutedPrecedingStep.addAllPrecedings(step.getPrecedingSteps());
                }
            }
        }
        int index = 0;
        Iterator<WorkflowStep> stepIterator = sortedByWeightsSteps.iterator();
        while (stepIterator.hasNext()) {
            WorkflowStep step = stepIterator.next();
            if (emptyIndexes.contains(index)) {
                stepIterator.remove();
                workflow.getSteps().remove(step.getName());
            }
            index++;
        }
    }

    private void doWithNode(TopologyContext tc, DoWithNodeCallBack callback, Set<String> whiteList) {
        // Attention: workflows with custom modifications are not processed
        AlienUtils.safe(tc.getTopology().getWorkflows()).values().stream()
                  .filter(wf -> !wf.isHasCustomModifications() && whiteList.contains(wf.getName()))
                  .forEach(wf -> AlienUtils.safe(tc.getTopology().getNodeTemplates()).keySet().forEach(nodeId -> {
                      SubGraphFilter stepFilter = new NodeSubGraphFilter(wf, nodeId, tc.getTopology());
                      SubGraph subGraph = new SubGraph(wf, stepFilter);
                      callback.doWithNode(subGraph, wf);
                  }));
    }

    private void flattenWorkflow(TopologyContext topologyContext, SubGraph subGraph) {
        ComputeNodesWeightsGraphConsumer consumer = new ComputeNodesWeightsGraphConsumer();
        subGraph.browse(consumer);
        if (consumer.getAllNodes().isEmpty()) {
            // This is really strange as we have a node template without any workflow step
            return;
        }
        Map<String, WorkflowStep> allNodes = consumer.getAllNodes();
        LinkedList<WorkflowStep> sortedByWeightsSteps = new LinkedList<>(allNodes.values());
        sortedByWeightsSteps.sort(new WorkflowStepWeightComparator(consumer.getAllNodeWeights(), topologyContext.getTopology()));
        Set<String> allSubGraphNodeIds = allNodes.keySet();
        sortedByWeightsSteps.forEach(workflowStep -> {
            // Remove all old links between the steps in the graph
            workflowStep.removeAllPrecedings(allSubGraphNodeIds);
            workflowStep.removeAllFollowings(allSubGraphNodeIds);
        });
        // Create a sequence with sorted sub graph steps
        for (int i = 0; i < sortedByWeightsSteps.size() - 1; i++) {
            sortedByWeightsSteps.get(i).addFollowing(sortedByWeightsSteps.get(i + 1).getName());
            sortedByWeightsSteps.get(i + 1).addPreceding(sortedByWeightsSteps.get(i).getName());
        }
    }
}
