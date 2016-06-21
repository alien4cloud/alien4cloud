package org.alien4cloud.tosca.editor.processors;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import javax.annotation.Resource;

import org.alien4cloud.tosca.editor.TopologyEditionContextManager;
import org.alien4cloud.tosca.editor.operations.AddNodeOperation;
import org.springframework.stereotype.Component;

import alien4cloud.application.TopologyCompositionService;
import alien4cloud.dao.IGenericSearchDAO;
import alien4cloud.exception.AlreadyExistException;
import alien4cloud.exception.CyclicReferenceException;
import alien4cloud.exception.InvalidNodeNameException;
import alien4cloud.exception.NotFoundException;
import alien4cloud.model.components.IndexedNodeType;
import alien4cloud.model.templates.TopologyTemplate;
import alien4cloud.model.topology.NodeTemplate;
import alien4cloud.model.topology.Topology;
import alien4cloud.paas.wf.WorkflowsBuilderService;
import alien4cloud.topology.TopologyService;
import alien4cloud.topology.TopologyUtils;
import lombok.extern.slf4j.Slf4j;

/**
 * Process an add node template operation.
 */
@Slf4j
@Component
public class AddNodeTemplateProcessor implements IEditorOperationProcessor<AddNodeOperation> {
    @Resource(name = "alien-es-dao")
    private IGenericSearchDAO alienDAO;
    @Resource
    private TopologyService topologyService;
    @Resource
    private TopologyCompositionService topologyCompositionService;
    @Resource
    private WorkflowsBuilderService workflowBuilderService;

    @Override
    public void process(AddNodeOperation operation) {
        Topology topology = TopologyEditionContextManager.getTopology();

        if (!TopologyUtils.isValidNodeName(operation.getNodeName())) {
            throw new InvalidNodeNameException("A name should only contains alphanumeric character from the basic Latin alphabet and the underscore.");
        }

        topologyService.isUniqueNodeTemplateName(topology, operation.getNodeName());

        IndexedNodeType indexedNodeType = alienDAO.findById(IndexedNodeType.class, operation.getIndexedNodeTypeId());
        if (indexedNodeType == null) {
            throw new NotFoundException(IndexedNodeType.class.getName(), operation.getIndexedNodeTypeId(),
                    "Unable to find node type to create template in topology.");
        }

        if (indexedNodeType.getSubstitutionTopologyId() != null && topology.getDelegateType().equalsIgnoreCase(TopologyTemplate.class.getSimpleName())) {
            // TODO merge that in the topologyCompositionService.recursivelyDetectTopologyCompositionCyclicReference
            // it's a try to add this topology's type
            if (indexedNodeType.getSubstitutionTopologyId().equals(topology.getId())) {
                throw new CyclicReferenceException("Cyclic reference : a topology template can not reference itself");
            }
            // detect try to add a substitution topology that indirectly reference this one
            topologyCompositionService.recursivelyDetectTopologyCompositionCyclicReference(topology.getId(), indexedNodeType.getSubstitutionTopologyId());
        }

        if (topology.getNodeTemplates() == null) {
            topology.setNodeTemplates(new HashMap<String, NodeTemplate>());
        }

        Map<String, NodeTemplate> nodeTemplates = topology.getNodeTemplates();
        Set<String> nodeTemplatesNames = nodeTemplates.keySet();
        if (nodeTemplatesNames.contains(operation.getNodeName())) {
            log.debug("Add Node Template <{}> impossible (already exists)", operation.getNodeName());
            // a node template already exist with the given name.
            throw new AlreadyExistException("A node template with the given name already exists.");
        } else {
            log.debug("Create node template <{}>", operation.getNodeName());
        }

        // FIXME update the tosca context here.
        indexedNodeType = topologyService.loadType(topology, indexedNodeType);

        NodeTemplate nodeTemplate = topologyService.buildNodeTemplate(topology.getDependencies(), indexedNodeType, null);
        nodeTemplate.setName(operation.getNodeName());
        topology.getNodeTemplates().put(operation.getNodeName(), nodeTemplate);

        log.debug("Adding a new Node template <" + operation.getNodeName() + "> bound to the node type <" + operation.getIndexedNodeTypeId()
                + "> to the topology <" + topology.getId() + "> .");

        WorkflowsBuilderService.TopologyContext topologyContext = workflowBuilderService.buildTopologyContext(topology);
        workflowBuilderService.addNode(topologyContext, operation.getNodeName(), nodeTemplate);
    }
}