package alien4cloud.paas.wf.util;

import static alien4cloud.utils.AlienUtils.safe;
import static org.alien4cloud.tosca.normative.constants.NormativeWorkflowNameConstants.*;
import static org.alien4cloud.tosca.utils.ToscaTypeUtils.isOfType;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.alien4cloud.tosca.model.definitions.Interface;
import org.alien4cloud.tosca.model.definitions.Operation;
import org.alien4cloud.tosca.model.templates.NodeTemplate;
import org.alien4cloud.tosca.model.templates.RelationshipTemplate;
import org.alien4cloud.tosca.model.types.NodeType;
import org.alien4cloud.tosca.model.workflow.NodeWorkflowStep;
import org.alien4cloud.tosca.model.workflow.RelationshipWorkflowStep;
import org.alien4cloud.tosca.model.workflow.Workflow;
import org.alien4cloud.tosca.model.workflow.WorkflowStep;
import org.alien4cloud.tosca.model.workflow.activities.CallOperationWorkflowActivity;
import org.alien4cloud.tosca.model.workflow.activities.DelegateWorkflowActivity;
import org.alien4cloud.tosca.model.workflow.activities.InlineWorkflowActivity;
import org.alien4cloud.tosca.model.workflow.activities.SetStateWorkflowActivity;
import org.alien4cloud.tosca.normative.constants.NormativeComputeConstants;
import org.alien4cloud.tosca.normative.constants.NormativeWorkflowNameConstants;
import org.alien4cloud.tosca.utils.TopologyNavigationUtil;
import org.apache.commons.lang3.StringUtils;

import com.google.common.collect.Maps;

import alien4cloud.exception.InvalidNameException;
import alien4cloud.exception.NotFoundException;
import alien4cloud.paas.wf.TopologyContext;

public class WorkflowUtils {

    public static final Pattern WORKFLOW_NAME_PATTERN = Pattern.compile("^\\w+$");

    private static final String NETWORK_TYPE = "tosca.nodes.Network";

    private static String getRootHostNode(String nodeId, TopologyContext topologyContext) {
        NodeTemplate nodeTemplate = topologyContext.getTopology().getNodeTemplates().get(nodeId);
        if (nodeTemplate == null) {
            return null;
        }
        NodeTemplate hostTemplate = TopologyNavigationUtil.getImmediateHostTemplate(topologyContext.getTopology(), nodeTemplate, topologyContext);
        if (hostTemplate == null) {
            return nodeId;
        }
        return getRootHostNode(hostTemplate.getName(), topologyContext);
    }

    private static String getRelationshipTarget(String source, String relationshipId, TopologyContext topologyContext) {
        NodeTemplate sourceNode = safe(topologyContext.getTopology().getNodeTemplates()).get(source);
        if (sourceNode == null) {
            throw new NotFoundException("Source " + source + " cannot be found in the topology " + topologyContext.getTopology().getId());
        }
        RelationshipTemplate relationshipTemplate = safe(sourceNode.getRelationships()).get(relationshipId);
        if (relationshipTemplate == null) {
            throw new NotFoundException(
                    "Source " + source + " does not have the relationship " + relationshipId + " in the topology " + topologyContext.getTopology().getId());
        }
        return relationshipTemplate.getTarget();
    }

    public static boolean isStandardWorkflow(Workflow workflow) {
        return NormativeWorkflowNameConstants.STANDARD_WORKFLOWS.contains(workflow.getName());
    }

