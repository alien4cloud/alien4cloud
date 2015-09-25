package alien4cloud.paas.wf;

import lombok.extern.slf4j.Slf4j;

import org.springframework.stereotype.Component;

import alien4cloud.model.components.IndexedRelationshipType;
import alien4cloud.model.topology.NodeTemplate;
import alien4cloud.model.topology.RelationshipTemplate;
import alien4cloud.paas.plan.ToscaNodeLifecycleConstants;
import alien4cloud.paas.wf.WorkflowsBuilderService.TopologyContext;
import alien4cloud.paas.wf.util.WorkflowUtils;
import alien4cloud.tosca.normative.NormativeRelationshipConstants;

@Component
@Slf4j
public class InstallWorkflowBuilder extends StandardWorflowBuilder {

    public void addNode(Workflow wf, String nodeId, TopologyContext toscaTypeFinder, boolean isCompute) {
        // node are systematically added to std lifecycle WFs

        // search for the hosting parent to find the right step
        AbstractStep lastStep = null; // wf.getStartStep();
        String parentId = WorkflowUtils.getParentId(wf, nodeId, toscaTypeFinder);
        if (parentId != null) {
            // the node is hosted-on something
            // the last step is then the setState(STARTED) corresponding to this node
            NodeActivityStep startedStep = WorkflowUtils.getStateStepByNode(wf, parentId, ToscaNodeLifecycleConstants.STARTED);
            if (startedStep != null) {
                lastStep = startedStep;
            }
        }
        boolean forceOperation = WorkflowUtils.isComputeOrVolume(nodeId, toscaTypeFinder) || WorkflowUtils.isComputeOrNetwork(nodeId, toscaTypeFinder);
        lastStep = appendStateStep(wf, lastStep, nodeId, ToscaNodeLifecycleConstants.INITIAL);
        lastStep = appendStateStep(wf, lastStep, nodeId, ToscaNodeLifecycleConstants.CREATING);
        
        lastStep = eventuallyAddStdOperationStep(wf, lastStep, nodeId, ToscaNodeLifecycleConstants.CREATE, toscaTypeFinder, forceOperation);
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


    @Override
    public void addRelationship(Workflow wf, String nodeId, NodeTemplate nodeTemplate, RelationshipTemplate relationshipTemplate,
            TopologyContext toscaTypeFinder) {
        IndexedRelationshipType indexedRelationshipType = toscaTypeFinder.findElement(IndexedRelationshipType.class, relationshipTemplate.getType());

        if (WorkflowUtils.isOfType(indexedRelationshipType, NormativeRelationshipConstants.HOSTED_ON)) {
            // now the node has a parent, let's sequence the creation
            String parentId = WorkflowUtils.getParentId(wf, nodeId, toscaTypeFinder);
            NodeActivityStep startedTargetStep = WorkflowUtils.getStateStepByNode(wf, parentId, ToscaNodeLifecycleConstants.STARTED);
            NodeActivityStep initSourceStep = WorkflowUtils.getStateStepByNode(wf, nodeId, ToscaNodeLifecycleConstants.INITIAL);
            WorkflowUtils.linkSteps(startedTargetStep, initSourceStep);
        } /*
           * else if (WorkflowUtils.isOfType(indexedRelationshipType, NormativeRelationshipConstants.ATTACH_TO)) {
           * String targetId = relationshipTemplate.getTarget();
           * NodeActivityStep startedTargetStep = WorkflowUtils.getStateStepByNode(wf, targetId, ToscaNodeLifecycleConstants.STARTED);
           * NodeActivityStep initSourceStep = WorkflowUtils.getStateStepByNode(wf, nodeId, ToscaNodeLifecycleConstants.CONFIGURING);
           * WorkflowUtils.linkSteps(startedTargetStep, initSourceStep);
           * }
           */else /* if (WorkflowUtils.isOfType(indexedRelationshipType, NormativeRelationshipConstants.DEPENDS_ON)) */{
            // now the node has a parent, let's sequence the creation
            String targetId = relationshipTemplate.getTarget();
            NodeActivityStep configuringTargetStep = WorkflowUtils.getStateStepByNode(wf, targetId, ToscaNodeLifecycleConstants.CONFIGURING);
            NodeActivityStep createdSourceStep = WorkflowUtils.getStateStepByNode(wf, nodeId, ToscaNodeLifecycleConstants.CREATED);
            WorkflowUtils.linkSteps(createdSourceStep, configuringTargetStep);
            NodeActivityStep startedTargetStep = WorkflowUtils.getStateStepByNode(wf, targetId, ToscaNodeLifecycleConstants.STARTED);
            NodeActivityStep configuringSourceStep = WorkflowUtils.getStateStepByNode(wf, nodeId, ToscaNodeLifecycleConstants.CONFIGURING);
            WorkflowUtils.linkSteps(startedTargetStep, configuringSourceStep);
        }
    }

}
