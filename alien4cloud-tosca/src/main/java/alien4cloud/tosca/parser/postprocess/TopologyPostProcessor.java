package alien4cloud.tosca.parser.postprocess;

import static alien4cloud.utils.AlienUtils.safe;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import javax.annotation.Resource;

import org.alien4cloud.tosca.model.templates.NodeGroup;
import org.alien4cloud.tosca.model.templates.NodeTemplate;
import org.alien4cloud.tosca.model.templates.PolicyTemplate;
import org.alien4cloud.tosca.model.templates.Topology;
import org.alien4cloud.tosca.model.types.AbstractToscaType;
import org.alien4cloud.tosca.model.workflow.NodeWorkflowStep;
import org.alien4cloud.tosca.model.workflow.RelationshipWorkflowStep;
import org.alien4cloud.tosca.model.workflow.Workflow;
import org.alien4cloud.tosca.model.workflow.WorkflowStep;
import org.alien4cloud.tosca.model.workflow.activities.AbstractWorkflowActivity;
import org.apache.commons.collections4.MapUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;
import org.yaml.snakeyaml.nodes.Node;

import com.google.common.collect.Maps;
import com.google.common.collect.Sets;

import alien4cloud.paas.wf.WorkflowsBuilderService;
import alien4cloud.paas.wf.util.WorkflowUtils;
import alien4cloud.topology.TopologyUtils;
import alien4cloud.tosca.context.ToscaContext;
import alien4cloud.tosca.model.ArchiveRoot;
import alien4cloud.tosca.parser.ParsingContextExecution;
import alien4cloud.tosca.parser.ParsingError;
import alien4cloud.tosca.parser.ParsingErrorLevel;
import alien4cloud.tosca.parser.ToscaParser;
import alien4cloud.tosca.parser.impl.ErrorCode;
import alien4cloud.utils.NameValidationUtils;

/**
 * Post process a topology.
 */
@Component
public class TopologyPostProcessor implements IPostProcessor<Topology> {
    @Resource
    private NodeTemplatePostProcessor nodeTemplatePostProcessor;
    @Resource
    private NodeTemplateRelationshipPostProcessor nodeTemplateRelationshipPostProcessor;
    @Resource
    private SubstitutionMappingPostProcessor substitutionMappingPostProcessor;
    @Resource
    private GroupPostProcessor groupPostProcessor;
    @Resource
    private WorkflowsBuilderService workflowBuilderService;
    @Resource
    private PropertyDefinitionPostProcessor propertyDefinitionPostProcessor;
    // Inputs do not define artifact reference so we don't perform validation on them like for types.
    @Resource
    private TypeDeploymentArtifactPostProcessor typeDeploymentArtifactPostProcessor;

    @Override
    public void process(Topology instance) {
        if (instance == null) {
            return;
        }
        ArchiveRoot archiveRoot = ParsingContextExecution.getRootObj();
        Node node = ParsingContextExecution.getObjectToNodeMap().get(instance); // The yaml node for the topology

        setDependencies(instance, archiveRoot);

        if (instance.isEmpty()) {
            // if the topology doesn't contains any node template it won't be imported so add a warning.
            ParsingContextExecution.getParsingErrors()
                    .add(new ParsingError(ParsingErrorLevel.WARNING, ErrorCode.EMPTY_TOPOLOGY, null, node.getStartMark(), null, node.getEndMark(), ""));
        }

        // archive name and version
        instance.setArchiveName(archiveRoot.getArchive().getName());
        instance.setArchiveVersion(archiveRoot.getArchive().getVersion());

        // Inputs validation
        safe(instance.getInputs()).entrySet().forEach(propertyDefinitionPostProcessor);
        safe(instance.getInputArtifacts()).values().forEach(typeDeploymentArtifactPostProcessor);

        int groupIndex = 0;
        // Groups validation
        for (NodeGroup nodeGroup : safe(instance.getGroups()).values()) {
            nodeGroup.setIndex(groupIndex++);
            groupPostProcessor.process(nodeGroup);
        }

        checkPolicies(instance);

        // Node templates validation
        for (Map.Entry<String, NodeTemplate> nodeTemplateEntry : safe(instance.getNodeTemplates()).entrySet()) {
            nodeTemplateEntry.getValue().setName(nodeTemplateEntry.getKey());
            nodeTemplatePostProcessor.process(nodeTemplateEntry.getValue());
        }
        safe(instance.getNodeTemplates()).values().forEach(nodeTemplateRelationshipPostProcessor);

        substitutionMappingPostProcessor.process(instance.getSubstitutionMapping());

        // first validate names
        TopologyUtils.normalizeAllNodeTemplateName(instance, ParsingContextExecution.getParsingErrors(), ParsingContextExecution.getObjectToNodeMap());

        // Workflow validation if any are defined
        WorkflowsBuilderService.TopologyContext topologyContext = workflowBuilderService
                .buildCachedTopologyContext(new WorkflowsBuilderService.TopologyContext() {
                    @Override
                    public String getDSLVersion() {
                        return ParsingContextExecution.getDefinitionVersion();
                    }

                    @Override
                    public Topology getTopology() {
                        return instance;
                    }

                    @Override
                    public <T extends AbstractToscaType> T findElement(Class<T> clazz, String id) {
                        return ToscaContext.get(clazz, id);
                    }
                });
        ensureThatActivitiesContainsOnlyOnActivityOrSplit(topologyContext);
        finalizeParsedWorkflows(topologyContext, node);
    }

