package alien4cloud.tosca.model;

import java.util.List;
import java.util.Map;

import lombok.Getter;
import lombok.Setter;
import alien4cloud.component.model.IndexedArtifactType;
import alien4cloud.component.model.IndexedCapabilityType;
import alien4cloud.component.model.IndexedNodeType;
import alien4cloud.component.model.IndexedRelationshipType;

import com.google.common.collect.Maps;

/** Root object to be de-serialized. */
@Getter
@Setter
public class ArchiveRoot {
    /** Contains meta-data related to the actual archive. */
    private Csar archive = new Csar();

    private List<ArchiveRoot> localImports;

    private Map<String, IndexedNodeType> nodeTypes = Maps.newHashMap();
    private Map<String, IndexedRelationshipType> relationshipTypes = Maps.newHashMap();
    private Map<String, IndexedCapabilityType> capabilityTypes = Maps.newHashMap();
    private Map<String, IndexedArtifactType> artifactTypes = Maps.newHashMap();
}