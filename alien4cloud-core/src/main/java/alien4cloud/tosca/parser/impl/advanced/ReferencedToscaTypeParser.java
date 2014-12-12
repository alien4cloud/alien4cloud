package alien4cloud.tosca.parser.impl.advanced;

import javax.annotation.Resource;

import org.apache.commons.lang3.ArrayUtils;
import org.yaml.snakeyaml.nodes.Node;

import alien4cloud.component.model.*;
import alien4cloud.tosca.container.services.csar.ICSARRepositorySearchService;
import alien4cloud.tosca.model.ArchiveRoot;
import alien4cloud.tosca.parser.INodeParser;
import alien4cloud.tosca.parser.ParsingContextExecution;
import alien4cloud.tosca.parser.ParsingError;
import alien4cloud.tosca.parser.impl.ErrorCode;
import alien4cloud.tosca.parser.impl.base.ScalarParser;

/**
 * Parse a type reference value. The referenced type must exists in the local definitions or in the dependencies.
 */
public abstract class ReferencedToscaTypeParser implements INodeParser<String> {
    @SuppressWarnings("unchecked")
    private static final Class<? extends IndexedInheritableToscaElement>[] possibleTypes = new Class[] { IndexedNodeType.class, IndexedRelationshipType.class,
            IndexedCapabilityType.class, IndexedArtifactType.class };

    @Resource
    private ICSARRepositorySearchService searchService;
    @Resource
    private ScalarParser scalarParser;
    private final Class<? extends IndexedInheritableToscaElement>[] validTypes;

    public ReferencedToscaTypeParser(Class<? extends IndexedInheritableToscaElement>... validTypes) {
        this.validTypes = validTypes;
    }

    @Override
    public boolean isDeferred(ParsingContextExecution context) {
        return true;
    }

    @Override
    public String parse(Node node, ParsingContextExecution context) {
        String valueAsString = scalarParser.parse(node, context);
        if (valueAsString == null || valueAsString.isEmpty()) {
            return null;
        }
        ArchiveRoot archiveRoot = (ArchiveRoot) context.getRoot().getWrappedInstance();
        IndexedInheritableToscaElement referencedElement = getFromArchive(archiveRoot, valueAsString);
        if (referencedElement == null) {
            referencedElement = getFromDependencies(archiveRoot, valueAsString);
        }
        if (referencedElement == null) {
            context.getParsingErrors().add(
                    new ParsingError(ErrorCode.TYPE_NOT_FOUND, "Type not found", node.getStartMark(),
                            "The referenced type is not found neither in the archive or it's dependencies.", node.getEndMark(), valueAsString));
            return null;
        }
        return valueAsString;
    }

    private IndexedInheritableToscaElement getFromArchive(ArchiveRoot archiveRoot, String referencedType) {
        IndexedInheritableToscaElement referencedElement = null;
        if (ArrayUtils.contains(validTypes, IndexedNodeType.class) && archiveRoot.getNodeTypes() != null) {
            referencedElement = archiveRoot.getNodeTypes().get(referencedType);
        }
        if (referencedElement == null && ArrayUtils.contains(validTypes, IndexedRelationshipType.class) && archiveRoot.getRelationshipTypes() != null) {
            referencedElement = archiveRoot.getRelationshipTypes().get(referencedType);
        }
        if (referencedElement == null && ArrayUtils.contains(validTypes, IndexedCapabilityType.class) && archiveRoot.getCapabilityTypes() != null) {
            referencedElement = archiveRoot.getCapabilityTypes().get(referencedType);
        }
        if (referencedElement == null && ArrayUtils.contains(validTypes, IndexedArtifactType.class) && archiveRoot.getArtifactTypes() != null) {
            referencedElement = archiveRoot.getArtifactTypes().get(referencedType);
        }
        return referencedElement;
    }

    private IndexedInheritableToscaElement getFromDependencies(ArchiveRoot archiveRoot, String referencedType) {
        IndexedInheritableToscaElement referencedElement = null;
        for (Class<? extends IndexedInheritableToscaElement> clazz : possibleTypes) {
            if (referencedElement == null && ArrayUtils.contains(validTypes, clazz)) {
                referencedElement = getFromDependencies(archiveRoot, referencedType, clazz);
            }
        }
        return referencedElement;
    }

    @SuppressWarnings({ "rawtypes", "unchecked" })
    private IndexedInheritableToscaElement getFromDependencies(ArchiveRoot archiveRoot, String referencedType,
            Class<? extends IndexedInheritableToscaElement> validType) {
        return (IndexedInheritableToscaElement) searchService.getElementInDependencies((Class) validType, referencedType, archiveRoot.getArchive()
                .getDependencies());
    }
}