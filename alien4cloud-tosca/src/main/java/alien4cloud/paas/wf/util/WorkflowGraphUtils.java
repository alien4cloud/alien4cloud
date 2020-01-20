package alien4cloud.paas.wf.util;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.alien4cloud.tosca.model.definitions.Interface;
import org.alien4cloud.tosca.model.definitions.Operation;
import org.alien4cloud.tosca.model.templates.AbstractInstantiableTemplate;
import org.alien4cloud.tosca.model.templates.NodeTemplate;
import org.alien4cloud.tosca.model.templates.RelationshipTemplate;
import org.alien4cloud.tosca.model.templates.Topology;
import org.alien4cloud.tosca.model.types.AbstractInstantiableToscaType;
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
import org.alien4cloud.tosca.model.workflow.declarative.RelationshipOperationHost;
import org.alien4cloud.tosca.normative.ToscaNormativeUtil;
import org.apache.commons.lang3.StringUtils;

import alien4cloud.exception.NotFoundException;
import alien4cloud.paas.wf.TopologyContext;
import alien4cloud.paas.wf.model.Path;
import alien4cloud.utils.AlienUtils;

public class WorkflowGraphUtils {

    private enum Color { WHITE, GRAY, BLACK};

    public static List<Path> getWorkflowGraphCycles(Workflow workflow) {
        List<Path> cycles = new ArrayList<>();

        Map<String,Color> colors = workflow.getSteps().keySet().stream().collect(Collectors.toMap(Function.identity(),x -> Color.WHITE));

        Path path = new Path();

        for (WorkflowStep step : workflow.getSteps().values()) {
            if (colors.get(step.getName()) == Color.WHITE) {
                if (doGetWorkflowGraphCycles(workflow,step,colors,path) == true) {
                    cycles.add(path);
                    return cycles;
                }
            }
        }

        return cycles;
    }

    private static boolean doGetWorkflowGraphCycles(Workflow workflow,WorkflowStep step,Map<String,Color> colors,Path path) {
        colors.put(step.getName(),Color.GRAY);

        path.add(step);

        for (String stepName : step.getOnSuccess()) {
                WorkflowStep nextStep = workflow.getSteps().get(stepName);

                if (colors.get(stepName) == Color.GRAY) {
                    path.setCycle(true);
                    path.setLoopingStep(nextStep);
                    return true;
                }

                if (colors.get(stepName) == Color.WHITE && doGetWorkflowGraphCycles(workflow,nextStep,colors,path)  == true) {
                    return true;
                }
        }

        path.remove(step);

        colors.put(step.getName(),Color.BLACK);

        return false;
    }

        /**
         * Build the paths of the graph starting from the entry points (steps without predecessors, so connected to 'start').
         * <p>
         * Will also detect orphans brothers in the entire graph (cycles not connected to start).
         */
    public static List<Path> getOldWorkflowGraphCycles(Workflow workflow) {
        SubGraph subGraph = new SubGraph(workflow, stepId -> true);
        List<Path> cycles = new ArrayList<>();
        subGraph.browse(new SimpleGraphConsumer() {
            @Override
            public boolean onNewPath(List<WorkflowStep> path) {
                if (path.size() > 1) {
                    Path parentPath = new Path(path.subList(0, path.size() - 1));
                    WorkflowStep currentStep = path.get(path.size() - 1);
                    if (parentPath.contains(currentStep)) {
                        Path cycle = new Path(path);
                        cycle.setCycle(true);
                        cycle.setLoopingStep(currentStep);
                        cycles.add(cycle);
                        // abort so that do not run into loop
                        return false;
                    }
                }
                return true;
            }
        });
        return cycles;
    }

