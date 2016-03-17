package alien4cloud.tosca.parser;

import java.util.List;

import org.yaml.snakeyaml.nodes.Node;
import org.yaml.snakeyaml.nodes.ScalarNode;

import alien4cloud.component.ICSARRepositorySearchService;
import alien4cloud.model.components.IndexedArtifactType;
import alien4cloud.model.components.IndexedCapabilityType;
import alien4cloud.model.components.IndexedDataType;
import alien4cloud.model.components.IndexedNodeType;
import alien4cloud.model.components.IndexedRelationshipType;
import alien4cloud.model.components.IndexedToscaElement;
import alien4cloud.tosca.model.ArchiveRoot;
import alien4cloud.tosca.parser.impl.ErrorCode;

public final class ToscaParsingUtil {
    private ToscaParsingUtil() {
    }

    /**
     * Get a string value.
     * 
     * @param keyNode The node that represents the key of the value node to parse.
     * @param valueNode The value node.
     * @param parsingErrors A list of errors in which to add an error if the value is not a valid yaml string.
     * @return The value of the string. In case of an error null is returned. Null return however is not sufficient to know that an error occured. In case of an
     *         error a new {@link ParsingError} is added to the parsingErrors list given as a parameter.
     */
    public static String getStringValue(ScalarNode keyNode, Node valueNode, List<ParsingError> parsingErrors) {
        if (valueNode instanceof ScalarNode) {
            ScalarNode scalarNode = (ScalarNode) valueNode;
            return scalarNode.getValue();
        }
        parsingErrors.add(new ParsingError(ErrorCode.SYNTAX_ERROR, "Error while parsing field " + keyNode.getValue(), keyNode.getStartMark(),
                "Expected a scalar type.", valueNode.getStartMark(), "scalar"));
        return null;
    }

    public static IndexedNodeType getNodeTypeFromArchiveOrDependencies(String nodeTypeName, ArchiveRoot archiveRoot, ICSARRepositorySearchService searchService) {
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

    public static IndexedDataType getDataTypeFromArchiveOrDependencies(String dataTypeName, ArchiveRoot archiveRoot, ICSARRepositorySearchService searchService) {
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
