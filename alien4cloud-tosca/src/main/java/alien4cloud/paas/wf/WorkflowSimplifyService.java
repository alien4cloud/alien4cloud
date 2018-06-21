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
import java.util.stream.Collectors;

import javax.annotation.Resource;

import org.alien4cloud.tosca.model.workflow.Workflow;
import org.alien4cloud.tosca.model.workflow.WorkflowStep;
import org.alien4cloud.tosca.model.workflow.activities.CallOperationWorkflowActivity;
import org.alien4cloud.tosca.model.workflow.activities.SetStateWorkflowActivity;
import org.alien4cloud.tosca.model.workflow.declarative.DefaultDeclarativeWorkflows;
import org.alien4cloud.tosca.model.workflow.declarative.NodeOperationDeclarativeWorkflow;
import org.springframework.stereotype.Component;

import alien4cloud.paas.wf.util.NodeSubGraphFilter;
import alien4cloud.paas.wf.util.SimpleGraphConsumer;
import alien4cloud.paas.wf.util.SubGraph;
import alien4cloud.paas.wf.util.SubGraphFilter;
import alien4cloud.paas.wf.util.WorkflowGraphUtils;
import alien4cloud.paas.wf.util.WorkflowStepWeightComparator;
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

    public void simplifyWorkflow(TopologyContext topologyContext) {
        doWithNode(topologyContext, (subGraph, workflow) -> flattenWorkflow(topologyContext, subGraph));
        doWithNode(topologyContext, (subGraph, workflow) -> removeUnnecessarySteps(topologyContext, workflow, subGraph));
        if (topologyContext.getDSLVersion().equals(ToscaParser.ALIEN_DSL_200)) {
            // only this DSL is compatible with this feature
            doWithNode(topologyContext, (subGraph, workflow) -> removeOrphanSetStateSteps(topologyContext, workflow));
        }
    }

    private void removeOrphanSetStateSteps(TopologyContext topologyContext, Workflow workflow) {
        DefaultDeclarativeWorkflows dwf = workflowsBuilderService.getDeclarativeWorkflows(topologyContext.getDSLVersion());
        // 1. Find the black list of operations
        Map<String, NodeOperationDeclarativeWorkflow> standardOps = dwf.getNodeWorkflows().get(workflow.getName()).getOperations();
        Set<String> allOps = standardOps.keySet();
		Collection<WorkflowStep> steps = workflow.getSteps().values();
		Set<String> actualOps = steps.stream()
										.filter(step -> step.getActivity() instanceof CallOperationWorkflowActivity)
										.map(step -> ((CallOperationWorkflowActivity) step.getActivity()).getOperationName())
										.collect(Collectors.toSet());
        allOps.removeAll(actualOps);
        Set<String> blackListOps = allOps;

        // 2. Find the black list of states
        Set<String> blackListStates = new HashSet<>();
        blackListOps.forEach(ops -> {
            blackListStates.add(standardOps.get(ops).getPrecedingState());
            blackListStates.add(standardOps.get(ops).getFollowingState());
        });

        // 3. Remove the black list of state operations
        List<String> blackListSteps = new ArrayList<>();
		steps.stream().filter(step -> step.getActivity() instanceof SetStateWorkflowActivity).forEach(step -> {

            String stateName = ((SetStateWorkflowActivity) step.getActivity()).getStateName();
            if (blackListStates.contains(stateName)) {
            	// Reconnect the pointer
				String currentStepName = step.getName();
				WorkflowStep nextStep = findNextStep(steps, currentStepName);
				WorkflowStep preStep = findPreStep(steps, step);
				if (nextStep == null && preStep == null) {
                    step.getOnSuccess().clear();
                    step.getPrecedingSteps().clear();
				} else if (preStep == null) {
					// Delete the head and reset the next step as the new head
                    step.getOnSuccess().clear();
					nextStep.getPrecedingSteps().clear();
				} else if (nextStep == null) {
					// Delete the tail and reset the precedent step as the new tail
                    step.getOnSuccess().clear();
                    preStep.getOnSuccess().clear();
				} else {
                    step.getOnSuccess().clear();
                    preStep.getOnSuccess().clear();
                    preStep.getOnSuccess().add(nextStep.getName());
					nextStep.getPrecedingSteps().clear();
					nextStep.getPrecedingSteps().add(preStep.getName());
				}
                blackListSteps.add(currentStepName);
            }
        });
        blackListSteps.forEach(name -> workflow.getSteps().remove(name));

    }

	private WorkflowStep findPreStep(Collection<WorkflowStep> steps, WorkflowStep currentStep) {
		if (currentStep.getPrecedingSteps().isEmpty()) {
			return null;
		}
		String predStepName = new ArrayList<>(currentStep.getPrecedingSteps()).get(0);
		for (WorkflowStep step : steps) {
			if (step.getName().equals(predStepName)) {
				return step;
			}
		}
		return null;
	}

	private WorkflowStep findNextStep(Collection<WorkflowStep> steps, String currentStep) {
        for (WorkflowStep step : steps) {
            if (step.getPrecedingSteps().contains(currentStep)) {
                return step;
            }
        }
        return null;
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

    private void doWithNode(TopologyContext topologyContext, DoWithNodeCallBack callBack) {
        // workflows with custom modifications are not processed
        AlienUtils.safe(topologyContext.getTopology().getWorkflows()).values().stream().filter(workflow -> !workflow.isHasCustomModifications())
                .forEach(workflow -> AlienUtils.safe(topologyContext.getTopology().getNodeTemplates()).keySet().forEach(nodeId -> {
                    SubGraphFilter stepFilter = new NodeSubGraphFilter(workflow, nodeId, topologyContext.getTopology());
                    SubGraph subGraph = new SubGraph(workflow, stepFilter);
                    callBack.doWithNode(subGraph, workflow);
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
