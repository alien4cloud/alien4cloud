package alien4cloud.paas.wf.util;

import static org.alien4cloud.tosca.normative.constants.NormativeWorkflowNameConstants.INSTALL;
import static org.alien4cloud.tosca.normative.constants.NormativeWorkflowNameConstants.START;
import static org.alien4cloud.tosca.normative.constants.NormativeWorkflowNameConstants.STOP;
import static org.alien4cloud.tosca.normative.constants.NormativeWorkflowNameConstants.UNINSTALL;

import java.util.Map;
import java.util.regex.Pattern;

import org.alien4cloud.tosca.model.definitions.Interface;
import org.alien4cloud.tosca.model.definitions.Operation;
import org.alien4cloud.tosca.model.templates.NodeTemplate;
import org.alien4cloud.tosca.model.templates.RelationshipTemplate;
import org.alien4cloud.tosca.model.types.AbstractInheritableToscaType;
import org.alien4cloud.tosca.model.types.NodeType;
import org.alien4cloud.tosca.model.types.RelationshipType;
import org.alien4cloud.tosca.model.workflow.Workflow;
import org.alien4cloud.tosca.model.workflow.WorkflowStep;
import org.alien4cloud.tosca.model.workflow.activities.CallOperationWorkflowActivity;
import org.alien4cloud.tosca.model.workflow.activities.DelegateWorkflowActivity;
import org.alien4cloud.tosca.model.workflow.activities.SetStateWorkflowActivity;
import org.alien4cloud.tosca.normative.constants.NormativeComputeConstants;
import org.alien4cloud.tosca.normative.constants.NormativeRelationshipConstants;
import org.apache.commons.lang3.StringUtils;

