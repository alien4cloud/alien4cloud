package alien4cloud.service;

import static alien4cloud.utils.AlienUtils.safe;

import javax.inject.Inject;

import alien4cloud.tosca.context.ToscaContext;
import org.alien4cloud.tosca.catalog.index.IToscaTypeSearchService;
import org.alien4cloud.tosca.model.CSARDependency;
import org.alien4cloud.tosca.model.instances.NodeInstance;
import org.alien4cloud.tosca.model.templates.NodeTemplate;
import org.alien4cloud.tosca.model.types.NodeType;
import org.springframework.stereotype.Service;

import alien4cloud.exception.NotFoundException;
import alien4cloud.paas.plan.ToscaNodeLifecycleConstants;
import alien4cloud.tosca.context.ToscaContextual;
import alien4cloud.tosca.parser.postprocess.NodeTemplatePostProcessor;
import alien4cloud.tosca.topology.NodeTemplateBuilder;

/**
 * Simple service to manage node instance validations.
 */
@Service
public class NodeInstanceService {
    @Inject
    private IToscaTypeSearchService toscaTypeSearchService;
    @Inject
    private NodeTemplatePostProcessor nodeTemplatePostProcessor;

    /**
     * Create a new instance of a given node type based on default generated template.
     * 
     * @param typeName The node instance type's name (elementId).
     * @param typeVersion The node instance type's version.
     * @return An instance that matches the given type created from a default template (default values). Note that the node instance may be constructed from an
     *         invalid template (missing required properties) without errors. State of the node is set to initial.
     */
    @ToscaContextual
    public NodeInstance create(String typeName, String typeVersion) {
        NodeType nodeType = toscaTypeSearchService.find(NodeType.class, typeName, typeVersion);
        ToscaContext.get().addDependency(new CSARDependency(nodeType.getArchiveName(), nodeType.getArchiveVersion()));
        if (nodeType == null) {
            throw new NotFoundException(String.format("Node type [%s] doesn't exists with version [%s].", typeName, typeVersion));
        }
        NodeTemplate nodeTemplate = NodeTemplateBuilder.buildNodeTemplate(nodeType, null);
        NodeInstance instance = new NodeInstance();
        instance.setAttribute(ToscaNodeLifecycleConstants.ATT_STATE, ToscaNodeLifecycleConstants.INITIAL);
        instance.setNodeTemplate(nodeTemplate);
        instance.setTypeVersion(typeVersion);
        return instance;
    }

    /**
     * Performs validation of the node instance. Note that based on the actual node state the validation is more or less strict.
     *
     * When the node state is initial the validation checks that all elements defined in the node template or node instance attributes matches the definition of
     * the node
     * type. It however does not check if the type has all required properties configured.
     *
     * When the node state is anything else the validation performs above validation and also checks that all required properties are defined.
     *
     * @param nodeInstance
     */
    @ToscaContextual
    public void validate(NodeInstance nodeInstance) {
        // FIXME this requires a parsing context actually.
        nodeTemplatePostProcessor.process(nodeInstance.getNodeTemplate());

        if (!ToscaNodeLifecycleConstants.INITIAL.equals(safe(nodeInstance.getAttributeValues()).get(ToscaNodeLifecycleConstants.ATT_STATE))) {
            // FIXME check that all required properties are defined.
        }
    }
}