    /**
     * Compute the wf in order to ensure that all step are tagged with the hostId property.
     * <p/>
     * The hostId is the first (and normally unique) compute found in the ascendency.
     */
    public static void fillHostId(Workflow wf, TopologyContext topologyContext) {
        wf.getHosts().clear();
        for (WorkflowStep step : wf.getSteps().values()) {
            if (step instanceof NodeWorkflowStep) {
                if (StringUtils.isEmpty(step.getTarget())) {
                    // Inline steps might not have target
                    // FIXME when tosca is clear on this point then we might change the model because it's not beautiful
                    return;
                }
                String hostId = WorkflowUtils.getRootHostNode(step.getTarget(), topologyContext);
                ((NodeWorkflowStep) step).setHostId(hostId);
                if (hostId != null) {
                    wf.getHosts().add(hostId);
                }
            } else if (step instanceof RelationshipWorkflowStep) {
                RelationshipWorkflowStep relationshipWorkflowStep = (RelationshipWorkflowStep) step;
                String sourceHostId = WorkflowUtils.getRootHostNode(relationshipWorkflowStep.getTarget(), topologyContext);
                String targetHostId = WorkflowUtils.getRootHostNode(
                        getRelationshipTarget(relationshipWorkflowStep.getTarget(), relationshipWorkflowStep.getTargetRelationship(), topologyContext),
                        topologyContext);
                relationshipWorkflowStep.setSourceHostId(sourceHostId);
                relationshipWorkflowStep.setTargetHostId(targetHostId);
            }
        }
    }

    /**
     * @return the operation browsing the type hierarchy
     *         FIXME: should we browse hierarchy ? what about order ?
     */
    public static Operation getOperation(String nodeTypeName, String interfaceName, String operationName, TopologyContext topologyContext) {
        NodeType nodeType = topologyContext.findElement(NodeType.class, nodeTypeName);
        if (nodeType == null) {
            return null;
        }
        if (nodeType.getInterfaces() == null) {
            return getOperationInSuperTypes(nodeType, interfaceName, operationName, topologyContext);
        }
        Interface interfaceType = nodeType.getInterfaces().get(interfaceName);
        if (interfaceType == null) {
            return getOperationInSuperTypes(nodeType, interfaceName, operationName, topologyContext);
        }
        if (interfaceType.getOperations() == null) {
            return getOperationInSuperTypes(nodeType, interfaceName, operationName, topologyContext);
        }
        Operation operation = interfaceType.getOperations().get(operationName);
        if (interfaceType.getOperations() == null) {
            return getOperationInSuperTypes(nodeType, interfaceName, operationName, topologyContext);
        }
        return operation;
    }

    private static Operation getOperationInSuperTypes(NodeType nodeType, String interfaceName, String operationName, TopologyContext topologyContext) {
        if (nodeType.getDerivedFrom() == null) {
            return null;
        }
        for (String superType : nodeType.getDerivedFrom()) {
            Operation operation = getOperation(superType, interfaceName, operationName, topologyContext);
            if (operation != null) {
                return operation;
            }
        }
        return null;
    }

    public static boolean isRelationshipStep(WorkflowStep step, String nodeId, String relationshipName) {
        return step instanceof RelationshipWorkflowStep && nodeId.equals(step.getTarget())
                && relationshipName.equals(((RelationshipWorkflowStep) step).getTargetRelationship());
    }

    public static boolean isNodeStep(WorkflowStep step, String nodeId) {
        return step instanceof NodeWorkflowStep && nodeId.equals(step.getTarget());
    }

    public static boolean isComputeOrNetwork(String nodeId, TopologyContext topologyContext) {
        NodeTemplate nodeTemplate = topologyContext.getTopology().getNodeTemplates().get(nodeId);
        if (nodeTemplate == null) {
            return false;
        }
        NodeType nodeType = topologyContext.findElement(NodeType.class, nodeTemplate.getType());
        return isOfType(nodeType, NormativeComputeConstants.COMPUTE_TYPE) || isOfType(nodeType, NETWORK_TYPE);
    }

    public static boolean isComputeOrVolume(String nodeId, TopologyContext topologyContext) {
        NodeTemplate nodeTemplate = topologyContext.getTopology().getNodeTemplates().get(nodeId);
        if (nodeTemplate == null) {
            return false;
        }
        NodeType nodeType = topologyContext.findElement(NodeType.class, nodeTemplate.getType());
        return isOfType(nodeType, NormativeComputeConstants.COMPUTE_TYPE) || isOfType(nodeType, "tosca.nodes.BlockStorage");
    }

