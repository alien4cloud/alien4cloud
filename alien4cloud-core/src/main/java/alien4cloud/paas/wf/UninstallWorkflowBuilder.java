package alien4cloud.paas.wf;

import lombok.extern.slf4j.Slf4j;

import org.springframework.stereotype.Component;

import alien4cloud.paas.model.PaaSNodeTemplate;
import alien4cloud.paas.model.PaaSRelationshipTemplate;
import alien4cloud.paas.model.PaaSTopology;
import alien4cloud.paas.plan.ToscaNodeLifecycleConstants;
import alien4cloud.paas.wf.util.WorkflowUtils;
import alien4cloud.tosca.normative.NormativeRelationshipConstants;

@Component
@Slf4j
public class UninstallWorkflowBuilder extends StandardWorflowBuilder {

    @Override
    public void addNode(Workflow wf, PaaSTopology paaSTopology, PaaSNodeTemplate paaSNodeTemplate, boolean isCompute) {
        AbstractStep lastStep = null;
        // TODO: look for children ?
        lastStep = appendStateStep(wf, lastStep, paaSNodeTemplate, ToscaNodeLifecycleConstants.STOPPING);
        lastStep = eventuallyAddStdOperationStep(wf, lastStep, paaSNodeTemplate, ToscaNodeLifecycleConstants.STOP, isCompute);
        lastStep = appendStateStep(wf, lastStep, paaSNodeTemplate, ToscaNodeLifecycleConstants.STOPPED);
        lastStep = appendStateStep(wf, lastStep, paaSNodeTemplate, ToscaNodeLifecycleConstants.DELETING);
        lastStep = eventuallyAddStdOperationStep(wf, lastStep, paaSNodeTemplate, ToscaNodeLifecycleConstants.DELETE, isCompute);
        lastStep = appendStateStep(wf, lastStep, paaSNodeTemplate, ToscaNodeLifecycleConstants.DELETED);
    }

    @Override
    public void addRelationship(Workflow wf, PaaSTopology paaSTopology, PaaSNodeTemplate paaSNodeTemplate, PaaSRelationshipTemplate pasSRelationshipTemplate) {
        if (pasSRelationshipTemplate.instanceOf(NormativeRelationshipConstants.HOSTED_ON)) {
            // now the node has a parent, let's sequence the deletion (children before parent)
            PaaSNodeTemplate parent = paaSNodeTemplate.getParent();
            NodeActivityStep deletedSourceStep = WorkflowUtils.getStateStepByNode(wf, paaSNodeTemplate.getId(), ToscaNodeLifecycleConstants.DELETED);
            NodeActivityStep stoppingTargetStep = WorkflowUtils.getStateStepByNode(wf, parent.getId(), ToscaNodeLifecycleConstants.STOPPING);
            WorkflowUtils.linkSteps(deletedSourceStep, stoppingTargetStep);
        } else if (pasSRelationshipTemplate.instanceOf(NormativeRelationshipConstants.ATTACH_TO)) {
            // in case of "Volume attached to Compute", we need to delete the compute before eventually delete the volume
            String volumeId = paaSNodeTemplate.getId();
            String computeId = pasSRelationshipTemplate.getRelationshipTemplate().getTarget();
            NodeActivityStep deletedComputeStep = WorkflowUtils.getStateStepByNode(wf, computeId, ToscaNodeLifecycleConstants.DELETED);
            NodeActivityStep stoppingVolumeStep = WorkflowUtils.getStateStepByNode(wf, volumeId, ToscaNodeLifecycleConstants.STOPPING);
            WorkflowUtils.linkSteps(deletedComputeStep, stoppingVolumeStep);
        }
    }

}
