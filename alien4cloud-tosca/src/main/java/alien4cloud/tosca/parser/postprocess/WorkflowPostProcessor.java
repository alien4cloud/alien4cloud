package alien4cloud.tosca.parser.postprocess;

import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.annotation.Resource;

import org.alien4cloud.tosca.model.templates.Topology;
import org.alien4cloud.tosca.model.types.AbstractToscaType;
import org.alien4cloud.tosca.model.workflow.Workflow;
import org.alien4cloud.tosca.model.workflow.WorkflowStep;
import org.apache.commons.collections4.MapUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;
import org.yaml.snakeyaml.nodes.Node;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;

import alien4cloud.paas.wf.TopologyContext;
import alien4cloud.paas.wf.WorkflowsBuilderService;
import alien4cloud.paas.wf.util.WorkflowUtils;
import alien4cloud.paas.wf.validation.AbstractWorkflowError;
import alien4cloud.paas.wf.validation.InlinedWorkflowNotFoundError;
import alien4cloud.paas.wf.validation.UnknownNodeError;
import alien4cloud.paas.wf.validation.UnknownRelationshipError;
import alien4cloud.paas.wf.validation.WorkflowHasCycleError;
import alien4cloud.tosca.context.ToscaContext;
import alien4cloud.tosca.parser.ParsingContextExecution;
import alien4cloud.tosca.parser.ParsingError;
import alien4cloud.tosca.parser.ParsingErrorLevel;
import alien4cloud.tosca.parser.ToscaParser;
import alien4cloud.tosca.parser.impl.ErrorCode;
import alien4cloud.utils.NameValidationUtils;

@Component
public class WorkflowPostProcessor {

    @Resource
    private WorkflowsBuilderService workflowBuilderService;

    /**
     * Process workflows of a topology
     * 
     * @param topology the topology to process workflow
     * @param topologyNode the yaml node of the topology
     */
    public void processWorkflows(Topology topology, Node topologyNode) {
        // Workflow validation if any are defined
        TopologyContext topologyContext = workflowBuilderService.buildCachedTopologyContext(new TopologyContext() {
            @Override
            public String getDSLVersion() {
                return ParsingContextExecution.getDefinitionVersion();
            }

            @Override
            public Topology getTopology() {
                return topology;
            }

            @Override
            public <T extends AbstractToscaType> T findElement(Class<T> clazz, String id) {
                return ToscaContext.get(clazz, id);
            }
        });
        // If the workflow contains steps with multiple activities then split them into single activity steps
        splitMultipleActivitiesSteps(topologyContext);
        finalizeParsedWorkflows(topologyContext, topologyNode);
    }

    /**
     * Called after yaml parsing.
     *
     * Add support of activities on alien-dsl-2.0.0 and higher.
     * For activity, other than the first, we create 1 step per activity.
     */
    private void splitMultipleActivitiesSteps(TopologyContext topologyContext) {
        if (!ToscaParser.ALIEN_DSL_200.equals(topologyContext.getDSLVersion()) || MapUtils.isEmpty(topologyContext.getTopology().getWorkflows())) {
            return;
        }

        for (Workflow wf : topologyContext.getTopology().getWorkflows().values()) {
            if (wf.getSteps() != null) {
                Map<String, WorkflowStep> stepsToAdd = new HashMap<>();
                Map<String, LinkedList<String>> newStepsNames = new HashMap<>();
                for (WorkflowStep step : wf.getSteps().values()) {
                    if (step.getActivities() == null) {
                        Node node = ParsingContextExecution.getObjectToNodeMap().get(step);
                        ParsingContextExecution.getParsingErrors().add(new ParsingError(ParsingErrorLevel.ERROR, ErrorCode.WORKFLOW_HAS_ERRORS, null,
                                node.getStartMark(), "Step should have at least one activity", node.getEndMark(), step.getName()));
                        continue;
                    } else if (step.getActivities().size() < 2) {
                        continue;
                    }
                    // We have a step with multiple activities we'll call it old step
                    // We will split this step into multiple steps, the first activity will be contained in the step with the same name
                    LinkedList<String> newStepsNamesForCurrentStep = newStepsNames.computeIfAbsent(step.getName(), k -> new LinkedList<>());
                    for (int i = 1; i < step.getActivities().size(); i++) {
                        // here we iterate on activities to create new step
                        WorkflowStep singleActivityStep = WorkflowUtils.cloneStep(step);
                        singleActivityStep.setActivities(Lists.newArrayList(step.getActivities().get(i)));
                        String wfStepName = WorkflowUtils.generateNewWfStepName(wf.getSteps().keySet(), stepsToAdd.keySet(), step.getName());
                        singleActivityStep.setName(wfStepName);
                        singleActivityStep.getOnSuccess().clear();
                        stepsToAdd.put(wfStepName, singleActivityStep);
                        newStepsNamesForCurrentStep.add(wfStepName);
                    }
                    // new steps are created, we can clean activities
                    step.getActivities().subList(1, step.getActivities().size()).clear();
                }

                // Generated steps must be executed in a sequential manner
                newStepsNames.forEach((stepName, generatedStepsNames) -> {
                    // first old step is chained to the first generated step
                    WorkflowStep firstStep = wf.getSteps().get(stepName);
                    Set<String> currentFirstStepOnSuccess = firstStep.getOnSuccess();
                    firstStep.setOnSuccess(Sets.newHashSet(generatedStepsNames.getFirst()));

                    WorkflowStep lastGeneratedStep = stepsToAdd.get(generatedStepsNames.getLast());
                    lastGeneratedStep.setOnSuccess(currentFirstStepOnSuccess);

                    for (int i = 0; i < generatedStepsNames.size() - 1; i++) {
                        // Each generated step is chained with the preceding to create a sequence
                        stepsToAdd.get(generatedStepsNames.get(i)).addFollowing(generatedStepsNames.get(i + 1));
                    }
                });

                // add new steps to the workflow
                wf.addAllSteps(stepsToAdd);
            }
        }
    }

