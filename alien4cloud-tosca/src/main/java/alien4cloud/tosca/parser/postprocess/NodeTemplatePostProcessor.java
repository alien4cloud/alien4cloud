package alien4cloud.tosca.parser.postprocess;

import static alien4cloud.utils.AlienUtils.safe;

import java.util.Objects;

import javax.annotation.Resource;

import org.springframework.stereotype.Component;
import org.yaml.snakeyaml.nodes.Node;

import org.alien4cloud.tosca.model.types.NodeType;
import org.alien4cloud.tosca.model.definitions.Operation;
import org.alien4cloud.tosca.model.templates.NodeTemplate;
import alien4cloud.tosca.context.ToscaContext;
import alien4cloud.tosca.parser.ParsingContextExecution;
import alien4cloud.tosca.parser.ParsingError;
import alien4cloud.tosca.parser.ParsingErrorLevel;
import alien4cloud.tosca.parser.impl.ErrorCode;
import alien4cloud.tosca.topology.NodeTemplateBuilder;

/**
 * Post process a node template
 */
@Component
public class NodeTemplatePostProcessor implements IPostProcessor<NodeTemplate> {
    @Resource
    private ReferencePostProcessor referencePostProcessor;
    @Resource
    private CapabilityPostProcessor capabilityPostProcessor;
    @Resource
    private RequirementPostProcessor requirementPostProcessor;
    @Resource
    private PropertyValueChecker propertyValueChecker;
    @Resource
    private ArtifactPostProcessor artifactPostProcessor;

    @Override
    public void process(final NodeTemplate instance) {
        // ensure type exists
        referencePostProcessor.process(new ReferencePostProcessor.TypeReference(instance.getType(), NodeType.class));
        final NodeType nodeType = ToscaContext.get(NodeType.class, instance.getType());
        if (nodeType == null) {
            return; // error managed by the reference post processor.
        }

        // FIXME we should check that the artifact is defined at the type level.
        safe(instance.getArtifacts()).values().forEach(artifactPostProcessor);
        // TODO Manage interfaces inputs to copy them to all operations.
        safe(instance.getInterfaces()).values().stream().flatMap(anInterface -> safe(anInterface.getOperations()).values().stream())
                .map(Operation::getImplementationArtifact).filter(Objects::nonNull).forEach(artifactPostProcessor);

        // Merge the node template with data coming from the type (default values etc.).
        NodeTemplate tempObject = NodeTemplateBuilder.buildNodeTemplate(nodeType, instance, false);
        safe(instance.getCapabilities()).keySet().forEach(s -> {
            if (!safe(tempObject.getCapabilities()).containsKey(s)) {
                Node node = ParsingContextExecution.getObjectToNodeMap().get(s);
                ParsingContextExecution.getParsingErrors()
                        .add(new ParsingError(ParsingErrorLevel.WARNING, ErrorCode.UNKNOWN_CAPABILITY, null, node.getStartMark(), null, node.getEndMark(), s));
            }
        });
        instance.setAttributes(tempObject.getAttributes());
        instance.setCapabilities(tempObject.getCapabilities());
        instance.setProperties(tempObject.getProperties());
        instance.setRequirements(tempObject.getRequirements());
        instance.setArtifacts(tempObject.getArtifacts());
        instance.setInterfaces(tempObject.getInterfaces());

        // apply post processor to capabilities defined locally on the element (no need to post-processed the one merged)
        safe(instance.getCapabilities()).entrySet().forEach(capabilityPostProcessor);
        safe(instance.getRequirements()).entrySet().forEach(requirementPostProcessor);

        propertyValueChecker.checkProperties(nodeType, instance.getProperties(), instance.getName());
    }
}