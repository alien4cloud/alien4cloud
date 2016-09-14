package alien4cloud.tosca.parser;

import org.alien4cloud.tosca.model.types.*;

import alien4cloud.component.ICSARRepositorySearchService;
import alien4cloud.tosca.model.ArchiveRoot;

@Deprecated
public final class ToscaParsingUtil {

    private ToscaParsingUtil() {
    }

    public static NodeType getNodeTypeFromArchiveOrDependencies(String nodeTypeName, ArchiveRoot archiveRoot,
                                                                ICSARRepositorySearchService searchService) {
        return getElementFromArchiveOrDependencies(NodeType.class, nodeTypeName, archiveRoot, searchService);
    }

    public static CapabilityType getCapabilityTypeFromArchiveOrDependencies(String nodeTypeName, ArchiveRoot archiveRoot,
                                                                            ICSARRepositorySearchService searchService) {
        return getElementFromArchiveOrDependencies(CapabilityType.class, nodeTypeName, archiveRoot, searchService);
    }

    public static RelationshipType getRelationshipTypeFromArchiveOrDependencies(String nodeTypeName, ArchiveRoot archiveRoot,
                                                                                ICSARRepositorySearchService searchService) {
        return getElementFromArchiveOrDependencies(RelationshipType.class, nodeTypeName, archiveRoot, searchService);
    }

    public static DataType getDataTypeFromArchiveOrDependencies(String dataTypeName, ArchiveRoot archiveRoot,
                                                                ICSARRepositorySearchService searchService) {
        return getElementFromArchiveOrDependencies(DataType.class, dataTypeName, archiveRoot, searchService);
    }

    @SuppressWarnings("unchecked")
    public static <T extends AbstractToscaType> T getElementFromArchiveOrDependencies(Class<T> elementClass, String elementId, ArchiveRoot archiveRoot,
                                                                                      ICSARRepositorySearchService searchService) {
        T result = null;
        // fisrt off all seach in the current archive
        if (elementClass == CapabilityType.class) {
            result = (T) archiveRoot.getCapabilityTypes().get(elementId);
        } else if (elementClass == ArtifactType.class) {
            result = (T) archiveRoot.getArtifactTypes().get(elementId);
        } else if (elementClass == RelationshipType.class) {
            result = (T) archiveRoot.getRelationshipTypes().get(elementId);
        } else if (elementClass == NodeType.class) {
            result = (T) archiveRoot.getNodeTypes().get(elementId);
        } else if (elementClass == DataType.class) {
            result = (T) archiveRoot.getDataTypes().get(elementId);
        }

        if (result == null) {
            // the result can't be found in current archive, let's have a look in dependencies
            result = searchService.getElementInDependencies(elementClass, elementId, archiveRoot.getArchive().getDependencies());
        }
        return result;
    }
}