    private void setDependencies(Topology instance, ArchiveRoot archiveRoot) {
        if (archiveRoot.getArchive().getDependencies() == null) {
            return;
        }
        instance.setDependencies(Sets.newHashSet(archiveRoot.getArchive().getDependencies()));
    }

    private void checkPolicies(Topology topology) {
        // Policies post processing
        for (PolicyTemplate policyTemplate : safe(topology.getPolicies()).values()) {
            for (String target : safe(policyTemplate.getTargets())) {
                // check that the target is an exiting node template
                if (!safe((topology.getNodeTemplates())).containsKey(target)) {
                    // Dispatch an error.
                    Node node = ParsingContextExecution.getObjectToNodeMap().get(policyTemplate.getTargets());
                    ParsingContextExecution.getParsingErrors().add(new ParsingError(ParsingErrorLevel.ERROR, ErrorCode.POLICY_TARGET_NOT_FOUND, null,
                            node.getStartMark(), null, node.getEndMark(), policyTemplate.getName()));
                }
            }
        }
    }

    /**
     * Called after yaml parsing.
     *
     * Add support of activities on alien-dsl-2.0.0 and higher.
     * For activity, other than the first, we create 1 step per activity.
     */
    private void ensureThatActivitiesContainsOnlyOnActivityOrSplit(WorkflowsBuilderService.TopologyContext topologyContext) {
        if (!ToscaParser.ALIEN_DSL_200.equals(topologyContext.getDSLVersion()) || MapUtils.isEmpty(topologyContext.getTopology().getWorkflows())) {
            return;
        }

        Map<String, Map<String, WorkflowStep>> stepsToAddByWf = new HashMap<>();
        Map<String, Set<String>> newStepNames = Maps.newHashMap();
        for (Workflow wf : topologyContext.getTopology().getWorkflows().values()) {
            if (wf.getSteps() != null) {
                for (WorkflowStep step : wf.getSteps().values()) {
                    if (step.getActivities().size() < 2) {
                        continue;
                    }
                    newStepNames.computeIfAbsent(step.getName(), k -> Sets.newHashSet());
                    stepsToAddByWf.computeIfAbsent(wf.getName(), k -> Maps.newHashMap());
                    for (int i = 1; i < step.getActivities().size(); i++) {
                        // here we iterate on activities to create new step
                        WorkflowStep singleActivityStep;
                        if (step instanceof NodeWorkflowStep) {
                            singleActivityStep = new NodeWorkflowStep(step.getTarget(), ((NodeWorkflowStep) step).getHostId(), step.getActivities().get(i));
                        } else {
                            RelationshipWorkflowStep rwfs = (RelationshipWorkflowStep) step;
                            singleActivityStep = new RelationshipWorkflowStep(rwfs.getTarget(), rwfs.getTargetRelationship(), rwfs.getSourceHostId(),
                                    rwfs.getTargetHostId(), rwfs.getActivities().get(i));
                        }
                        String wfStepName = generateNewWfStepName(wf.getSteps().keySet(), stepsToAddByWf.get(wf.getName()).keySet(), step.getName());
                        singleActivityStep.setName(wfStepName);
                        singleActivityStep.setOnSuccess(step.getOnSuccess());
                        singleActivityStep.setOperationHost(step.getOperationHost());
                        stepsToAddByWf.get(wf.getName()).put(wfStepName, singleActivityStep);
                        newStepNames.get(step.getName()).add(wfStepName);
                    }
                    // new steps are created, we can clean activities
                    step.getActivities().subList(1, step.getActivities().size()).clear();
                }
            }
        }

        // add new steps to the workflow
        for (String wf : stepsToAddByWf.keySet()) {
            for (String stepName : stepsToAddByWf.get(wf).keySet()) {
                topologyContext.getTopology().getWorkflows().get(wf).getSteps().put(stepName, stepsToAddByWf.get(wf).get(stepName));
            }
        }

        // update on_success map to call the new steps when necessary
        for (Workflow wf : topologyContext.getTopology().getWorkflows().values()) {
            if (wf.getSteps() != null) {
                for (WorkflowStep step : wf.getSteps().values()) {
                    if (step.getOnSuccess() == null || step.getOnSuccess().isEmpty()) {
                        break;
                    }
                    for (String onSuccessId : step.getOnSuccess()) {
                        if (newStepNames.containsKey(onSuccessId)) {
                            step.getOnSuccess().addAll(newStepNames.get(onSuccessId));
                        }
                    }
                }
            }
        }
    }

    private String generateNewWfStepName(Set<String> existingStepNames, Set<String> newStepNames, String stepName) {
        int i = 0;
        while (existingStepNames.contains(stepName + "_" + i) || newStepNames.contains(stepName + "_" + i)) {
            i++;
        }
        return stepName + "_" + i;
    }

    /**
     * Called after yaml parsing.
     */
    private void finalizeParsedWorkflows(WorkflowsBuilderService.TopologyContext topologyContext, Node node) {
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
                            AbstractWorkflowActivity activity = step.getActivity();
                            if (activity == null) {
                                // add an error ?
                            } else {
                                activity.setTarget(step.getTarget());
                            }
                        }
                    }
                }
            }
            WorkflowUtils.fillHostId(wf, topologyContext);
            int errorCount = workflowBuilderService.validateWorkflow(topologyContext, wf);
            if (errorCount > 0) {
                ParsingContextExecution.getParsingErrors().add(new ParsingError(ParsingErrorLevel.WARNING, ErrorCode.WORKFLOW_HAS_ERRORS, null,
                        node.getStartMark(), null, node.getEndMark(), wf.getName()));
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