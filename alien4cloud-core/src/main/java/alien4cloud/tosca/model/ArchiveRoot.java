package alien4cloud.tosca.model;

import java.util.List;
import java.util.Map;

import lombok.Getter;
import lombok.Setter;
import alien4cloud.model.components.Csar;
import alien4cloud.model.components.IndexedArtifactType;
import alien4cloud.model.components.IndexedCapabilityType;
import alien4cloud.model.components.IndexedNodeType;
import alien4cloud.model.components.IndexedRelationshipType;
import alien4cloud.model.topology.Topology;

import com.google.common.collect.Maps;

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

    private Map<String, IndexedNodeType> nodeTypes = Maps.newHashMap();
    private Map<String, IndexedRelationshipType> relationshipTypes = Maps.newHashMap();
    private Map<String, IndexedCapabilityType> capabilityTypes = Maps.newHashMap();
    private Map<String, IndexedArtifactType> artifactTypes = Maps.newHashMap();

    /**
     * Indicates if this archive contains tosca types (node types, relationships, capabilities, artifacts).
     */
    public boolean hasToscaTypes() {
        return !nodeTypes.isEmpty() || !relationshipTypes.isEmpty() || !capabilityTypes.isEmpty() || !artifactTypes.isEmpty();
    }

    /**
     * Indicates if this archive contains a topology template.
     */
    public boolean hasToscaTopologyTemplate() {
        return topology != null;
    }

}