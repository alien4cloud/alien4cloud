package org.alien4cloud.tosca.editor.processors.nodetemplate;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

import javax.inject.Inject;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;

import org.alien4cloud.alm.deployment.configuration.flow.FlowExecutionContext;
import org.alien4cloud.tosca.catalog.index.IToscaTypeSearchService;
import org.alien4cloud.tosca.editor.Constants;
import org.alien4cloud.tosca.editor.operations.nodetemplate.AddNodeOperation;
import org.alien4cloud.tosca.editor.processors.IEditorOperationProcessor;
import org.alien4cloud.tosca.model.Csar;
import org.alien4cloud.tosca.model.templates.NodeTemplate;
import org.alien4cloud.tosca.model.templates.Topology;
import org.alien4cloud.tosca.model.types.NodeType;
import org.alien4cloud.tosca.services.DanglingRequirementService;
import org.springframework.stereotype.Component;

import alien4cloud.application.TopologyCompositionService;
import alien4cloud.exception.CyclicReferenceException;
import alien4cloud.model.common.Tag;
import alien4cloud.paas.wf.TopologyContext;
import alien4cloud.paas.wf.WorkflowsBuilderService;
import alien4cloud.topology.TopologyService;
import alien4cloud.tosca.topology.TemplateBuilder;
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
    private TopologyCompositionService topologyCompositionService;
    @Inject
    private WorkflowsBuilderService workflowBuilderService;
    @Inject
    private DanglingRequirementService danglingRequirementService;

    @Override
    public void process(Csar csar, Topology topology, AddNodeOperation operation) {
        NameValidationUtils.validateNodeName(operation.getNodeName());
        AlienUtils.failIfExists(topology.getNodeTemplates(), operation.getNodeName(),
                "A node template with the given name {} already exists in the topology {}.", operation.getNodeName(), topology.getId());

        NodeType nodeType = toscaTypeSearchService.findByIdOrFail(NodeType.class, operation.getIndexedNodeTypeId());

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

        log.debug("Create node template [ {} ]", operation.getNodeName());

        NodeType loadedIndexedNodeType = topologyService.loadType(topology, nodeType);

        NodeTemplate nodeTemplate = TemplateBuilder.buildNodeTemplate(loadedIndexedNodeType);
        nodeTemplate.setName(operation.getNodeName());
        if (operation.getCoords() != null) {
            // Set the position information of the node as meta-data.
            nodeTemplate.setTags(Lists.newArrayList(new Tag(Constants.X_META, String.valueOf(operation.getCoords().getX())),
                    new Tag(Constants.Y_META, String.valueOf(operation.getCoords().getY()))));
        }
        topology.getNodeTemplates().put(operation.getNodeName(), nodeTemplate);

        log.debug("Adding a new Node template <" + operation.getNodeName() + "> bound to the node type <" + operation.getIndexedNodeTypeId()
                + "> to the topology <" + topology.getId() + "> .");

        TopologyContext topologyContext = workflowBuilderService.buildTopologyContext(topology, csar);
        workflowBuilderService.addNode(topologyContext, operation.getNodeName());

        if (!operation.isSkipAutoCompletion()) {
            danglingRequirementService.addDanglingRequirements(topology, topologyContext, nodeTemplate, operation.getRequirementSkipAutoCompletion());
        }

        if (operation.getContext() != null) {
            String locationId = (String) operation.getContext().getExecutionCache().get(FlowExecutionContext.ORIGIN_LOCATION_FOR_MODIFIER);
            if (locationId != null) {
                Map<String, Set<String>> nodesPerLocation = (Map<String, Set<String>>) operation.getContext().getExecutionCache().get(FlowExecutionContext.NODES_PER_LOCATIONS_CACHE_KEY);
                nodesPerLocation.computeIfAbsent(locationId, k -> Sets.newHashSet()).add(operation.getNodeName());
            }
        }
    }
}