package alien4cloud.paas.wf.util;

import static alien4cloud.utils.AlienUtils.safe;
import static org.alien4cloud.tosca.normative.constants.NormativeWorkflowNameConstants.INSTALL;
import static org.alien4cloud.tosca.normative.constants.NormativeWorkflowNameConstants.START;
import static org.alien4cloud.tosca.normative.constants.NormativeWorkflowNameConstants.STOP;
import static org.alien4cloud.tosca.normative.constants.NormativeWorkflowNameConstants.UNINSTALL;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.alien4cloud.tosca.model.definitions.Interface;
import org.alien4cloud.tosca.model.definitions.Operation;
import org.alien4cloud.tosca.model.templates.NodeTemplate;
import org.alien4cloud.tosca.model.templates.RelationshipTemplate;
import org.alien4cloud.tosca.model.types.AbstractInheritableToscaType;
import org.alien4cloud.tosca.model.types.NodeType;
import org.alien4cloud.tosca.model.types.RelationshipType;
import org.alien4cloud.tosca.model.workflow.NodeWorkflowStep;
import org.alien4cloud.tosca.model.workflow.RelationshipWorkflowStep;
import org.alien4cloud.tosca.model.workflow.Workflow;
import org.alien4cloud.tosca.model.workflow.WorkflowStep;
import org.alien4cloud.tosca.model.workflow.activities.CallOperationWorkflowActivity;
import org.alien4cloud.tosca.model.workflow.activities.DelegateWorkflowActivity;
import org.alien4cloud.tosca.model.workflow.activities.InlineWorkflowActivity;
import org.alien4cloud.tosca.model.workflow.activities.SetStateWorkflowActivity;
import org.alien4cloud.tosca.normative.constants.NormativeComputeConstants;
import org.alien4cloud.tosca.normative.constants.NormativeRelationshipConstants;

import alien4cloud.exception.InvalidNameException;
import alien4cloud.exception.NotFoundException;
import alien4cloud.paas.wf.WorkflowsBuilderService.TopologyContext;

public class WorkflowUtils {

    public static final Pattern WORKFLOW_NAME_PATTERN = Pattern.compile("^\\w+$");

    private static final String NETWORK_TYPE = "tosca.nodes.Network";
    private static final String DOCKER_TYPE = "tosca.nodes.Container.Application.DockerContainer";

    private static String getRootHostNode(String nodeId, TopologyContext topologyContext) {
        NodeTemplate nodeTemplate = topologyContext.getTopology().getNodeTemplates().get(nodeId);
        if (nodeTemplate == null) {
            return null;
        }
        NodeType nodeType = topologyContext.findElement(NodeType.class, nodeTemplate.getType());
        if (isOfType(nodeType, NormativeComputeConstants.COMPUTE_TYPE) || isOfType(nodeType, DOCKER_TYPE)) {
            return nodeId;
        } else {
            if (nodeTemplate.getRelationships() != null) {
                for (RelationshipTemplate relationshipTemplate : nodeTemplate.getRelationships().values()) {
                    RelationshipType relationshipType = topologyContext.findElement(RelationshipType.class, relationshipTemplate.getType());
                    if (isOfType(relationshipType, NormativeRelationshipConstants.HOSTED_ON)) {
                        return getRootHostNode(relationshipTemplate.getTarget(), topologyContext);
                    }
                }
            }
            return null;
        }
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
        return INSTALL.equals(workflow.getName()) || UNINSTALL.equals(workflow.getName()) || START.equals(workflow.getName())
                || STOP.equals(workflow.getName());
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
                String hostId = WorkflowUtils.getRootHostNode(step.getTarget(), topologyContext);
                ((NodeWorkflowStep) step).setHostId(hostId);
                if (hostId != null) {
                    wf.getHosts().add(hostId);
                }
            } else {
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
        return step instanceof RelationshipWorkflowStep && step.getTarget().equals(nodeId)
                && relationshipName.equals(((RelationshipWorkflowStep) step).getTargetRelationship());
    }

    public static boolean isNodeStep(WorkflowStep step, String nodeId) {
        return step instanceof NodeWorkflowStep && step.getTarget().equals(nodeId);
    }

    /**
     * Check whether the node type is equals or derived from the given type name
     * 
     * @param indexedNodeType the node type
     * @param type the type name
     * @return true if the node type is equals or derived from the given type name
     */
    public static boolean isOfType(AbstractInheritableToscaType indexedNodeType, String type) {
        return indexedNodeType != null
                && (indexedNodeType.getElementId().equals(type) || indexedNodeType.getDerivedFrom() != null && indexedNodeType.getDerivedFrom().contains(type));
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
        CallOperationWorkflowActivity task = new CallOperationWorkflowActivity();
        task.setInterfaceName(interfaceName);
        task.setOperationName(operationName);
        NodeWorkflowStep step = new NodeWorkflowStep();
        step.setTarget(nodeId);
        step.setActivity(task);
        step.setName(buildStepName(wf, step, 0));
        wf.addStep(step);
        return step;
    }

    public static WorkflowStep addRelationshipOperationStep(Workflow wf, String nodeId, String relationshipId, String interfaceName, String operationName,
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
        wf.addStep(step);
        return step;
    }

    public static WorkflowStep addStateStep(Workflow wf, String nodeId, String stateName) {
        SetStateWorkflowActivity task = new SetStateWorkflowActivity();
        task.setStateName(stateName);
        NodeWorkflowStep step = new NodeWorkflowStep();
        step.setTarget(nodeId);
        step.setActivity(task);
        step.setName(buildStepName(wf, step, 0));
        wf.addStep(step);
        return step;
    }

