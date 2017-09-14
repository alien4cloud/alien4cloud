package alien4cloud.paas.wf.validation;

import static org.alien4cloud.tosca.normative.constants.NormativeWorkflowNameConstants.INSTALL;
import static org.alien4cloud.tosca.normative.constants.NormativeWorkflowNameConstants.START;
import static org.alien4cloud.tosca.normative.constants.NormativeWorkflowNameConstants.STOP;
import static org.alien4cloud.tosca.normative.constants.NormativeWorkflowNameConstants.UNINSTALL;

import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.alien4cloud.tosca.model.workflow.Workflow;
import org.alien4cloud.tosca.model.workflow.WorkflowStep;
import org.alien4cloud.tosca.model.workflow.activities.SetStateWorkflowActivity;
import org.elasticsearch.common.collect.Lists;
import org.elasticsearch.common.collect.Maps;
import org.elasticsearch.common.collect.Sets;

import alien4cloud.paas.plan.ToscaNodeLifecycleConstants;
import alien4cloud.paas.wf.Path;
import alien4cloud.paas.wf.WorkflowsBuilderService.TopologyContext;
import alien4cloud.paas.wf.exception.WorkflowException;
import alien4cloud.paas.wf.util.WorkflowGraphUtils;
import lombok.extern.slf4j.Slf4j;

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
    private static final Map<String, Integer> START_STATES_SEQUENCE;
    private static final Map<String, Integer> STOP_STATES_SEQUENCE;

    static {
        INSTALL_STATES_SEQUENCE = new HashMap<>();
        INSTALL_STATES_SEQUENCE.put(ToscaNodeLifecycleConstants.INITIAL, 0);
        INSTALL_STATES_SEQUENCE.put(ToscaNodeLifecycleConstants.CREATING, 1);
        INSTALL_STATES_SEQUENCE.put(ToscaNodeLifecycleConstants.CREATED, 2);
        INSTALL_STATES_SEQUENCE.put(ToscaNodeLifecycleConstants.CONFIGURING, 3);
        INSTALL_STATES_SEQUENCE.put(ToscaNodeLifecycleConstants.CONFIGURED, 4);
        INSTALL_STATES_SEQUENCE.put(ToscaNodeLifecycleConstants.STARTING, 5);
        INSTALL_STATES_SEQUENCE.put(ToscaNodeLifecycleConstants.STARTED, 6);
        START_STATES_SEQUENCE = new HashMap<>();
        START_STATES_SEQUENCE.put(ToscaNodeLifecycleConstants.STARTING, 0);
        START_STATES_SEQUENCE.put(ToscaNodeLifecycleConstants.STARTED, 1);
        UNINSTALL_STATES_SEQUENCE = new HashMap<>();
        UNINSTALL_STATES_SEQUENCE.put(ToscaNodeLifecycleConstants.STOPPING, 0);
        UNINSTALL_STATES_SEQUENCE.put(ToscaNodeLifecycleConstants.STOPPED, 1);
        UNINSTALL_STATES_SEQUENCE.put(ToscaNodeLifecycleConstants.DELETING, 2);
        UNINSTALL_STATES_SEQUENCE.put(ToscaNodeLifecycleConstants.DELETED, 3);
        STOP_STATES_SEQUENCE = new HashMap<>();
        STOP_STATES_SEQUENCE.put(ToscaNodeLifecycleConstants.STOPPING, 0);
        STOP_STATES_SEQUENCE.put(ToscaNodeLifecycleConstants.STOPPED, 1);
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
        Map<String, Map<WorkflowStep, Set<Path>>> pathsPerNodePerStepMap = getPathsPerNodePerStepMap(paths);
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
        Iterator<WorkflowStep> steps = path.iterator();
        WorkflowStep lastDetectedStep = null;
        while (steps.hasNext()) {
            WorkflowStep step = steps.next();
            if (step.getTarget().equals(nodeId) && step.getActivity() instanceof SetStateWorkflowActivity) {
                String stateName = ((SetStateWorkflowActivity) (step).getActivity()).getStateName();
                Integer stateIdx = stateSequence.get(stateName);
                if (stateIdx == null) {
                    // if the state is null, it can be a custom state, we don't care about it
                    continue;
                }
                if (lastDetectedStep == null) {
                    lastDetectedStep = step;
                } else {
                    String lastDetectedState = ((SetStateWorkflowActivity) (lastDetectedStep).getActivity()).getStateName();
                    Integer lastDetectedStateIdx = stateSequence.get(lastDetectedState);
                    Integer currentDetectedStateIdx = stateSequence.get(stateName);
                    if (lastDetectedStateIdx.compareTo(currentDetectedStateIdx) > 0) {
                        errors.add(new BadStateSequenceError(lastDetectedStep.getName(), step.getName()));
                        // throw new BadStateOrderException(String.format("Issue in the state sequence for node '%s': '%s' can not be set after the state '%s'",
                        // nodeId, stateName,
                        // lastDetectedState));
                    } else {
                        lastDetectedStep = step;
                    }
                }
            }
        }
    }

    /**
     * Per node, just keep the intersection between all the {@link Path} sets.
     */
    private Map<String, Set<Path>> getPathsPerNodeIntersectionMap(Map<String, Map<WorkflowStep, Set<Path>>> pathsPerNodePerStepMap) {
        Map<String, Set<Path>> pathsPerNodeIntersectionMap = Maps.newHashMap();
        for (Entry<String, Map<WorkflowStep, Set<Path>>> entry : pathsPerNodePerStepMap.entrySet()) {
            String nodeId = entry.getKey();
            Map<WorkflowStep, Set<Path>> pathsPerStep = entry.getValue();
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
    private Map<String, Map<WorkflowStep, Set<Path>>> getPathsPerNodePerStepMap(List<Path> paths) {
        Map<String, Map<WorkflowStep, Set<Path>>> pathsPerNodePerStepMap = Maps.newHashMap();
        for (Path path : paths) {
            for (WorkflowStep step : path) {
                if (step.getActivity() instanceof SetStateWorkflowActivity) {
                    String node = step.getTarget();
                    Map<WorkflowStep, Set<Path>> pathsPerStepMap = pathsPerNodePerStepMap.computeIfAbsent(node, k -> Maps.newHashMap());
                    Set<Path> pathsPerStep = pathsPerStepMap.computeIfAbsent(step, k -> Sets.newHashSet());
                    pathsPerStep.add(path);
                }
            }
        }
        return pathsPerNodePerStepMap;
    }

    private Map<String, Integer> getStateSequence(Workflow workflow) {
        switch (workflow.getName()) {
        case INSTALL:
            return INSTALL_STATES_SEQUENCE;
        case UNINSTALL:
            return UNINSTALL_STATES_SEQUENCE;
        case START:
            return INSTALL_STATES_SEQUENCE;
        case STOP:
            return UNINSTALL_STATES_SEQUENCE;
        default:
            return null;
        }
    }

}
