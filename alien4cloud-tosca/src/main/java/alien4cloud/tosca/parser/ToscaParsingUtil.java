package alien4cloud.tosca.parser;

import java.util.List;

import org.yaml.snakeyaml.nodes.Node;
import org.yaml.snakeyaml.nodes.ScalarNode;

import alien4cloud.component.ICSARRepositorySearchService;
import alien4cloud.model.components.*;
import alien4cloud.tosca.model.ArchiveRoot;
import alien4cloud.tosca.parser.impl.ErrorCode;

@Deprecated
public final class ToscaParsingUtil {

    private ToscaParsingUtil() {
    }

    public static IndexedNodeType getNodeTypeFromArchiveOrDependencies(String nodeTypeName, ArchiveRoot archiveRoot,
            ICSARRepositorySearchService searchService) {
        return getElementFromArchiveOrDependencies(IndexedNodeType.class, nodeTypeName, archiveRoot, searchService);
    }

    public static IndexedCapabilityType getCapabilityTypeFromArchiveOrDependencies(String nodeTypeName, ArchiveRoot archiveRoot,
            ICSARRepositorySearchService searchService) {
        return getElementFromArchiveOrDependencies(IndexedCapabilityType.class, nodeTypeName, archiveRoot, searchService);
    }

    public static IndexedRelationshipType getRelationshipTypeFromArchiveOrDependencies(String nodeTypeName, ArchiveRoot archiveRoot,
            ICSARRepositorySearchService searchService) {
        return getElementFromArchiveOrDependencies(IndexedRelationshipType.class, nodeTypeName, archiveRoot, searchService);
    }

    public static IndexedDataType getDataTypeFromArchiveOrDependencies(String dataTypeName, ArchiveRoot archiveRoot,
            ICSARRepositorySearchService searchService) {
        return getElementFromArchiveOrDependencies(IndexedDataType.class, dataTypeName, archiveRoot, searchService);
    }

    @SuppressWarnings("unchecked")
    public static <T extends IndexedToscaElement> T getElementFromArchiveOrDependencies(Class<T> elementClass, String elementId, ArchiveRoot archiveRoot,
            ICSARRepositorySearchService searchService) {
        T result = null;
        // fisrt off all seach in the current archive
        if (elementClass == IndexedCapabilityType.class) {
            result = (T) archiveRoot.getCapabilityTypes().get(elementId);
        } else if (elementClass == IndexedArtifactType.class) {
            result = (T) archiveRoot.getArtifactTypes().get(elementId);
        } else if (elementClass == IndexedRelationshipType.class) {
            result = (T) archiveRoot.getRelationshipTypes().get(elementId);
        } else if (elementClass == IndexedNodeType.class) {
            result = (T) archiveRoot.getNodeTypes().get(elementId);
        } else if (elementClass == IndexedDataType.class) {
            result = (T) archiveRoot.getDataTypes().get(elementId);
        }

        if (result == null) {
            // the result can't be found in current archive, let's have a look in dependencies
            result = searchService.getElementInDependencies(elementClass, elementId, archiveRoot.getArchive().getDependencies());
        }
        return result;
    }
}