    public static boolean isNativeOrSubstitutionNode(String nodeId, TopologyContext topologyContext) {
        NodeTemplate nodeTemplate = topologyContext.getTopology().getNodeTemplates().get(nodeId);
        if (nodeTemplate == null) {
            return false;
        }
        NodeType nodeType = topologyContext.findElement(NodeType.class, nodeTemplate.getType());
        if (nodeType.isAbstract() || nodeType.getSubstitutionTopologyId() != null) {
            return true;
        }
        // TODO: the following should be removed after merge with orchestrator refactoring branch
        // (since these types will be abstract)
        return isOfType(nodeType, NormativeComputeConstants.COMPUTE_TYPE) || isOfType(nodeType, NETWORK_TYPE) || isOfType(nodeType, "tosca.nodes.BlockStorage");
    }

    public static void linkSteps(WorkflowStep from, WorkflowStep to) {
        if (from != null && to != null) {
            from.addFollowing(to.getName());
            to.addPreceding(from.getName());
        }
    }

    public static String buildStepName(Workflow wf, WorkflowStep step, int increment) {
        StringBuilder nameBuilder = new StringBuilder(step.getStepAsString());
        if (increment > 0) {
            nameBuilder.append("_").append(increment);
        }
        String name = nameBuilder.toString();
        if (wf.getSteps().containsKey(name)) {
            return buildStepName(wf, step, ++increment);
        } else {
            return name;
        }
    }

    public static String debugWorkflow(Workflow wf) {
        StringBuilder stringBuilder = new StringBuilder("\n ======> Paste the folowing graph in http://www.webgraphviz.com/  !!\n");
        int subgraphCount = 0;
        stringBuilder.append("\ndigraph ").append(wf.getName()).append(" {");
        stringBuilder.append("\n  node [shape=box];");
        for (String host : wf.getHosts()) {
            stringBuilder.append("\n  subgraph cluster_").append(++subgraphCount).append(" {");
            stringBuilder.append("\n    label = \"").append(host).append("\";\n    color=blue;");
            for (WorkflowStep step : wf.getSteps().values()) {
                if (step instanceof NodeWorkflowStep && host.equals(((NodeWorkflowStep) step).getHostId())) {
                    stringBuilder.append("\n    \"").append(step.getName()).append("\";");
                }
            }
            stringBuilder.append("\n  }\n");
        }
        for (WorkflowStep step : wf.getSteps().values()) {
            if (step.getOnSuccess() != null) {
                for (String following : step.getOnSuccess()) {
                    stringBuilder.append("\n  \"").append(step.getName()).append("\" -> \"").append(following).append("\";");
                }
            }
            if (step.getOnSuccess() == null || step.getOnSuccess().isEmpty()) {
                stringBuilder.append("\n  \"").append(step.getName()).append("\" -> end;");
            }
            if (step.getPrecedingSteps() == null || step.getPrecedingSteps().isEmpty()) {
                stringBuilder.append("\n  start -> \"").append(step.getName()).append("\";");
            }
        }
        stringBuilder.append("\n  start [shape=doublecircle];\n");
        stringBuilder.append("  end [shape=circle];\n");
        stringBuilder.append("}\n");
        stringBuilder.append("======================\n");
        return stringBuilder.toString();
    }

    public static WorkflowStep addOperationStep(Workflow wf, String nodeId, String interfaceName, String operationName) {
        WorkflowStep step = createOperationStep(wf, nodeId, interfaceName, operationName);
        wf.addStep(step);
        return step;
    }

    public static WorkflowStep createOperationStep(Workflow wf, String nodeId, String interfaceName, String operationName) {
        CallOperationWorkflowActivity task = new CallOperationWorkflowActivity();
        task.setInterfaceName(interfaceName);
        task.setOperationName(operationName);
        NodeWorkflowStep step = new NodeWorkflowStep();
        step.setTarget(nodeId);
        step.setActivity(task);
        step.setName(buildStepName(wf, step, 0));
        return step;
    }

