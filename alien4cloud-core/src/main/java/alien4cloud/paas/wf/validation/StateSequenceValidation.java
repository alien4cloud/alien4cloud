package alien4cloud.paas.wf.validation;

import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import lombok.extern.slf4j.Slf4j;

import org.elasticsearch.common.collect.Lists;
import org.elasticsearch.common.collect.Maps;
import org.elasticsearch.common.collect.Sets;

import alien4cloud.paas.plan.ToscaNodeLifecycleConstants;
import alien4cloud.paas.wf.AbstractStep;
import alien4cloud.paas.wf.NodeActivityStep;
import alien4cloud.paas.wf.Path;
import alien4cloud.paas.wf.SetStateActivity;
import alien4cloud.paas.wf.Workflow;
import alien4cloud.paas.wf.WorkflowsBuilderService.TopologyContext;
import alien4cloud.paas.wf.exception.WorkflowException;
import alien4cloud.paas.wf.util.WorkflowGraphUtils;

/**
 * This rule will check that for a given node, the 'set state' operations are done in the
 * right order regarding the type of workflow.
 * <p>
 * For example, if the right sequence is known as 'create' -> 'configure' -> 'start', this means that for a given node, these 3 operations can not appear in
 * another order :
 * <ul>
 * <li>'create' -> 'configure' -> 'start' : valid
 * <li>'create' -> 'start' : valid
 * <li>'configure' : valid
 * <li>'create' -> 'start' -> 'configure' : invalid
 * </ul>
 * They must also be on the same branch (they can not be parallelized).
 * <p>
 * Actually the rule is: for each node, all set state steps must be <b>at least on a same path</b> and they should be in the <b>correct order</b> on this path.
 * <p>
 * To achieve such check, for each node we:
 * <ul>
 * <li>list the state step states and the paths they are found on (a step can be on several paths).
 * <li>get the intersection of all these path : this way we should have at least 1 path containing all steps.
 * <li>check the order on these paths.
 * </ul>
 * <p>
 * Maybe a more efficient algo can be found (1 pass ?) ... Feel free to have fun !
 */
@Slf4j
public class StateSequenceValidation implements Rule {

    private static final Map<String, Integer> INSTALL_STATES_SEQUENCE;
    private static final Map<String, Integer> UNINSTALL_STATES_SEQUENCE;

    static {
        INSTALL_STATES_SEQUENCE = new HashMap<String, Integer>();
        INSTALL_STATES_SEQUENCE.put(ToscaNodeLifecycleConstants.INITIAL, 0);
        INSTALL_STATES_SEQUENCE.put(ToscaNodeLifecycleConstants.CREATING, 1);
        INSTALL_STATES_SEQUENCE.put(ToscaNodeLifecycleConstants.CREATED, 2);
        INSTALL_STATES_SEQUENCE.put(ToscaNodeLifecycleConstants.CONFIGURING, 3);
        INSTALL_STATES_SEQUENCE.put(ToscaNodeLifecycleConstants.CONFIGURED, 4);
        INSTALL_STATES_SEQUENCE.put(ToscaNodeLifecycleConstants.STARTING, 5);
        INSTALL_STATES_SEQUENCE.put(ToscaNodeLifecycleConstants.STARTED, 6);
        UNINSTALL_STATES_SEQUENCE = new HashMap<String, Integer>();
        UNINSTALL_STATES_SEQUENCE.put(ToscaNodeLifecycleConstants.STOPPING, 0);
        UNINSTALL_STATES_SEQUENCE.put(ToscaNodeLifecycleConstants.STOPPED, 1);
        UNINSTALL_STATES_SEQUENCE.put(ToscaNodeLifecycleConstants.DELETING, 2);
        UNINSTALL_STATES_SEQUENCE.put(ToscaNodeLifecycleConstants.DELETED, 3);
    }

    @Override
    public List<AbstractWorkflowError> validate(TopologyContext topologyContext, Workflow workflow) throws WorkflowException {
        // the state sequence to use
        Map<String, Integer> stateSequence = getStateSequence(workflow);
        if (stateSequence == null) {
            // this rule only apply on std wfs and
            // needs a state sequence to run
            return null;
        }
        if (workflow.getSteps() == null || workflow.getSteps().isEmpty()) {
            return null;
        }
        List<AbstractWorkflowError> errors = Lists.newArrayList();
        List<Path> paths = WorkflowGraphUtils.getWorkflowGraphPaths(workflow);
        Map<String, Map<NodeActivityStep, Set<Path>>> pathsPerNodePerStepMap = getPathsPerNodePerStepMap(paths);
        Map<String, Set<Path>> pathsPerNodeMap = getPathsPerNodeIntersectionMap(pathsPerNodePerStepMap);
        // now we have to ensure that for the remaining paths, the order is correct between steps
        for (Entry<String, Set<Path>> pathSetEntry : pathsPerNodeMap.entrySet()) {
            String nodeId = pathSetEntry.getKey();
            if (pathSetEntry.getValue().isEmpty()) {
                // the intersection is empty : this means that step are in parallel
                // TODO: which one ?
                errors.add(new ParallelSetStatesError(nodeId));
            }
            for (Path path : pathSetEntry.getValue()) {
                ensureOrderIsCorrect(nodeId, path, stateSequence, errors);
            }
        }
        return errors;
    }