    /**
     * Called after yaml parsing.
     */
    private void finalizeParsedWorkflows(TopologyContext topologyContext, Node node) {
        if (MapUtils.isEmpty(topologyContext.getTopology().getWorkflows())) {
            return;
        }
        normalizeWorkflowNames(topologyContext.getTopology().getWorkflows());
        for (Workflow wf : topologyContext.getTopology().getWorkflows().values()) {
            wf.setStandard(WorkflowUtils.isStandardWorkflow(wf));
            if (wf.getSteps() != null) {
                for (WorkflowStep step : wf.getSteps().values()) {
                    if (step.getOnSuccess() != null) {
                        Iterator<String> followingIds = step.getOnSuccess().iterator();
                        while (followingIds.hasNext()) {
                            String followingId = followingIds.next();
                            WorkflowStep followingStep = wf.getSteps().get(followingId);
                            if (followingStep == null) {
                                followingIds.remove();
                                ParsingContextExecution.getParsingErrors().add(new ParsingError(ParsingErrorLevel.WARNING, ErrorCode.UNKNWON_WORKFLOW_STEP,
                                        null, node.getStartMark(), null, node.getEndMark(), followingId));
                            } else {
                                followingStep.addPreceding(step.getName());
                            }
                        }
                    }
                }
            }
            WorkflowUtils.fillHostId(wf, topologyContext);
            int errorCount = workflowBuilderService.validateWorkflow(topologyContext, wf);
            if (errorCount > 0) {
                processWorkflowErrors(wf, wf.getErrors(), node);
            }
        }
    }

    private void processWorkflowErrors(Workflow wf, List<AbstractWorkflowError> errors, Node workflowNode) {
        for (AbstractWorkflowError error : errors) {
            if (error instanceof InlinedWorkflowNotFoundError) {
                Node node = ParsingContextExecution.getObjectToNodeMap().get(((InlinedWorkflowNotFoundError) error).getInlinedWorkflow());
                ParsingContextExecution.getParsingErrors().add(new ParsingError(ParsingErrorLevel.ERROR, ErrorCode.WORKFLOW_INLINED_WORKFLOW_NOT_FOUND, null,
                        node.getStartMark(), null, node.getEndMark(), ((InlinedWorkflowNotFoundError) error).getInlinedWorkflow()));
            } else if (error instanceof UnknownNodeError) {
                Node node = ParsingContextExecution.getObjectToNodeMap().get(((UnknownNodeError) error).getNodeId());
                ParsingContextExecution.getParsingErrors()
                        .add(new ParsingError(ParsingErrorLevel.ERROR, ErrorCode.UNKNWON_WORKFLOW_STEP_TARGET, null, node.getStartMark(),
                                "The target node referenced by the workflow step do not exist.", node.getEndMark(), ((UnknownNodeError) error).getNodeId()));
            } else if (error instanceof UnknownRelationshipError) {
                Node node = ParsingContextExecution.getObjectToNodeMap().get(((UnknownRelationshipError) error).getRelationshipId());
                ParsingContextExecution.getParsingErrors()
                        .add(new ParsingError(ParsingErrorLevel.ERROR, ErrorCode.UNKNWON_WORKFLOW_STEP_RELATIONSHIP_TARGET, null, node.getStartMark(),
                                "The target relationship referenced by the workflow step do not exist.", node.getEndMark(),
                                ((UnknownRelationshipError) error).getRelationshipId()));
            } else if (error instanceof WorkflowHasCycleError) {
                ParsingContextExecution.getParsingErrors().add(new ParsingError(ParsingErrorLevel.ERROR, ErrorCode.WORKFLOW_HAS_CYCLE, null,
                        workflowNode.getStartMark(), null, workflowNode.getEndMark(), wf.getName()));
            } else {
                ParsingContextExecution.getParsingErrors().add(new ParsingError(ParsingErrorLevel.WARNING, ErrorCode.WORKFLOW_HAS_ERRORS, null,
                        workflowNode.getStartMark(), null, workflowNode.getEndMark(), wf.getName()));
            }
        }
    }

    private void normalizeWorkflowNames(Map<String, Workflow> workflows) {
        for (String oldName : Sets.newHashSet(workflows.keySet())) {
            if (!NameValidationUtils.isValid(oldName)) {
                String newName = StringUtils.stripAccents(oldName);
                newName = NameValidationUtils.DEFAULT_NAME_REPLACE_PATTERN.matcher(newName).replaceAll("_");
                String toAppend = "";
                int i = 1;
                while (workflows.containsKey(newName + toAppend)) {
                    toAppend = "_" + i++;
                }
                newName = newName.concat(toAppend);
                Workflow wf = workflows.remove(oldName);
                wf.setName(newName);
                workflows.put(newName, wf);
                Node node = ParsingContextExecution.getObjectToNodeMap().get(oldName);
                ParsingContextExecution.getParsingErrors().add(new ParsingError(ParsingErrorLevel.WARNING, ErrorCode.INVALID_NAME, "Workflow",
                        node.getStartMark(), oldName, node.getEndMark(), newName));
            }
        }
    }
}