    public static WorkflowStep addRelationshipOperationStep(Workflow wf, String nodeId, String relationshipId, String interfaceName, String operationName,
            String operationHost) {
        WorkflowStep step = createRelationshipOperationStep(wf, nodeId, relationshipId, interfaceName, operationName, operationHost);
        wf.addStep(step);
        return step;
    }

    public static WorkflowStep createRelationshipOperationStep(Workflow wf, String nodeId, String relationshipId, String interfaceName, String operationName,
            String operationHost) {
        CallOperationWorkflowActivity task = new CallOperationWorkflowActivity();
        task.setInterfaceName(interfaceName);
        task.setOperationName(operationName);
        RelationshipWorkflowStep step = new RelationshipWorkflowStep();
        step.setTarget(nodeId);
        step.setTargetRelationship(relationshipId);
        step.setActivity(task);
        step.setName(buildStepName(wf, step, 0));
        step.setOperationHost(operationHost);
        return step;
    }

    public static WorkflowStep addStateStep(Workflow wf, String nodeId, String stateName) {
        WorkflowStep step = createStateStep(wf, nodeId, stateName);
        wf.addStep(step);
        return step;
    }

    public static WorkflowStep createStateStep(Workflow wf, String nodeId, String stateName) {
        SetStateWorkflowActivity task = new SetStateWorkflowActivity();
        task.setStateName(stateName);
        NodeWorkflowStep step = new NodeWorkflowStep();
        step.setTarget(nodeId);
        step.setActivity(task);
        step.setName(buildStepName(wf, step, 0));
        return step;
    }

    public static WorkflowStep addDelegateWorkflowStep(Workflow wf, String nodeId) {
        WorkflowStep step = createDelegateWorkflowStep(wf, nodeId);
        wf.addStep(step);
        return step;
    }

    public static WorkflowStep createDelegateWorkflowStep(Workflow wf, String nodeId) {
        DelegateWorkflowActivity activity = new DelegateWorkflowActivity();
        activity.setDelegate(wf.getName());
        NodeWorkflowStep step = new NodeWorkflowStep();
        step.setTarget(nodeId);
        step.setActivity(activity);
        step.setName(buildStepName(wf, step, 0));
        return step;
    }

    public static void validateName(String name) {
        if (!WORKFLOW_NAME_PATTERN.matcher(name).matches()) {
            throw new InvalidNameException("workflowName", name, "name <" + name
                    + "> is not valid. It should only contains alphanumeric character from the basic Latin alphabet, underscores(_) and dash(-).");
        }
    }

