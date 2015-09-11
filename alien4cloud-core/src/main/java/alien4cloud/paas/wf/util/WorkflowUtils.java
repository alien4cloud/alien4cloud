package alien4cloud.paas.wf.util;

import java.util.Map;

import alien4cloud.model.components.Interface;
import alien4cloud.paas.model.PaaSNodeTemplate;
import alien4cloud.paas.wf.AbstractStep;
import alien4cloud.paas.wf.NodeActivityStep;
import alien4cloud.paas.wf.OperationCallActivity;
import alien4cloud.paas.wf.SetStateActivity;
import alien4cloud.paas.wf.Workflow;
import alien4cloud.tosca.normative.NormativeComputeConstants;

public class WorkflowUtils {

    private static final String NETWORK_TYPE = "tosca.nodes.Network";

    public static Interface getNodeInterface(PaaSNodeTemplate nodeTemplate, String interfaceName) {
        Interface interfaz = getInterface(interfaceName, nodeTemplate.getIndexedToscaElement().getInterfaces());
        if (interfaz == null) {
            throw new IllegalArgumentException("Plan cannot be generated as required interface <" + interfaceName + "> has not been found on node <"
                    + nodeTemplate.getNodeTemplate().getName() + "> from type <" + nodeTemplate.getNodeTemplate().getType() + ">.");
        }
        return interfaz;
    }

    public static Interface getInterface(String interfaceName, Map<String, Interface> interfaces) {
        return interfaces == null ? null : interfaces.get(interfaceName);
    }

    public static boolean isCompute(PaaSNodeTemplate paaSNodeTemplate) {
        return isOfType(paaSNodeTemplate, NormativeComputeConstants.COMPUTE_TYPE);
    }

    public static boolean isNetwork(PaaSNodeTemplate paaSNodeTemplate) {
        return isOfType(paaSNodeTemplate, NETWORK_TYPE);
    }

    public static boolean isVolume(PaaSNodeTemplate paaSNodeTemplate) {
        return isOfType(paaSNodeTemplate, "tosca.nodes.BlockStorage");
    }

    private static boolean isOfType(PaaSNodeTemplate paaSNodeTemplate, String type) {
        return paaSNodeTemplate.getIndexedToscaElement().getElementId().equals(type) || paaSNodeTemplate.getIndexedToscaElement().getDerivedFrom() != null
                && paaSNodeTemplate.getIndexedToscaElement().getDerivedFrom().contains(type);
    }

    public static void linkSteps(AbstractStep from, AbstractStep to) {
        if (from != null && to != null) {
            from.addFollowing(to.getName());
            to.addPreceding(from.getName());
        }
    }

    public static String buildStepName(Workflow wf, NodeActivityStep step, int increment) {
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
