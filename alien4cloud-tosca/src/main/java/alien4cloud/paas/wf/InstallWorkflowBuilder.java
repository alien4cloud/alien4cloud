package alien4cloud.paas.wf;

import lombok.extern.slf4j.Slf4j;

import org.springframework.stereotype.Component;

import org.alien4cloud.tosca.model.types.RelationshipType;
import org.alien4cloud.tosca.model.templates.NodeTemplate;
import org.alien4cloud.tosca.model.templates.RelationshipTemplate;
import alien4cloud.paas.plan.ToscaNodeLifecycleConstants;
import alien4cloud.paas.wf.WorkflowsBuilderService.TopologyContext;
import alien4cloud.paas.wf.util.WorkflowUtils;
import alien4cloud.tosca.normative.NormativeRelationshipConstants;

@Component
@Slf4j
public class InstallWorkflowBuilder extends StandardWorflowBuilder {

    public void addNode(Workflow wf, String nodeId, TopologyContext toscaTypeFinder, boolean isCompute) {
        // node are systematically added to std lifecycle WFs
        if (WorkflowUtils.isNativeOrSubstitutionNode(nodeId, toscaTypeFinder)) {
            // for a native node, we just add a subworkflow step
            WorkflowUtils.addDelegateWorkflowStep(wf, nodeId);
        } else {
            // search for the hosting parent to find the right step
            AbstractStep lastStep = null; // wf.getStartStep();
            String parentId = WorkflowUtils.getParentId(wf, nodeId, toscaTypeFinder);
            if (parentId != null) {
                // the node is hosted-on something
                if (WorkflowUtils.isNativeOrSubstitutionNode(parentId, toscaTypeFinder)) {
                    // the parent is a native node
                    lastStep = WorkflowUtils.getDelegateWorkflowStepByNode(wf, parentId);
                } else {
                    // the last step is then the setState(STARTED) corresponding to this node
                    NodeActivityStep startedStep = WorkflowUtils.getStateStepByNode(wf, parentId, ToscaNodeLifecycleConstants.STARTED);
                    if (startedStep != null) {
                        lastStep = startedStep;
                    }
                }
            }
            lastStep = appendStateStep(wf, lastStep, nodeId, ToscaNodeLifecycleConstants.INITIAL);
            lastStep = appendStateStep(wf, lastStep, nodeId, ToscaNodeLifecycleConstants.CREATING);

            lastStep = eventuallyAddStdOperationStep(wf, lastStep, nodeId, ToscaNodeLifecycleConstants.CREATE, toscaTypeFinder, false);
            lastStep = appendStateStep(wf, lastStep, nodeId, ToscaNodeLifecycleConstants.CREATED);
            // TODO: here look for DEPENDS_ON relationships ?
            lastStep = appendStateStep(wf, lastStep, nodeId, ToscaNodeLifecycleConstants.CONFIGURING);
            // since relationhip operations are call in 'configure', this is required
            lastStep = eventuallyAddStdOperationStep(wf, lastStep, nodeId, ToscaNodeLifecycleConstants.CONFIGURE, toscaTypeFinder, true);
            lastStep = appendStateStep(wf, lastStep, nodeId, ToscaNodeLifecycleConstants.CONFIGURED);
            lastStep = appendStateStep(wf, lastStep, nodeId, ToscaNodeLifecycleConstants.STARTING);
            // since relationhip operations are call in 'start', this is required
            lastStep = eventuallyAddStdOperationStep(wf, lastStep, nodeId, ToscaNodeLifecycleConstants.START, toscaTypeFinder, true);
            lastStep = appendStateStep(wf, lastStep, nodeId, ToscaNodeLifecycleConstants.STARTED);
        }
    }


    /**
     * 
     */
    @Override
    public void addRelationship(Workflow wf, String nodeId, NodeTemplate nodeTemplate, RelationshipTemplate relationshipTemplate,
            TopologyContext toscaTypeFinder) {

        if (WorkflowUtils.isNativeOrSubstitutionNode(nodeId, toscaTypeFinder)) {
            // for native types we don't care about relation ships in workflows
            return;
        }
        RelationshipType indexedRelationshipType = toscaTypeFinder.findElement(RelationshipType.class, relationshipTemplate.getType());
        String targetId = relationshipTemplate.getTarget();
        boolean targetIsNative = WorkflowUtils.isNativeOrSubstitutionNode(targetId, toscaTypeFinder);
        if (targetIsNative || WorkflowUtils.isOfType(indexedRelationshipType, NormativeRelationshipConstants.HOSTED_ON)) {
            // now the node has a parent, let's sequence the creation
            AbstractStep lastStep = null;
            if (targetIsNative) {
                lastStep = WorkflowUtils.getDelegateWorkflowStepByNode(wf, targetId);
            } else {
                lastStep = WorkflowUtils.getStateStepByNode(wf, targetId, ToscaNodeLifecycleConstants.STARTED);
            }
            NodeActivityStep initSourceStep = WorkflowUtils.getStateStepByNode(wf, nodeId, ToscaNodeLifecycleConstants.INITIAL);
            WorkflowUtils.linkSteps(lastStep, initSourceStep);
        } else {
            // now the node has a parent, let's sequence the creation
            NodeActivityStep configuringTargetStep = WorkflowUtils.getStateStepByNode(wf, targetId, ToscaNodeLifecycleConstants.CONFIGURING);
            NodeActivityStep createdSourceStep = WorkflowUtils.getStateStepByNode(wf, nodeId, ToscaNodeLifecycleConstants.CREATED);
            WorkflowUtils.linkSteps(createdSourceStep, configuringTargetStep);
            NodeActivityStep startedTargetStep = WorkflowUtils.getStateStepByNode(wf, targetId, ToscaNodeLifecycleConstants.STARTED);
            NodeActivityStep configuringSourceStep = WorkflowUtils.getStateStepByNode(wf, nodeId, ToscaNodeLifecycleConstants.CONFIGURING);
            WorkflowUtils.linkSteps(startedTargetStep, configuringSourceStep);
        }
    }

}