    private static void processInlineWorkflow(Map<String, Workflow> workflowMap, Workflow workflow) {
        final Set<String> newInlinedStepNames = new HashSet<>();
        // Clone the map as we iterate and in the same time modify it
        Maps.newHashMap(workflow.getSteps()).entrySet().stream().filter(entry -> entry.getValue().getActivity() instanceof InlineWorkflowActivity)
                .forEach(inlinedStepEntry -> {
                    String inlinedStepName = inlinedStepEntry.getKey();
                    WorkflowStep inlinedStep = inlinedStepEntry.getValue();
                    InlineWorkflowActivity inlineWorkflowActivity = (InlineWorkflowActivity) inlinedStep.getActivity();
                    String inlinedName = inlineWorkflowActivity.getInline();
                    Workflow inlined = workflowMap.get(inlinedName);
                    if (inlined == null) {
                        throw new NotFoundException("Inlined workflow " + inlinedName);
                    }
                    Map<String, WorkflowStep> generatedSteps = cloneSteps(inlined.getSteps());

                    Map<String, WorkflowStep> generatedStepsWithNewNames = generatedSteps.entrySet().stream().collect(Collectors.toMap(entry -> {
                        String newName = generateNewWfStepNameWithPrefix(inlinedStepEntry.getKey() + "_", workflow.getSteps().keySet(), newInlinedStepNames,
                                entry.getKey());
                        newInlinedStepNames.add(newName);
                        if (!newName.equals(entry.getKey())) {
                            entry.getValue().setName(newName);
                            generatedSteps.forEach((generatedStepId, generatedStep) -> {
                                if (generatedStep.removeFollowing(entry.getKey())) {
                                    generatedStep.addFollowing(newName);
                                }
                                if (generatedStep.removePreceding(entry.getKey())) {
                                    generatedStep.addPreceding(newName);

                                }
                            });
                        }
                        return newName;
                    }, Map.Entry::getValue));
                    // Find all root steps of the workflow and link them to the parent workflows
                    final Map<String, WorkflowStep> rootInlinedSteps = generatedStepsWithNewNames.values().stream()
                            .filter(generatedStepWithNewName -> generatedStepWithNewName.getPrecedingSteps().isEmpty())
                            .peek(rootInlinedStep -> rootInlinedStep.addAllPrecedings(inlinedStep.getPrecedingSteps()))
                            .collect(Collectors.toMap(WorkflowStep::getName, rootInlinedStep -> rootInlinedStep));

                    inlinedStep.getPrecedingSteps().forEach(precedingStepName -> {
                        WorkflowStep precedingStep = workflow.getSteps().get(precedingStepName);
                        precedingStep.removeFollowing(inlinedStepName);
                        precedingStep.addAllFollowings(rootInlinedSteps.keySet());
                    });
                    // Find all leaf steps of the workflow and link them to the parent workflows
                    final Map<String, WorkflowStep> leafInlinedSteps = generatedStepsWithNewNames.values().stream()
                            .filter(generatedStepWithNewName -> generatedStepWithNewName.getOnSuccess().isEmpty())
                            .peek(leafInlinedStep -> leafInlinedStep.addAllFollowings(inlinedStep.getOnSuccess()))
                            .collect(Collectors.toMap(WorkflowStep::getName, leafInlinedStep -> leafInlinedStep));

                    inlinedStep.getOnSuccess().forEach(onSuccessStepName -> {
                        WorkflowStep onSuccessStep = workflow.getSteps().get(onSuccessStepName);
                        onSuccessStep.removePreceding(inlinedStepName);
                        onSuccessStep.addAllPrecedings(leafInlinedSteps.keySet());
                    });
                    // Remove the inlined step and replace by other workflow's steps
                    workflow.getSteps().remove(inlinedStepName);
                    workflow.addAllSteps(generatedStepsWithNewNames);
                });
        // Check if the workflow contains inline activity event after processing
        boolean processedWorkflowContainsInline = workflow.getSteps().values().stream().anyMatch(step -> step.getActivity() instanceof InlineWorkflowActivity);
        if (processedWorkflowContainsInline) {
            // Recursively process inline workflow until no step is inline workflow
            processInlineWorkflow(workflowMap, workflow);
        }
    }

    public static void processInlineWorkflows(Map<String, Workflow> workflowMap) {
        workflowMap.forEach((workflowId, workflow) -> processInlineWorkflow(workflowMap, workflow));
    }

    private static Map<String, WorkflowStep> cloneSteps(Map<String, WorkflowStep> steps) {
        return steps.entrySet().stream().collect(Collectors.toMap(Map.Entry::getKey, entry -> cloneStep(entry.getValue())));
    }

    public static Workflow cloneWorkflow(Workflow workflow) {
        Workflow cloned = new Workflow();
        cloned.setName(workflow.getName());
        cloned.setDescription(workflow.getDescription());
        cloned.setMetadata(workflow.getMetadata());
        cloned.setInputs(workflow.getInputs());
        cloned.setPreconditions(workflow.getPreconditions());
        cloned.setSteps(safe(workflow.getSteps()).entrySet().stream().collect(Collectors.toMap(Map.Entry::getKey, entry -> cloneStep(entry.getValue()))));
        cloned.setHasCustomModifications(workflow.isHasCustomModifications());
        cloned.setStandard(workflow.isStandard());
        cloned.setHosts(workflow.getHosts());
        cloned.setErrors(workflow.getErrors());
        return cloned;
    }