import alien4cloud.exception.InvalidNameException;
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
        NodeType nodeType = (NodeType) topologyContext.findElement(NodeType.class, nodeTemplate.getType());
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
            if (StringUtils.isEmpty(step.getTargetRelationship())) {
                String hostId = WorkflowUtils.getRootHostNode(step.getTarget(), topologyContext);
                step.setHostId(hostId);
                if (hostId != null) {
                    wf.getHosts().add(hostId);
                }
            }
        }
    }

    /**
     * @return the parentId of the node : the id of the node it's hostedOn (if exists).
     */
    public static String getParentId(Workflow wf, String nodeId, TopologyContext topologyContext) {
        NodeTemplate nodeTemplate = topologyContext.getTopology().getNodeTemplates().get(nodeId);
        if (nodeTemplate != null && nodeTemplate.getRelationships() != null) {
            for (RelationshipTemplate relationshipTemplate : nodeTemplate.getRelationships().values()) {
                RelationshipType relationshipType = topologyContext.findElement(RelationshipType.class, relationshipTemplate.getType());
                if (isOfType(relationshipType, NormativeRelationshipConstants.HOSTED_ON)) {
                    return relationshipTemplate.getTarget();
                }
            }
        }
        return null;
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

    public static boolean isOfType(AbstractInheritableToscaType indexedNodeType, String type) {
        if (indexedNodeType == null) {
            return false;
        }
        return indexedNodeType.getElementId().equals(type) || indexedNodeType.getDerivedFrom() != null && indexedNodeType.getDerivedFrom().contains(type);
    }

    public static Interface getInterface(String interfaceName, Map<String, Interface> interfaces) {
        return interfaces == null ? null : interfaces.get(interfaceName);
    }

    public static boolean isCompute(String nodeId, TopologyContext topologyContext) {
        NodeTemplate nodeTemplate = topologyContext.getTopology().getNodeTemplates().get(nodeId);
        if (nodeTemplate == null) {
            return false;
        }
        NodeType nodeType = (NodeType) topologyContext.findElement(NodeType.class, nodeTemplate.getType());
        return isOfType(nodeType, NormativeComputeConstants.COMPUTE_TYPE);
    }

    public static boolean isComputeOrNetwork(String nodeId, TopologyContext topologyContext) {
        NodeTemplate nodeTemplate = topologyContext.getTopology().getNodeTemplates().get(nodeId);
        if (nodeTemplate == null) {
            return false;
        }
        NodeType nodeType = (NodeType) topologyContext.findElement(NodeType.class, nodeTemplate.getType());
        if (isOfType(nodeType, NormativeComputeConstants.COMPUTE_TYPE)) {
            return true;
        } else {
            return isOfType(nodeType, NETWORK_TYPE);
        }
    }

    public static boolean isComputeOrVolume(String nodeId, TopologyContext topologyContext) {
        NodeTemplate nodeTemplate = topologyContext.getTopology().getNodeTemplates().get(nodeId);
        if (nodeTemplate == null) {
            return false;
        }
        NodeType nodeType = (NodeType) topologyContext.findElement(NodeType.class, nodeTemplate.getType());
        if (isOfType(nodeType, NormativeComputeConstants.COMPUTE_TYPE)) {
            return true;
        } else {
            return isOfType(nodeType, "tosca.nodes.BlockStorage");
        }
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
        if (isOfType(nodeType, NormativeComputeConstants.COMPUTE_TYPE) || isOfType(nodeType, NETWORK_TYPE)) {
            return true;
        } else {
            return isOfType(nodeType, "tosca.nodes.BlockStorage");
        }
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

    public static boolean isStateStep(WorkflowStep step) {
        return step.getActivity() instanceof SetStateWorkflowActivity;
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
                if (StringUtils.isEmpty(step.getTargetRelationship()) && host.equals(step.getHostId())) {
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
        task.setTarget(nodeId);
        WorkflowStep step = new WorkflowStep();
        step.setTarget(nodeId);
        step.setActivity(task);
        step.setName(buildStepName(wf, step, 0));
        wf.addStep(step);
        return step;
    }

    public static WorkflowStep addStateStep(Workflow wf, String nodeId, String stateName) {
        SetStateWorkflowActivity task = new SetStateWorkflowActivity();
        task.setStateName(stateName);
        task.setTarget(nodeId);
        WorkflowStep step = new WorkflowStep();
        step.setTarget(nodeId);
        step.setActivity(task);
        step.setName(buildStepName(wf, step, 0));
        wf.addStep(step);
        return step;
    }

    public static WorkflowStep addDelegateWorkflowStep(Workflow wf, String nodeId) {
        DelegateWorkflowActivity activity = new DelegateWorkflowActivity();
        activity.setDelegate(wf.getName());
        activity.setTarget(nodeId);
        WorkflowStep step = new WorkflowStep();
        step.setTarget(nodeId);
        step.setActivity(activity);
        step.setName(buildStepName(wf, step, 0));
        wf.addStep(step);
        return step;
    }

    public static WorkflowStep getDelegateWorkflowStepByNode(Workflow wf, String nodeName) {
        for (WorkflowStep step : wf.getSteps().values()) {
            if (StringUtils.isEmpty(step.getTargetRelationship())) {
                if (step.getTarget().equals(nodeName) && (step.getActivity() instanceof DelegateWorkflowActivity)) {
                    return step;
                }
            }
        }
        return null;
    }

    public static WorkflowStep getStateStepByNode(Workflow wf, String nodeName, String stateName) {
        for (WorkflowStep step : wf.getSteps().values()) {
            if (StringUtils.isEmpty(step.getTargetRelationship())) {
                if (step.getTarget().equals(nodeName) && isStateStep(step, stateName)) {
                    return step;
                }
            }
        }
        return null;
    }

    public static boolean isStateStep(WorkflowStep step, String stateName) {
        if (step.getActivity() instanceof SetStateWorkflowActivity && ((SetStateWorkflowActivity) step.getActivity()).getStateName().equals(stateName)) {
            return true;
        }
        return false;
    }

    public static void validateName(String name) {
        if (!WORKFLOW_NAME_PATTERN.matcher(name).matches()) {
            throw new InvalidNameException("workflowName", name, "name <" + name
                    + "> is not valid. It should only contains alphanumeric character from the basic Latin alphabet, underscores(_) and dash(-).");
        }
    }

}