    private void ensureOrderIsCorrect(String nodeId, Path path, Map<String, Integer> stateSequence, List<AbstractWorkflowError> errors) {
        Iterator<AbstractStep> steps = path.iterator();
        NodeActivityStep lastDetectedStep = null;
        while (steps.hasNext()) {
            AbstractStep step = steps.next();
            if (step instanceof NodeActivityStep && ((NodeActivityStep) step).getNodeId().equals(nodeId)
                    && ((NodeActivityStep) step).getActivity() instanceof SetStateActivity) {
                String stateName = ((SetStateActivity)((NodeActivityStep) step).getActivity()).getStateName();
                Integer stateIdx = stateSequence.get(stateName);
                if (stateIdx == null) {
                 // if the state is null, it can be a custom state, we don't care about it
                    continue;
                }
                if (lastDetectedStep == null) {
                    lastDetectedStep = (NodeActivityStep) step;
                } else {
                    String lastDetectedState = ((SetStateActivity) ((NodeActivityStep) lastDetectedStep).getActivity()).getStateName();
                    Integer lastDetectedStateIdx = stateSequence.get(lastDetectedState);
                    Integer currentDetectedStateIdx = stateSequence.get(stateName);
                    if (lastDetectedStateIdx.compareTo(currentDetectedStateIdx) > 0) {
                        errors.add(new BadStateSequenceError(lastDetectedStep.getName(), step.getName()));
                        // throw new BadStateOrderException(String.format("Issue in the state sequence for node '%s': '%s' can not be set after the state '%s'",
                        // nodeId, stateName,
                        // lastDetectedState));
                    } else {
                        lastDetectedStep = (NodeActivityStep) step;
                    }
                }
            }
        }
    }

    /**
     * Per node, just keep the intersection between all the {@link Path} sets.
     */
    private Map<String, Set<Path>> getPathsPerNodeIntersectionMap(Map<String, Map<NodeActivityStep, Set<Path>>> pathsPerNodePerStepMap) {
        Map<String, Set<Path>> pathsPerNodeIntersectionMap = Maps.newHashMap();
        for (Entry<String, Map<NodeActivityStep, Set<Path>>> entry : pathsPerNodePerStepMap.entrySet()) {
            String nodeId = entry.getKey();
            Map<NodeActivityStep, Set<Path>> pathsPerStep = entry.getValue();
            Iterator<Set<Path>> paths = pathsPerStep.values().iterator();
            if (pathsPerStep.size() == 1) {
                pathsPerNodeIntersectionMap.put(nodeId, paths.next());
                paths.remove();
            } else {
                Set<Path> lastPaths = paths.next();
                paths.remove();
                while (paths.hasNext()) {
                    Set<Path> intersection = Sets.intersection(lastPaths, paths.next());
                    paths.remove();
                    lastPaths = intersection;
                }
                pathsPerNodeIntersectionMap.put(nodeId, lastPaths);
            }
        }
        return pathsPerNodeIntersectionMap;
    }

    /**
     * For each node / step, constitute a set containing all the paths in which this node has steps of type 'set state'.
     * 
     * @return a map using nodeId as key and the list of concerned {@link Path}s as value.
     */
    private Map<String, Map<NodeActivityStep, Set<Path>>> getPathsPerNodePerStepMap(List<Path> paths) {
        Map<String, Map<NodeActivityStep, Set<Path>>> pathsPerNodePerStepMap = Maps.newHashMap();
        for (Path path : paths) {
            Iterator<AbstractStep> steps = path.iterator();
            while (steps.hasNext()) {
                AbstractStep step = steps.next();
                if (step instanceof NodeActivityStep && ((NodeActivityStep) step).getActivity() instanceof SetStateActivity) {
                    NodeActivityStep nodeActivityStep = (NodeActivityStep) step;
                    String node = nodeActivityStep.getNodeId();
                    Map<NodeActivityStep, Set<Path>> pathsPerStepMap = pathsPerNodePerStepMap.get(node);
                    if (pathsPerStepMap == null) {
                        pathsPerStepMap = Maps.newHashMap();
                        pathsPerNodePerStepMap.put(node, pathsPerStepMap);
                    }
                    Set<Path> pathsPerStep = pathsPerStepMap.get(nodeActivityStep);
                    if (pathsPerStep == null) {
                        pathsPerStep = Sets.newHashSet();
                        pathsPerStepMap.put(nodeActivityStep, pathsPerStep);
                    }
                    pathsPerStep.add(path);
                }
            }
        }
        return pathsPerNodePerStepMap;
    }

    private Map<String, Integer> getStateSequence(Workflow workflow) {
        if (!workflow.isStandard()) {
            return null;
        } else if (workflow.getName().equals(Workflow.INSTALL_WF)) {
            return INSTALL_STATES_SEQUENCE;
        } else if (workflow.getName().equals(Workflow.UNINSTALL_WF)) {
            return UNINSTALL_STATES_SEQUENCE;
        } else {
            return null;
        }
    }
    
}
