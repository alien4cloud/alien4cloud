package alien4cloud.tosca.model;

import java.util.List;
import java.util.Map;

import org.apache.commons.collections4.MapUtils;

import com.google.common.collect.Maps;

import alien4cloud.model.components.Csar;
import alien4cloud.model.components.IndexedArtifactType;
import alien4cloud.model.components.IndexedCapabilityType;
import alien4cloud.model.components.IndexedDataType;
import alien4cloud.model.components.IndexedNodeType;
import alien4cloud.model.components.IndexedRelationshipType;
import alien4cloud.model.components.RepositoryDefinition;
import alien4cloud.model.topology.Topology;
import lombok.Getter;
import lombok.Setter;

/** Root object to be de-serialized. */
@Getter
@Setter
public class ArchiveRoot {
    /** Contains meta-data related to the actual archive. */
    private Csar archive = new Csar();

    /** An archive can embed topology template (TOSCA meaning). */
    private Topology topology;
    /** The description of the topology template is not a property of the topology. */
    private String topologyTemplateDescription;

    private List<ArchiveRoot> localImports;

    private Map<String, RepositoryDefinition> repositories = Maps.newHashMap();

    private Map<String, IndexedDataType> dataTypes = Maps.newHashMap();
    private Map<String, IndexedArtifactType> artifactTypes = Maps.newHashMap();
    private Map<String, IndexedCapabilityType> capabilityTypes = Maps.newHashMap();
    private Map<String, IndexedRelationshipType> relationshipTypes = Maps.newHashMap();
    private Map<String, IndexedNodeType> nodeTypes = Maps.newHashMap();

    /**
     * Indicates if this archive contains tosca types (node types, relationships, capabilities, artifacts).
     */
    public boolean hasToscaTypes() {
        return MapUtils.isNotEmpty(nodeTypes) || MapUtils.isNotEmpty(relationshipTypes) || MapUtils.isNotEmpty(capabilityTypes)
                || MapUtils.isNotEmpty(artifactTypes);
    }

    /**
     * Indicates if this archive contains a topology template.
     */
    public boolean hasToscaTopologyTemplate() {
        return topology != null;
    }

}