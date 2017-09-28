package org.alien4cloud.tosca.editor.processors.nodetemplate;

import java.util.LinkedHashMap;

import javax.inject.Inject;

import alien4cloud.tosca.topology.TemplateBuilder;
import org.alien4cloud.tosca.catalog.index.IToscaTypeSearchService;
import org.alien4cloud.tosca.editor.EditionContextManager;
import org.alien4cloud.tosca.editor.operations.nodetemplate.AddNodeOperation;
import org.alien4cloud.tosca.editor.processors.IEditorOperationProcessor;
import org.alien4cloud.tosca.model.templates.NodeTemplate;
import org.alien4cloud.tosca.model.templates.Topology;
import org.alien4cloud.tosca.model.types.NodeType;
import org.springframework.stereotype.Component;

import alien4cloud.application.TopologyCompositionService;
import alien4cloud.exception.CyclicReferenceException;
import alien4cloud.paas.wf.WorkflowsBuilderService;
import alien4cloud.topology.TopologyService;
import alien4cloud.topology.TopologyServiceCore;
import alien4cloud.utils.AlienUtils;
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

    @Override
    public void process(AddNodeOperation operation) {
        Topology topology = EditionContextManager.getTopology();

        NameValidationUtils.validateNodeName(operation.getNodeName());
        AlienUtils.failIfExists(topology.getPolicies(), operation.getNodeName(), "A node template with the given name {} already exists in the topology {}.",
                operation.getNodeName(), topology.getId());

        String[] splittedId = operation.getIndexedNodeTypeId().split(":");
        NodeType nodeType = toscaTypeSearchService.findOrFail(NodeType.class, splittedId[0], splittedId[1]);

        if (nodeType.getSubstitutionTopologyId() != null) {
            // TODO merge that in the topologyCompositionService.recursivelyDetectTopologyCompositionCyclicReference
            // it's a try to add this topology's type
            if (nodeType.getSubstitutionTopologyId().equals(topology.getId())) {
                throw new CyclicReferenceException("Cyclic reference : a topology template can not reference itself");
            }
            // detect try to add a substitution topology that indirectly reference this one
            topologyCompositionService.recursivelyDetectTopologyCompositionCyclicReference(topology.getId(), nodeType.getSubstitutionTopologyId());
        }

        if (topology.getNodeTemplates() == null) {
            topology.setNodeTemplates(new LinkedHashMap<>());
        }

        log.debug("Create node template <{}>", operation.getNodeName());

        NodeType loadedIndexedNodeType = topologyService.loadType(topology, nodeType);

        NodeTemplate nodeTemplate = TemplateBuilder.buildNodeTemplate(loadedIndexedNodeType);
        nodeTemplate.setName(operation.getNodeName());
        topology.getNodeTemplates().put(operation.getNodeName(), nodeTemplate);

        log.debug("Adding a new Node template <" + operation.getNodeName() + "> bound to the node type <" + operation.getIndexedNodeTypeId()
                + "> to the topology <" + topology.getId() + "> .");

        WorkflowsBuilderService.TopologyContext topologyContext = workflowBuilderService.buildTopologyContext(topology, EditionContextManager.getCsar());
        workflowBuilderService.addNode(topologyContext, operation.getNodeName());
    }
}