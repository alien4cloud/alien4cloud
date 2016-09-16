package alien4cloud.paas.wf.util;

import java.util.Map;

import org.alien4cloud.tosca.model.types.AbstractInheritableToscaType;
import org.alien4cloud.tosca.model.types.NodeType;
import org.alien4cloud.tosca.model.types.RelationshipType;
import org.alien4cloud.tosca.model.definitions.Interface;
import org.alien4cloud.tosca.model.definitions.Operation;
import org.alien4cloud.tosca.model.templates.NodeTemplate;
import org.alien4cloud.tosca.model.templates.RelationshipTemplate;
import alien4cloud.paas.wf.AbstractStep;
import alien4cloud.paas.wf.DelegateWorkflowActivity;
import alien4cloud.paas.wf.NodeActivityStep;
import alien4cloud.paas.wf.OperationCallActivity;
import alien4cloud.paas.wf.SetStateActivity;
import alien4cloud.paas.wf.Workflow;
import alien4cloud.paas.wf.WorkflowsBuilderService.TopologyContext;
import alien4cloud.tosca.normative.NormativeComputeConstants;
import alien4cloud.tosca.normative.NormativeRelationshipConstants;

public class WorkflowUtils {

    private static final String NETWORK_TYPE = "tosca.nodes.Network";

    private static String getRootHostNode(String nodeId, TopologyContext topologyContext) {
        NodeTemplate nodeTemplate = topologyContext.getTopology().getNodeTemplates().get(nodeId);
        if (nodeTemplate == null) {
            return null;
        }
        NodeType nodeType = (NodeType) topologyContext.findElement(NodeType.class, nodeTemplate.getType());
        if (isOfType(nodeType, NormativeComputeConstants.COMPUTE_TYPE)) {
            return nodeId;
        } else {
            if (nodeTemplate.getRelationships() != null) {
                for (RelationshipTemplate relationshipTemplate : nodeTemplate.getRelationships().values()) {
                    RelationshipType relationshipType = (RelationshipType) topologyContext.findElement(RelationshipType.class, relationshipTemplate.getType());
                    if (isOfType(relationshipType, NormativeRelationshipConstants.HOSTED_ON)) {
                        return getRootHostNode(relationshipTemplate.getTarget(), topologyContext);
                    }
                }
            }
            return null;
        }
    }

    public static boolean isStandardWorkflow(Workflow workflow) {
        return Workflow.INSTALL_WF.equals(workflow.getName()) || Workflow.UNINSTALL_WF.equals(workflow.getName());
    }

    /**
     * Compute the wf in order to ensure that all step are tagged with the hostId property.
     * <p/>
     * The hostId is the first (and normally unique) compute found in the ascendency.
     */
    public static void fillHostId(Workflow wf, TopologyContext topologyContext) {
        wf.getHosts().clear();
        for (AbstractStep step : wf.getSteps().values()) {
            if (step instanceof NodeActivityStep) {
                NodeActivityStep dstep = (NodeActivityStep) step;
                String hostId = WorkflowUtils.getRootHostNode(dstep.getNodeId(), topologyContext);
                dstep.setHostId(hostId);
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

    public static void linkSteps(AbstractStep from, AbstractStep to) {
        if (from != null && to != null) {
            from.addFollowing(to.getName());
            to.addPreceding(from.getName());
        }
    }

    public static String buildStepName(Workflow wf, AbstractStep step, int increment) {
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

    public static boolean isStateStep(AbstractStep step) {
        return (step instanceof NodeActivityStep) && ((NodeActivityStep) step).getActivity() instanceof SetStateActivity;
    }

    public static String debugWorkflow(Workflow wf) {
        StringBuilder stringBuilder = new StringBuilder("\n ======> Paste the folowing graph in http://www.webgraphviz.com/  !!\n");
        int subgraphCount = 0;
        stringBuilder.append("\ndigraph ").append(wf.getName()).append(" {");
        stringBuilder.append("\n  node [shape=box];");

        for (String host : wf.getHosts()) {
            stringBuilder.append("\n  subgraph cluster_").append(++subgraphCount).append(" {");
            stringBuilder.append("\n    label = \"").append(host).append("\";\n    color=blue;");
            for (AbstractStep step : wf.getSteps().values()) {
                if (step instanceof NodeActivityStep && host.equals(((NodeActivityStep) step).getHostId())) {
                    stringBuilder.append("\n    \"").append(step.getName()).append("\";");
                }
            }
            stringBuilder.append("\n  }\n");
        }
        for (AbstractStep step : wf.getSteps().values()) {
            if (step.getFollowingSteps() != null) {
                for (String following : step.getFollowingSteps()) {
                    stringBuilder.append("\n  \"").append(step.getName()).append("\" -> \"").append(following).append("\";");
                }
            }
            if (step.getFollowingSteps() == null || step.getFollowingSteps().isEmpty()) {
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

    public static NodeActivityStep addOperationStep(Workflow wf, String nodeId, String interfaceName, String operationName) {
        OperationCallActivity task = new OperationCallActivity();
        task.setInterfaceName(interfaceName);
        task.setOperationName(operationName);
        task.setNodeId(nodeId);
        NodeActivityStep step = new NodeActivityStep();
        step.setNodeId(nodeId);
        step.setActivity(task);
        step.setName(buildStepName(wf, step, 0));
        wf.addStep(step);
        return step;
    }

    public static NodeActivityStep addStateStep(Workflow wf, String nodeId, String stateName) {
        SetStateActivity task = new SetStateActivity();
        task.setStateName(stateName);
        task.setNodeId(nodeId);
        NodeActivityStep step = new NodeActivityStep();
        step.setNodeId(nodeId);
        step.setActivity(task);
        step.setName(buildStepName(wf, step, 0));
        wf.addStep(step);
        return step;
    }

    public static NodeActivityStep addDelegateWorkflowStep(Workflow wf, String nodeId) {
        DelegateWorkflowActivity activity = new DelegateWorkflowActivity();
        activity.setNodeId(nodeId);
        activity.setWorkflowName(wf.getName());
        NodeActivityStep step = new NodeActivityStep();
        step.setNodeId(nodeId);
        step.setActivity(activity);
        step.setName(buildStepName(wf, step, 0));
        wf.addStep(step);
        return step;
    }

    public static AbstractStep getDelegateWorkflowStepByNode(Workflow wf, String nodeName) {
        for (AbstractStep step : wf.getSteps().values()) {
            if (step instanceof NodeActivityStep) {
                NodeActivityStep defaultStep = (NodeActivityStep) step;
                if (defaultStep.getNodeId().equals(nodeName) && (defaultStep.getActivity() instanceof DelegateWorkflowActivity)) {
                    return defaultStep;
                }
            }
        }
        return null;
    }

    public static NodeActivityStep getStateStepByNode(Workflow wf, String nodeName, String stateName) {
        for (AbstractStep step : wf.getSteps().values()) {
            if (step instanceof NodeActivityStep) {
                NodeActivityStep defaultStep = (NodeActivityStep) step;
                if (defaultStep.getActivity().getNodeId().equals(nodeName) && isStateStep(defaultStep, stateName)) {
                    return defaultStep;
                }
            }
        }
        return null;
    }

    public static boolean isStateStep(NodeActivityStep defaultStep, String stateName) {
        if (defaultStep.getActivity() instanceof SetStateActivity && ((SetStateActivity) defaultStep.getActivity()).getStateName().equals(stateName)) {
            return true;
        }
        return false;
    }

}
