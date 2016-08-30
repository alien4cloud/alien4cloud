package alien4cloud.tosca.parser.postprocess;

import static alien4cloud.utils.AlienUtils.safe;

import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import javax.annotation.Resource;

import alien4cloud.model.components.CapabilityDefinition;
import alien4cloud.tosca.parser.ParsingError;
import alien4cloud.tosca.parser.ParsingErrorLevel;
import alien4cloud.tosca.parser.impl.ErrorCode;
import org.apache.commons.collections4.MapUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

import alien4cloud.model.components.DeploymentArtifact;
import alien4cloud.model.components.IndexedNodeType;
import alien4cloud.model.components.Interface;
import alien4cloud.model.topology.NodeTemplate;
import alien4cloud.tosca.context.ToscaContext;
import alien4cloud.tosca.model.ArchiveRoot;
import alien4cloud.tosca.parser.ParsingContextExecution;
import alien4cloud.tosca.topology.NodeTemplateBuilder;
import org.yaml.snakeyaml.nodes.Node;

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
        referencePostProcessor.process(new ReferencePostProcessor.TypeReference(instance.getType(), IndexedNodeType.class));
        final IndexedNodeType nodeType = ToscaContext.get(IndexedNodeType.class, instance.getType());
        if (nodeType == null) {
            return; // error managed by the reference post processor.
        }

        // FIXME we should check that the artifact is defined at the type level.
        safe(instance.getArtifacts()).values().stream().forEach(artifactPostProcessor);
        // TODO Manage interfaces inputs to copy them to all operations.
        for (Interface anInterface : safe(instance.getInterfaces()).values()) {
            safe(anInterface.getOperations()).values().stream().map(operation -> operation.getImplementationArtifact()).filter(Objects::nonNull)
                    .forEach(artifactPostProcessor);
        }

        // TODO check logic below out of of NodeTemplateChecker
        // check which overidded deployment artifact exists in the node type
        // fill in the artifact type if needed
        // add archive name and version to overridden deployment artifacts
        checkDeploymentArtifacts(nodeType, instance);

        // Merge the node template with data coming from the type (default values etc.).
        NodeTemplate tempObject = NodeTemplateBuilder.buildNodeTemplate(nodeType, instance);
        safe(instance.getCapabilities()).keySet().stream().forEach(s -> {
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
        safe(instance.getCapabilities()).entrySet().stream().forEach(capabilityPostProcessor);
        safe(instance.getRequirements()).entrySet().stream().forEach(requirementPostProcessor);

        propertyValueChecker.checkProperties(nodeType, instance.getProperties(), instance.getName());
    }

    private void checkDeploymentArtifacts(IndexedNodeType indexedNodeType, NodeTemplate instance) {
        if (MapUtils.isEmpty(instance.getArtifacts())) {
            return;
        } else if (MapUtils.isEmpty(indexedNodeType.getArtifacts())) {
            instance.setArtifacts(null);
            return;
        }

        ArchiveRoot archiveRoot = (ArchiveRoot) ParsingContextExecution.getRoot().getWrappedInstance();
        Iterator<Map.Entry<String, DeploymentArtifact>> it = instance.getArtifacts().entrySet().iterator();
        Map<String, DeploymentArtifact> nodeTypeArtifacts = indexedNodeType.getArtifacts();
        while (it.hasNext()) {
            Map.Entry<String, DeploymentArtifact> artifactEntry = it.next();
            String name = artifactEntry.getKey();
            // check which overidded deployment artifact exists in the node type
            // TODO: add warning in the context ??
            if (!nodeTypeArtifacts.containsKey(name)) {
                it.remove();
                continue;
            }

            // fill in the artifact type if needed
            DeploymentArtifact artifact = artifactEntry.getValue();
            DeploymentArtifact nodeTypeArtifact = nodeTypeArtifacts.get(name);
            if (StringUtils.isBlank(artifact.getArtifactType()) && nodeTypeArtifact != null) {
                artifact.setArtifactType(nodeTypeArtifact.getArtifactType());
            }
            // add archive name and version to overridden deployment artifacts
            artifactEntry.getValue().setArchiveName(archiveRoot.getArchive().getName());
            artifactEntry.getValue().setArchiveVersion(archiveRoot.getArchive().getVersion());
        }
    }
}