    public static Map<String, Workflow> cloneWorkflowMap(Map<String, Workflow> that) {
        return that.entrySet().stream()
            .collect(Collectors.toMap(Map.Entry::getKey, entry -> WorkflowUtils.cloneWorkflow(entry.getValue())));
    }

    public static WorkflowStep cloneStep(WorkflowStep step) {
        WorkflowStep cloned;
        if (step instanceof NodeWorkflowStep) {
            NodeWorkflowStep nodeWorkflowStep = (NodeWorkflowStep) step;
            cloned = new NodeWorkflowStep();
            ((NodeWorkflowStep) cloned).setHostId(nodeWorkflowStep.getHostId());
        } else {
            RelationshipWorkflowStep relationshipWorkflowStep = (RelationshipWorkflowStep) step;
            cloned = new RelationshipWorkflowStep();
            ((RelationshipWorkflowStep) cloned).setTargetRelationship(relationshipWorkflowStep.getTargetRelationship());
            ((RelationshipWorkflowStep) cloned).setSourceHostId(relationshipWorkflowStep.getSourceHostId());
            ((RelationshipWorkflowStep) cloned).setTargetHostId(relationshipWorkflowStep.getTargetHostId());
        }
        cloned.setActivities(step.getActivities());
        cloned.setFilter(step.getFilter());
        cloned.setName(step.getName());
        cloned.setOnFailure(step.getOnFailure());
        cloned.setOnSuccess(new HashSet<>(step.getOnSuccess()));
        cloned.setOperationHost(step.getOperationHost());
        cloned.setPrecedingSteps(new HashSet<>(step.getPrecedingSteps()));
        cloned.setTarget(step.getTarget());
        return cloned;
    }

    private static String generateNewWfStepNameWithPrefix(String prefix, Set<String> existingStepNames, Set<String> newStepNames, String stepName) {
        String newStepName = prefix + stepName;
        int i = 0;
        while (existingStepNames.contains(newStepName) || newStepNames.contains(newStepName)) {
            newStepName = prefix + stepName + "_" + (i++);
        }
        return newStepName;
    }

    public static String generateNewWfStepName(Set<String> existingStepNames, Set<String> newStepNames, String stepName) {
        return generateNewWfStepNameWithPrefix("", existingStepNames, newStepNames, stepName);
    }

    public static WorkflowStep findStep(Collection<WorkflowStep> steps, String name) {
        List<WorkflowStep> result = findSteps(steps, new HashSet<>(Arrays.asList(name)));
        return result.size() == 0 ? null : result.get(0);
    }

    public static List<WorkflowStep> findSteps(Collection<WorkflowStep> steps, Set<String> names) {
        return steps.stream().filter(step -> names.contains(step.getName())).collect(Collectors.toList());
    }

    /**
     * Find all the name of preceding nodes of the given step
     * @param steps All the steps
     * @param stepName Given step name
     * @return A set of preceding node names
     */
    public static Set<String> findAllPrecedences(Collection<WorkflowStep> steps, String stepName) {
        Set<String> result = new HashSet<>();
        rFindAllPrecedences(result, steps, findStep(steps, stepName));
        return result;
    }

    private static void rFindAllPrecedences(Set<String> result, Collection<WorkflowStep> steps, WorkflowStep step) {
        if (step == null) {
            return;
        }
        result.add(step.getName());
        List<WorkflowStep> preSteps = findSteps(steps, step.getPrecedingSteps());
        preSteps.forEach(pre -> rFindAllPrecedences(result, steps, pre));
    }

    /**
     * Remove the edge between preStep and step
     */
    public static void removeEdge(WorkflowStep preStep, WorkflowStep step) {
        preStep.removeFollowing(step.getName());
        step.removePreceding(preStep.getName());
    }
}
