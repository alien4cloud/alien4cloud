package alien4cloud.tosca.parser.impl.advanced;

import java.util.Collection;

import javax.annotation.Resource;

import lombok.extern.slf4j.Slf4j;

import org.springframework.stereotype.Component;
import org.yaml.snakeyaml.nodes.Node;

import alien4cloud.component.ICSARRepositorySearchService;
import alien4cloud.component.IToscaElementFinder;
import alien4cloud.csar.services.CsarService;
import alien4cloud.model.components.CSARDependency;
import alien4cloud.model.components.IndexedArtifactType;
import alien4cloud.model.components.IndexedCapabilityType;
import alien4cloud.model.components.IndexedNodeType;
import alien4cloud.model.components.IndexedRelationshipType;
import alien4cloud.model.components.IndexedToscaElement;
import alien4cloud.model.topology.NodeTemplate;
import alien4cloud.topology.TopologyServiceCore;
import alien4cloud.tosca.model.ArchiveRoot;
import alien4cloud.tosca.parser.IChecker;
import alien4cloud.tosca.parser.ParsingContextExecution;
import alien4cloud.tosca.parser.ParsingError;
import alien4cloud.tosca.parser.ToscaParsingUtil;
import alien4cloud.tosca.parser.impl.ErrorCode;

@Slf4j
@Component
public class NodeTemplateChecker implements IChecker<NodeTemplate> {

    private static final String KEY = "nodeTemplateChecker";

    @Resource
    private CsarService csarService;

    @Resource
    private ICSARRepositorySearchService searchService;

    @Resource
    private TopologyServiceCore topologyServiceCore;

    @Override
    public String getName() {
        return KEY;
    }

    @Override
    public void check(NodeTemplate instance, ParsingContextExecution context, Node node) {
        String nodeTypeName = instance.getType();
        final ArchiveRoot archiveRoot = (ArchiveRoot) context.getRoot().getWrappedInstance();
        IndexedNodeType indexedNodeType = ToscaParsingUtil.getNodeTypeFromArchiveOrDependencies(nodeTypeName, archiveRoot, searchService);
        if (indexedNodeType == null) {
            // node type can't be found neither in archive or in dependencies
            context.getParsingErrors()
                    .add(new ParsingError(ErrorCode.TYPE_NOT_FOUND, "Derived_from type not found", node.getStartMark(),
                            "The type specified for a node_template is not found neither in the archive or it's dependencies.", node.getEndMark(), nodeTypeName));
            return;
        }
        IToscaElementFinder toscaElementFinder = new IToscaElementFinder() {
            @Override
            public <T extends IndexedToscaElement> T getElementInDependencies(Class<T> elementClass, String elementId, Collection<CSARDependency> dependencies) {
                T result = null;
                // fisrt off all seach in the current archive
                if(elementClass == IndexedCapabilityType.class) {
                    result = (T) archiveRoot.getCapabilityTypes().get(elementId);
                } else if (elementClass == IndexedArtifactType.class) {
                    result = (T) archiveRoot.getArtifactTypes().get(elementId);
                } else if (elementClass == IndexedRelationshipType.class) {
                    result = (T) archiveRoot.getRelationshipTypes().get(elementId);
                } else if (elementClass == IndexedNodeType.class) {
                    result = (T) archiveRoot.getNodeTypes().get(elementId);
                }

                if (result == null) {
                    // the result can't be found in current archive, let's have a look in dependencies
                    result = searchService.getElementInDependencies(elementClass, elementId, dependencies);
                }
                return result;
            }
        };

        NodeTemplate tempObject = topologyServiceCore.buildNodeTemplate(archiveRoot.getArchive().getDependencies(), indexedNodeType, instance,
                toscaElementFinder);
        instance.setAttributes(tempObject.getAttributes());
        instance.setCapabilities(tempObject.getCapabilities());
        instance.setProperties(tempObject.getProperties());
        instance.setRequirements(tempObject.getRequirements());
        instance.setArtifacts(tempObject.getArtifacts());
    }

}
