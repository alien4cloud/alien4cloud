package alien4cloud.paas.wf;

import lombok.extern.slf4j.Slf4j;

import org.springframework.stereotype.Component;

import alien4cloud.paas.model.PaaSNodeTemplate;
import alien4cloud.paas.model.PaaSRelationshipTemplate;
import alien4cloud.paas.model.PaaSTopology;
import alien4cloud.paas.plan.ToscaNodeLifecycleConstants;
import alien4cloud.tosca.normative.NormativeRelationshipConstants;

@Component
@Slf4j
public class UninstallWorkflowBuilder extends AbstractWorkflowBuilder {

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
            NodeActivityStep deletedSourceStep = getStateStepByNode(wf, paaSNodeTemplate.getId(), ToscaNodeLifecycleConstants.DELETED);
            NodeActivityStep stoppingTargetStep = getStateStepByNode(wf, parent.getId(), ToscaNodeLifecycleConstants.STOPPING);
            linkSteps(deletedSourceStep, stoppingTargetStep);
        }
    }

}
