package alien4cloud.tosca.parser.impl.advanced;

import java.util.List;
import java.util.Map;

import javax.annotation.Resource;

import org.springframework.stereotype.Component;
import org.yaml.snakeyaml.nodes.Node;

import alien4cloud.component.ICSARRepositorySearchService;
import alien4cloud.model.components.CSARDependency;
import alien4cloud.model.components.IndexedInheritableToscaElement;
import alien4cloud.model.components.IndexedModelUtils;
import alien4cloud.model.topology.Topology;
import alien4cloud.tosca.model.ArchiveRoot;
import alien4cloud.tosca.parser.IChecker;
import alien4cloud.tosca.parser.ParsingContextExecution;
import alien4cloud.tosca.parser.ToscaParsingUtil;

import com.google.common.collect.Maps;

@Component
public class TopologyChecker implements IChecker<Topology> {

    private static final String KEY = "topologyChecker";

    @Resource
    private ICSARRepositorySearchService searchService;

    @Override
    public String getName() {
        return KEY;
    }

    @Override
    public void check(Topology instance, ParsingContextExecution context, Node node) {
        ArchiveRoot archiveRoot = (ArchiveRoot) context.getRoot().getWrappedInstance();
        instance.setDependencies(archiveRoot.getArchive().getDependencies());
        // this topology is in fact dependent on current archive
        CSARDependency selfDependency = new CSARDependency(archiveRoot.getArchive().getName(), archiveRoot.getArchive().getVersion());
        instance.getDependencies().add(selfDependency);
        
        // we need that node types inherited stuffs have to be merged before we start parsing requirements
        mergeHierarchy(archiveRoot.getArtifactTypes(), archiveRoot);
        mergeHierarchy(archiveRoot.getCapabilityTypes(), archiveRoot);
        mergeHierarchy(archiveRoot.getNodeTypes(), archiveRoot);
        mergeHierarchy(archiveRoot.getRelationshipTypes(), archiveRoot);
    }

    private <T extends IndexedInheritableToscaElement> void mergeHierarchy(Map<String, T> indexedElements, ArchiveRoot archiveRoot) {
        if (indexedElements == null) {
            return;
        }
        for (T element : indexedElements.values()) {
            mergeHierarchy(element, archiveRoot);
        }
    }

    private <T extends IndexedInheritableToscaElement> void mergeHierarchy(T indexedElement, ArchiveRoot archiveRoot) {
        List<String> derivedFrom = indexedElement.getDerivedFrom();
        if (derivedFrom == null) {
            return;
        }
        Map<String, T> hierarchy = Maps.newHashMap();
        for (String parentId : derivedFrom) {
            T parentElement = (T) ToscaParsingUtil.getElementFromArchiveOrDependencies(indexedElement.getClass(), parentId, archiveRoot, searchService);
            hierarchy.put(parentElement.getId(), parentElement);
        }
        List<T> hierarchyList = IndexedModelUtils.orderByDerivedFromHierarchy(hierarchy);
        hierarchyList.add(indexedElement);
        for (int i = 0; i < hierarchyList.size() - 1; i++) {
            T from = hierarchyList.get(i);
            T to = hierarchyList.get(i + 1);
            if (to.getArchiveName().equals(archiveRoot.getArchive().getName()) && to.getArchiveVersion().equals(archiveRoot.getArchive().getVersion())) {
                // we only merge element that come with current archive (the one we are parsing).
                // by this way, we don't remerge existing elements
                IndexedModelUtils.mergeInheritableIndex(from, to);
            }
        }
    }

}