    public static WorkflowStep addDelegateWorkflowStep(Workflow wf, String nodeId) {
        DelegateWorkflowActivity activity = new DelegateWorkflowActivity();
        activity.setDelegate(wf.getName());
        NodeWorkflowStep step = new NodeWorkflowStep();
        step.setTarget(nodeId);
        step.setActivity(activity);
        step.setName(buildStepName(wf, step, 0));
        wf.addStep(step);
        return step;
    }

    public static void validateName(String name) {
        if (!WORKFLOW_NAME_PATTERN.matcher(name).matches()) {
            throw new InvalidNameException("workflowName", name, "name <" + name
                    + "> is not valid. It should only contains alphanumeric character from the basic Latin alphabet, underscores(_) and dash(-).");
        }
    }

    public static void processInlineWorkflows(Map<String, Workflow> workflowMap) {
        workflowMap.forEach((workflowId, workflow) -> {
            Map<String, Map<String, WorkflowStep>> inlinedWorkflowSteps = workflow.getSteps().entrySet().stream()
                    .filter(entry -> entry.getValue().getActivity() instanceof InlineWorkflowActivity)
                    .collect(Collectors.toMap(Map.Entry::getKey, inlinedStepEntry -> {
                        WorkflowStep step = inlinedStepEntry.getValue();
                        InlineWorkflowActivity inlineWorkflowActivity = (InlineWorkflowActivity) inlinedStepEntry.getValue().getActivity();
                        String inlinedName = inlineWorkflowActivity.getInline();
                        Workflow inlined = workflowMap.get(inlinedName);
                        if (inlined == null) {
                            throw new NotFoundException("Inlined workflow " + inlinedName);
                        }
                        Map<String, WorkflowStep> inlinedSteps = cloneSteps(inlined.getSteps());
                        // Find all root steps of the workflow and link them to the parent workflows
                        final Map<String, WorkflowStep> rootInlinedSteps = inlinedSteps.values().stream()
                                .filter(inlinedStep -> inlinedStep.getPrecedingSteps().isEmpty())
                                .peek(rootInlinedStep -> rootInlinedStep.getPrecedingSteps().addAll(step.getPrecedingSteps()))
                                .collect(Collectors.toMap(WorkflowStep::getName, rootInlinedStep -> rootInlinedStep));

                        step.getPrecedingSteps().forEach(precedingStepName -> {
                            WorkflowStep precedingStep = workflow.getSteps().get(precedingStepName);
                            precedingStep.getOnSuccess().remove(inlinedStepEntry.getKey());
                            precedingStep.getOnSuccess().addAll(rootInlinedSteps.keySet());
                        });
                        // Find all leaf steps of the workflow and link them to the parent workflows
                        final Map<String, WorkflowStep> leafInlinedSteps = inlinedSteps.values().stream()
                                .filter(inlinedStep -> inlinedStep.getOnSuccess().isEmpty())
                                .peek(leafInlinedStep -> leafInlinedStep.getOnSuccess().addAll(step.getOnSuccess()))
                                .collect(Collectors.toMap(WorkflowStep::getName, leafInlinedStep -> leafInlinedStep));

                        step.getOnSuccess().forEach(onSuccessStepName -> {
                            WorkflowStep onSuccessStep = workflow.getSteps().get(onSuccessStepName);
                            onSuccessStep.getPrecedingSteps().remove(inlinedStepEntry.getKey());
                            onSuccessStep.getPrecedingSteps().addAll(leafInlinedSteps.keySet());
                        });

                        // Remove the inlined step and replace by other workflow's steps
                        final Set<String> newInlinedStepNames = new HashSet<>();
                        return inlinedSteps.entrySet().stream().collect(Collectors.toMap(entry -> {
                            if (workflow.getSteps().containsKey(entry.getKey())) {
                                // If the workflow contains step with the same name then generate a new one
                                String newName = generateNewWfStepName(workflow.getSteps().keySet(), newInlinedStepNames, entry.getKey());
                                newInlinedStepNames.add(newName);
                                return newName;
                            } else {
                                return entry.getKey();
                            }
                        }, Map.Entry::getValue));
                    }));
            inlinedWorkflowSteps.forEach((inlinedStepName, generatedInlinedWorkflowSteps) -> {
                workflow.getSteps().remove(inlinedStepName);
                workflow.getSteps().putAll(generatedInlinedWorkflowSteps);
            });
        });
    }

    private static Map<String, WorkflowStep> cloneSteps(Map<String, WorkflowStep> steps) {
        return steps.entrySet().stream().collect(Collectors.toMap(Map.Entry::getKey, entry -> cloneStep(entry.getValue())));
    }

    private static WorkflowStep cloneStep(WorkflowStep step) {
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
        cloned.setActivity(step.getActivity());
        cloned.setFilter(step.getFilter());
        cloned.setName(step.getName());
        cloned.setOnFailure(step.getOnFailure());
        cloned.setOnSuccess(new HashSet<>(step.getOnSuccess()));
        cloned.setOperationHost(step.getOperationHost());
        cloned.setPrecedingSteps(new HashSet<>(step.getPrecedingSteps()));
        cloned.setTarget(step.getTarget());
        return cloned;
    }

    public static String generateNewWfStepName(Set<String> existingStepNames, Set<String> newStepNames, String stepName) {
        int i = 0;
        while (existingStepNames.contains(stepName + "_" + i) || newStepNames.contains(stepName + "_" + i)) {
            i++;
        }
        return stepName + "_" + i;
    }

}
