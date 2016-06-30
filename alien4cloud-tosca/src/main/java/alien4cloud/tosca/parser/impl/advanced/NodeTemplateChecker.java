package alien4cloud.tosca.parser.impl.advanced;

import alien4cloud.component.ICSARRepositorySearchService;
import alien4cloud.model.components.DeploymentArtifact;
import alien4cloud.model.components.IndexedNodeType;
import alien4cloud.model.topology.NodeTemplate;
import alien4cloud.tosca.model.ArchiveRoot;
import alien4cloud.tosca.parser.IChecker;
import alien4cloud.tosca.parser.ParsingContextExecution;
import alien4cloud.tosca.parser.ParsingError;
import alien4cloud.tosca.parser.ToscaParsingUtil;
import alien4cloud.tosca.parser.impl.ErrorCode;
import alien4cloud.tosca.topology.NodeTemplateBuilder;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import javax.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.MapUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;
import org.yaml.snakeyaml.nodes.Node;

@Slf4j
@Component
public class NodeTemplateChecker implements IChecker<NodeTemplate> {

    private static final String KEY = "nodeTemplateChecker";

    @Resource
    private ICSARRepositorySearchService searchService;

    @Override
    public String getName() {
        return KEY;
    }

    @Override
    public void before(ParsingContextExecution context, Node node) {
    }

    @Override
    public void check(NodeTemplate instance, ParsingContextExecution context, Node node) {
        String nodeTypeName = instance.getType();
        final ArchiveRoot archiveRoot = (ArchiveRoot) context.getRoot().getWrappedInstance();
        IndexedNodeType indexedNodeType = ToscaParsingUtil.getNodeTypeFromArchiveOrDependencies(nodeTypeName, archiveRoot, searchService);
        if (indexedNodeType == null) {
            // node type can't be found neither in archive or in dependencies
            context.getParsingErrors().add(new ParsingError(ErrorCode.TYPE_NOT_FOUND, "Derived_from type not found", node.getStartMark(),
                    "The type specified for a node_template is not found neither in the archive nor its dependencies.", node.getEndMark(), nodeTypeName));
            return;
        }

        // check which overidded deployment artifact exists in the node type
        // fill in the artifact type if needed
        // add archive name and version to overridden deployment artifacts
        checkDeploymentArtifacts(context, indexedNodeType, instance);

        NodeTemplate tempObject = NodeTemplateBuilder.buildNodeTemplate(indexedNodeType, instance);

        instance.setAttributes(tempObject.getAttributes());
        instance.setCapabilities(tempObject.getCapabilities());
        instance.setProperties(tempObject.getProperties());
        instance.setRequirements(tempObject.getRequirements());
        instance.setArtifacts(tempObject.getArtifacts());
        instance.setInterfaces(tempObject.getInterfaces());
    }

    private void checkDeploymentArtifacts(ParsingContextExecution context, IndexedNodeType indexedNodeType, NodeTemplate instance) {
        if (MapUtils.isEmpty(instance.getArtifacts())) {
            return;
        } else if (MapUtils.isEmpty(indexedNodeType.getArtifacts())) {
            instance.setArtifacts(null);
            return;
        }

        ArchiveRoot archiveRoot = (ArchiveRoot) context.getRoot().getWrappedInstance();
        Iterator<Entry<String, DeploymentArtifact>> it = instance.getArtifacts().entrySet().iterator();
        Map<String, DeploymentArtifact> nodeTypeArtifacts = indexedNodeType.getArtifacts();
        while (it.hasNext()) {
            Entry<String, DeploymentArtifact> artifactEntry = it.next();
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