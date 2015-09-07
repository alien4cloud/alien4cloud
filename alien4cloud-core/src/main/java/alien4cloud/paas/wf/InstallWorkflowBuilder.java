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
public class InstallWorkflowBuilder extends StandardWorflowBuilder {

    public void addNode(Workflow wf, PaaSTopology paaSTopology, PaaSNodeTemplate paaSNodeTemplate, boolean isCompute) {
        // node are systematically added to std lifecycle WFs

        // search for the hosting parent to find the right step
        AbstractStep lastStep = null; // wf.getStartStep();
        PaaSNodeTemplate parent = paaSNodeTemplate.getParent();
        if (parent != null) {
            // the node is hosted-on something
            // the last step is then the setState(STARTED) corresponding to this node
            NodeActivityStep startedStep = getStateStepByNode(wf, parent.getId(), ToscaNodeLifecycleConstants.STARTED);
            if (startedStep != null) {
                lastStep = startedStep;
            }
        }

        lastStep = appendStateStep(wf, lastStep, paaSNodeTemplate, ToscaNodeLifecycleConstants.INITIAL);
        lastStep = appendStateStep(wf, lastStep, paaSNodeTemplate, ToscaNodeLifecycleConstants.CREATING);
        lastStep = eventuallyAddStdOperationStep(wf, lastStep, paaSNodeTemplate, ToscaNodeLifecycleConstants.CREATE, isCompute);
        lastStep = appendStateStep(wf, lastStep, paaSNodeTemplate, ToscaNodeLifecycleConstants.CREATED);
        // TODO: here look for DEPENDS_ON relationships ?
        lastStep = appendStateStep(wf, lastStep, paaSNodeTemplate, ToscaNodeLifecycleConstants.CONFIGURING);
        lastStep = eventuallyAddStdOperationStep(wf, lastStep, paaSNodeTemplate, ToscaNodeLifecycleConstants.CONFIGURE, isCompute);
        lastStep = appendStateStep(wf, lastStep, paaSNodeTemplate, ToscaNodeLifecycleConstants.CONFIGURED);
        lastStep = appendStateStep(wf, lastStep, paaSNodeTemplate, ToscaNodeLifecycleConstants.STARTING);
        lastStep = eventuallyAddStdOperationStep(wf, lastStep, paaSNodeTemplate, ToscaNodeLifecycleConstants.START, isCompute);
        lastStep = appendStateStep(wf, lastStep, paaSNodeTemplate, ToscaNodeLifecycleConstants.STARTED);
    }

    public void addRelationship(Workflow wf, PaaSTopology paaSTopology, PaaSNodeTemplate paaSNodeTemplate, PaaSRelationshipTemplate pasSRelationshipTemplate) {
        if (pasSRelationshipTemplate.instanceOf(NormativeRelationshipConstants.HOSTED_ON)) {
            // now the node has a parent, let's sequence the creation
            PaaSNodeTemplate parent = paaSNodeTemplate.getParent();
            NodeActivityStep startedTargetStep = getStateStepByNode(wf, parent.getId(), ToscaNodeLifecycleConstants.STARTED);
            NodeActivityStep initSourceStep = getStateStepByNode(wf, paaSNodeTemplate.getId(), ToscaNodeLifecycleConstants.INITIAL);
            WorkflowUtils.linkSteps(startedTargetStep, initSourceStep);
        } else if (pasSRelationshipTemplate.instanceOf(NormativeRelationshipConstants.DEPENDS_ON)) {
            // now the node has a parent, let's sequence the creation
            String targetId = pasSRelationshipTemplate.getRelationshipTemplate().getTarget();
            NodeActivityStep startedTargetStep = getStateStepByNode(wf, targetId, ToscaNodeLifecycleConstants.STARTED);
            NodeActivityStep configureSourceStep = getStateStepByNode(wf, paaSNodeTemplate.getId(), ToscaNodeLifecycleConstants.CONFIGURING);
            WorkflowUtils.linkSteps(startedTargetStep, configureSourceStep);
        }
    }

}
