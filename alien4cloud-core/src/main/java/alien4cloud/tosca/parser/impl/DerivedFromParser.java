package alien4cloud.tosca.parser.impl;

import java.util.List;

import lombok.AllArgsConstructor;

import org.yaml.snakeyaml.nodes.Node;

import alien4cloud.component.model.IndexedArtifactType;
import alien4cloud.component.model.IndexedCapabilityType;
import alien4cloud.component.model.IndexedInheritableToscaElement;
import alien4cloud.component.model.IndexedNodeType;
import alien4cloud.component.model.IndexedRelationshipType;
import alien4cloud.tosca.container.services.csar.ICSARRepositorySearchService;
import alien4cloud.tosca.model.ArchiveRoot;
import alien4cloud.tosca.parser.INodeParser;
import alien4cloud.tosca.parser.ParsingContextExecution;
import alien4cloud.tosca.parser.ParsingError;

import com.google.common.collect.Lists;

/**
 * Parse a type reference value. The referenced type must exists in the local definitions or in the
 */
@AllArgsConstructor
public class DerivedFromParser implements INodeParser<List<String>> {
    private final ICSARRepositorySearchService searchService;
    private final ScalarParser scalarParser;
    private final Class<? extends IndexedInheritableToscaElement> validType;

    @Override
    public boolean isDeffered() {
        return true;
    }

    @Override
    public List<String> parse(Node node, ParsingContextExecution context) {
        String valueAsString = scalarParser.parse(node, context).trim();
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
                    new ParsingError(null, "Derived_from type not found", node.getStartMark(),
                            "The type specified as parent is not found neither in the archive or it's dependencies.", node.getEndMark(), valueAsString));
            return null;
        }
        List<String> derivedFrom = Lists.newArrayList(parent.getDerivedFrom());
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
        }
        return null;
    }

    @SuppressWarnings({ "rawtypes", "unchecked" })
    private IndexedInheritableToscaElement getFromDependencies(ArchiveRoot archiveRoot, String referencedType) {
        return (IndexedInheritableToscaElement) searchService.getElementInDependencies((Class) validType, referencedType, archiveRoot.getArchive()
                .getDependencies());
    }
}