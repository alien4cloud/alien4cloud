package org.alien4cloud.tosca.editor.processors.nodetemplate;

import java.util.HashMap;

import javax.inject.Inject;

import org.alien4cloud.tosca.catalog.index.IToscaTypeSearchService;
import org.alien4cloud.tosca.editor.EditionContextManager;
import org.alien4cloud.tosca.editor.operations.nodetemplate.AddNodeOperation;
import org.alien4cloud.tosca.editor.processors.IEditorOperationProcessor;
import org.alien4cloud.tosca.editor.services.EditorTopologyRecoveryHelperService;
import org.alien4cloud.tosca.model.templates.NodeTemplate;
import org.alien4cloud.tosca.model.templates.Topology;
import org.alien4cloud.tosca.model.types.NodeType;
import org.springframework.stereotype.Component;

import alien4cloud.application.TopologyCompositionService;
import alien4cloud.exception.CyclicReferenceException;
import alien4cloud.exception.NotFoundException;
import alien4cloud.paas.wf.WorkflowsBuilderService;
import alien4cloud.topology.TopologyService;
import alien4cloud.topology.TopologyServiceCore;
import alien4cloud.utils.NameValidationUtils;
import lombok.extern.slf4j.Slf4j;

/**
 * Process an {@link AddNodeOperation}
 */
@Slf4j
@Component
public class AddNodeProcessor implements IEditorOperationProcessor<AddNodeOperation> {
    @Inject
    private IToscaTypeSearchService toscaTypeSearchService;
    @Inject
    private TopologyService topologyService;
    @Inject
    private TopologyServiceCore topologyServiceCore;
    @Inject
    private TopologyCompositionService topologyCompositionService;
    @Inject
    private WorkflowsBuilderService workflowBuilderService;
    @Inject
    private EditorTopologyRecoveryHelperService recoveryHelperService;

    @Override
    public void process(AddNodeOperation operation) {
        Topology topology = EditionContextManager.getTopology();

        NameValidationUtils.validateNodeName(operation.getNodeName());
        topologyService.isUniqueNodeTemplateName(topology, operation.getNodeName());

        String[] splittedId = operation.getIndexedNodeTypeId().split(":");
        NodeType indexedNodeType = toscaTypeSearchService.find(NodeType.class, splittedId[0], splittedId[1]);
        if (indexedNodeType == null) {
            throw new NotFoundException(NodeType.class.getName(), operation.getIndexedNodeTypeId(), "Unable to find node type to create template in topology.");
        }

        if (indexedNodeType.getSubstitutionTopologyId() != null) {
            // TODO merge that in the topologyCompositionService.recursivelyDetectTopologyCompositionCyclicReference
            // it's a try to add this topology's type
            if (indexedNodeType.getSubstitutionTopologyId().equals(topology.getId())) {
                throw new CyclicReferenceException("Cyclic reference : a topology template can not reference itself");
            }
            // detect try to add a substitution topology that indirectly reference this one
            topologyCompositionService.recursivelyDetectTopologyCompositionCyclicReference(topology.getId(), indexedNodeType.getSubstitutionTopologyId());
        }

        if (topology.getNodeTemplates() == null) {
            topology.setNodeTemplates(new HashMap<>());
        }

        log.debug("Create node template <{}>", operation.getNodeName());

        NodeType loadedIndexedNodeType = topologyService.loadType(topology, indexedNodeType);

        NodeTemplate nodeTemplate = topologyServiceCore.buildNodeTemplate(topology.getDependencies(), loadedIndexedNodeType, null);
        nodeTemplate.setName(operation.getNodeName());
        topology.getNodeTemplates().put(operation.getNodeName(), nodeTemplate);

        log.debug("Adding a new Node template <" + operation.getNodeName() + "> bound to the node type <" + operation.getIndexedNodeTypeId()
                + "> to the topology <" + topology.getId() + "> .");

        WorkflowsBuilderService.TopologyContext topologyContext = workflowBuilderService.buildTopologyContext(topology);
        workflowBuilderService.addNode(topologyContext, operation.getNodeName(), nodeTemplate);
    }
}