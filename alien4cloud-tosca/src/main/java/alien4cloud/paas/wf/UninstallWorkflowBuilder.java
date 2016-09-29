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
public class UninstallWorkflowBuilder extends StandardWorflowBuilder {

    @Override
    public void addNode(Workflow wf, String nodeId, TopologyContext toscaTypeFinder, boolean isCompute) {
        if (WorkflowUtils.isNativeOrSubstitutionNode(nodeId, toscaTypeFinder)) {
            // for a native node, we just add a subworkflow step
            WorkflowUtils.addDelegateWorkflowStep(wf, nodeId);
        } else {
            AbstractStep lastStep = null;
            // TODO: look for children ?
            lastStep = appendStateStep(wf, lastStep, nodeId, ToscaNodeLifecycleConstants.STOPPING);
            lastStep = eventuallyAddStdOperationStep(wf, lastStep, nodeId, ToscaNodeLifecycleConstants.STOP, toscaTypeFinder, false);
            lastStep = appendStateStep(wf, lastStep, nodeId, ToscaNodeLifecycleConstants.STOPPED);
            lastStep = appendStateStep(wf, lastStep, nodeId, ToscaNodeLifecycleConstants.DELETING);
            lastStep = eventuallyAddStdOperationStep(wf, lastStep, nodeId, ToscaNodeLifecycleConstants.DELETE, toscaTypeFinder, false);
            lastStep = appendStateStep(wf, lastStep, nodeId, ToscaNodeLifecycleConstants.DELETED);
        }
    }

    @Override
    public void addRelationship(Workflow wf, String nodeId, NodeTemplate nodeTemplate, RelationshipTemplate relationshipTemplate,
            TopologyContext toscaTypeFinder) {
        RelationshipType indexedRelationshipType = toscaTypeFinder.findElement(RelationshipType.class, relationshipTemplate.getType());
        String targetId = relationshipTemplate.getTarget();
        boolean targetIsNative = WorkflowUtils.isNativeOrSubstitutionNode(targetId, toscaTypeFinder);
        if (targetIsNative || WorkflowUtils.isOfType(indexedRelationshipType, NormativeRelationshipConstants.HOSTED_ON)) {
            // now the node has a parent, let's sequence the deletion (children before parent)
            String parentId = WorkflowUtils.getParentId(wf, nodeId, toscaTypeFinder);
            NodeActivityStep deletedSourceStep = WorkflowUtils.getStateStepByNode(wf, nodeId, ToscaNodeLifecycleConstants.DELETED);
            AbstractStep targetStep = null;
            if (targetIsNative) {
                targetStep = WorkflowUtils.getDelegateWorkflowStepByNode(wf, targetId);
            } else {
                targetStep = WorkflowUtils.getStateStepByNode(wf, parentId, ToscaNodeLifecycleConstants.STOPPING);
            }
            WorkflowUtils.linkSteps(deletedSourceStep, targetStep);
        } else if (WorkflowUtils.isOfType(indexedRelationshipType, NormativeRelationshipConstants.CONNECTS_TO)) {
            // in case of "Volume attached to Compute", we need to delete the compute before eventually delete the volume
            String volumeId = nodeId;
            NodeActivityStep targetStep = WorkflowUtils.getStateStepByNode(wf, targetId, ToscaNodeLifecycleConstants.STOPPING);
            NodeActivityStep sourceStep = WorkflowUtils.getStateStepByNode(wf, nodeId, ToscaNodeLifecycleConstants.STOPPED);
            WorkflowUtils.linkSteps(sourceStep, targetStep);
        }
    }

}
