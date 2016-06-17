package alien4cloud.utils.services;

import java.util.Set;

import javax.annotation.Resource;

import org.springframework.stereotype.Service;

import alien4cloud.component.ICSARRepositorySearchService;
import alien4cloud.model.components.CSARDependency;
import alien4cloud.model.components.IndexedCapabilityType;
import alien4cloud.model.components.IndexedDataType;
import alien4cloud.model.components.IndexedNodeType;
import alien4cloud.model.components.IndexedRelationshipType;
import alien4cloud.model.components.IndexedToscaElement;
import alien4cloud.model.topology.Topology;
import alien4cloud.tosca.model.ArchiveRoot;
import alien4cloud.tosca.parser.ToscaParsingUtil;

@Service
public class DependencyService {

    @Resource
    private ICSARRepositorySearchService searchService;

    public IndexedNodeType getNodeType(String nodeTypeName, DependencyContext dependencyContext) {
        return getElement(IndexedNodeType.class, nodeTypeName, dependencyContext);
    }

    public IndexedCapabilityType getCapabilityType(String nodeTypeName, DependencyContext dependencyContext) {
        return getElement(IndexedCapabilityType.class, nodeTypeName, dependencyContext);
    }

    public IndexedRelationshipType getRelationshipType(String relationshipTypeName, DependencyContext dependencyContext) {
        return getElement(IndexedRelationshipType.class, relationshipTypeName, dependencyContext);
    }

    public IndexedDataType getDataType(String dataTypeName, DependencyContext dependencyContext) {
        return getElement(IndexedDataType.class, dataTypeName, dependencyContext);
    }

    public <T extends IndexedToscaElement> T getElement(Class<T> elementClass, String elementId, DependencyContext dependencyContext) {
        if (dependencyContext instanceof ArchiveDependencyContext) {
            return ToscaParsingUtil.getElementFromArchiveOrDependencies(elementClass, elementId, ((ArchiveDependencyContext) dependencyContext).archiveRoot,
                    searchService);
        } else if (dependencyContext instanceof TopologyDependencyContext) {
            return searchService.getElementInDependencies(elementClass, elementId, ((TopologyDependencyContext) dependencyContext).topology.getDependencies());
        } else if (dependencyContext instanceof DependenciesDependencyContext) {
            return searchService.getElementInDependencies(elementClass, elementId, ((DependenciesDependencyContext) dependencyContext).dependencies);
        } else {
            throw new IllegalArgumentException("Unknwown DependencyContext type: " + dependencyContext.getClass().getName());
        }
    }

    public static interface DependencyContext {
    }

    public static class TopologyDependencyContext implements DependencyContext {
        private Topology topology;

        public TopologyDependencyContext(Topology topology) {
            super();
            this.topology = topology;
        }
    }

    public static class ArchiveDependencyContext implements DependencyContext {
        private ArchiveRoot archiveRoot;

        public ArchiveDependencyContext(ArchiveRoot archiveRoot) {
            super();
            this.archiveRoot = archiveRoot;
        }
    }

    public static class DependenciesDependencyContext implements DependencyContext {
        private Set<CSARDependency> dependencies;

        public DependenciesDependencyContext(Set<CSARDependency> dependencies) {
            super();
            this.dependencies = dependencies;
        }
    }

}
