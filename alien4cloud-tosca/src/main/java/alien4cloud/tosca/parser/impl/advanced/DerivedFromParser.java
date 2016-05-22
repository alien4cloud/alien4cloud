package alien4cloud.tosca.parser.impl.advanced;

import java.util.List;

import javax.annotation.Resource;

import org.yaml.snakeyaml.nodes.Node;

import alien4cloud.component.ICSARRepositorySearchService;
import alien4cloud.model.components.IndexedArtifactType;
import alien4cloud.model.components.IndexedCapabilityType;
import alien4cloud.model.components.IndexedDataType;
import alien4cloud.model.components.IndexedInheritableToscaElement;
import alien4cloud.model.components.IndexedNodeType;
import alien4cloud.model.components.IndexedRelationshipType;
import alien4cloud.tosca.model.ArchiveRoot;
import alien4cloud.tosca.parser.ParsingContextExecution;
import alien4cloud.tosca.parser.ParsingError;
import alien4cloud.tosca.parser.impl.ErrorCode;
import alien4cloud.tosca.parser.impl.base.ScalarParser;
import alien4cloud.tosca.parser.mapping.DefaultDeferredParser;

import com.google.common.collect.Lists;

/**
 * Parse a type reference value. The referenced type must exists in the local definitions or in the alien repository.
 */
public abstract class DerivedFromParser extends DefaultDeferredParser<List<String>> {
    @Resource
    private ICSARRepositorySearchService searchService;
    @Resource
    private ScalarParser scalarParser;

    private final Class<? extends IndexedInheritableToscaElement> validType;

    public DerivedFromParser(Class<? extends IndexedInheritableToscaElement> validType) {
        this.validType = validType;
    }

    @Override
    public List<String> parse(Node node, ParsingContextExecution context) {
        String valueAsString = scalarParser.parse(node, context);
        if (valueAsString == null || valueAsString.isEmpty()) {
            return null;
        }
        ArchiveRoot archiveRoot = (ArchiveRoot) context.getRoot().getWrappedInstance();
        IndexedInheritableToscaElement parent = getFromArchive(archiveRoot, valueAsString);
        if (parent == null) {
            parent = getFromDependencies(archiveRoot, valueAsString);
        }
        if (parent == null) {
            context.getParsingErrors().add(
                    new ParsingError(ErrorCode.TYPE_NOT_FOUND, "Derived_from type not found", node.getStartMark(),
                            "The type specified as parent is not found neither in the archive or its dependencies.", node.getEndMark(), valueAsString));
            return null;
        }
        List<String> derivedFrom;
        if (parent.getDerivedFrom() == null) {
            derivedFrom = Lists.newArrayList();
        } else {
            derivedFrom = Lists.newArrayList(parent.getDerivedFrom());
        }
        derivedFrom.add(0, valueAsString);
        return derivedFrom;
    }

    private IndexedInheritableToscaElement getFromArchive(ArchiveRoot archiveRoot, String referencedType) {
        if (validType.equals(IndexedNodeType.class)) {
            return archiveRoot.getNodeTypes().get(referencedType);
        } else if (validType.equals(IndexedRelationshipType.class)) {
            return archiveRoot.getRelationshipTypes().get(referencedType);
        } else if (validType.equals(IndexedCapabilityType.class)) {
            return archiveRoot.getCapabilityTypes().get(referencedType);
        } else if (validType.equals(IndexedArtifactType.class)) {
            return archiveRoot.getArtifactTypes().get(referencedType);
        } else if (validType.equals(IndexedDataType.class)) {
            return archiveRoot.getDataTypes().get(referencedType);
        }
        return null;
    }

    @SuppressWarnings({ "rawtypes", "unchecked" })
    private IndexedInheritableToscaElement getFromDependencies(ArchiveRoot archiveRoot, String referencedType) {
        return (IndexedInheritableToscaElement) searchService.getElementInDependencies((Class) validType, referencedType, archiveRoot.getArchive()
                .getDependencies());
    }
}