    public static String getConcernedNodeName(WorkflowStep stepFound, Topology topology) {
        if (stepFound instanceof NodeWorkflowStep) {
            return stepFound.getTarget();
        } else if (stepFound instanceof RelationshipWorkflowStep) {
            RelationshipWorkflowStep relationshipWorkflowStepFound = (RelationshipWorkflowStep) stepFound;
            if (RelationshipOperationHost.SOURCE.toString().equals(relationshipWorkflowStepFound.getOperationHost())) {
                return relationshipWorkflowStepFound.getTarget();
            } else if (RelationshipOperationHost.TARGET.toString().equals(relationshipWorkflowStepFound.getOperationHost())) {
                return topology.getNodeTemplates().get(relationshipWorkflowStepFound.getTarget()).getRelationships()
                        .get(relationshipWorkflowStepFound.getTargetRelationship()).getTarget();
            }
        }
        throw new NotFoundException("This is a bug, no node can be found concerning the step " + stepFound.getStepAsString());
    }

    public static boolean isStepEmpty(WorkflowStep step, TopologyContext topologyContext) {
        // No activity
        if (step.getActivity() == null) {
            return true;
        }
        // Delegate activity is managed by orchestrator so it has implementation
        // State activity is never empty
        // Inline activity is never empty
        if (step.getActivity() instanceof DelegateWorkflowActivity || step.getActivity() instanceof SetStateWorkflowActivity
                || step.getActivity() instanceof InlineWorkflowActivity) {
            return false;
        }
        CallOperationWorkflowActivity callOperationWorkflowActivity = (CallOperationWorkflowActivity) step.getActivity();
        NodeTemplate nodeTemplate = topologyContext.getTopology().getNodeTemplates().get(step.getTarget());
        String stepInterfaceName = ToscaNormativeUtil.getLongInterfaceName(callOperationWorkflowActivity.getInterfaceName());
        String stepOperationName = callOperationWorkflowActivity.getOperationName();
        if (step instanceof NodeWorkflowStep) {
            return !hasImplementation(nodeTemplate, NodeType.class, topologyContext, stepInterfaceName, stepOperationName);
        } else if (step instanceof RelationshipWorkflowStep) {
            RelationshipWorkflowStep relationshipWorkflowStep = (RelationshipWorkflowStep) step;
            RelationshipTemplate relationshipTemplate = nodeTemplate.getRelationships().get(relationshipWorkflowStep.getTargetRelationship());
            return !hasImplementation(relationshipTemplate, RelationshipType.class, topologyContext, stepInterfaceName, stepOperationName);
        } else {
            return false;
        }
    }

    private static <T extends AbstractInstantiableTemplate, U extends AbstractInstantiableToscaType> boolean hasImplementation(T template,
            Class<U> templateClass, TopologyContext topologyContext, String stepInterfaceName, String stepOperationName) {
        if (hasImplementation(template.getInterfaces(), stepInterfaceName, stepOperationName)) {
            // The operation is overridden on the node level and is not empty
            return true;
        } else {
            U templateType = topologyContext.findElement(templateClass, template.getType());
            if (templateType == null) {
                throw new NotFoundException("Template Type " + template.getType() + " cannot be found");
            } else {
                return hasImplementation(templateType.getInterfaces(), stepInterfaceName, stepOperationName);
            }
        }
    }

    private static boolean hasImplementation(Map<String, Interface> interfaceMap, String stepInterfaceName, String stepOperationName) {
        Optional<Interface> foundInterface = AlienUtils.safe(interfaceMap).entrySet().stream()
                .filter(ie -> ToscaNormativeUtil.getLongInterfaceName(ie.getKey()).equals(stepInterfaceName)).map(Map.Entry::getValue).findFirst();
        return foundInterface.map(anInterface -> {
            Operation operation = anInterface.getOperations().get(stepOperationName);
            return operation != null && operation.getImplementationArtifact() != null
                    && StringUtils.isNotBlank(operation.getImplementationArtifact().getArtifactRef());
        }).orElse(false);
    }

    public static Map<String, WorkflowStep> getAllStepsInSubGraph(Workflow workflow, SubGraphFilter filter) {
        return workflow.getSteps().entrySet().stream().filter(workflowEntry -> filter.isInSubGraph(workflowEntry.getKey()))
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
    }
}
