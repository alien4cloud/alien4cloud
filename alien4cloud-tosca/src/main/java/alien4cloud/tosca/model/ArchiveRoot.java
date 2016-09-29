package alien4cloud.tosca.model;

import java.util.List;
import java.util.Map;

import org.apache.commons.collections4.MapUtils;

import com.google.common.collect.Maps;

import org.alien4cloud.tosca.model.Csar;
import org.alien4cloud.tosca.model.types.ArtifactType;
import org.alien4cloud.tosca.model.types.CapabilityType;
import org.alien4cloud.tosca.model.types.DataType;
import org.alien4cloud.tosca.model.types.NodeType;
import org.alien4cloud.tosca.model.types.RelationshipType;
import org.alien4cloud.tosca.model.definitions.RepositoryDefinition;
import org.alien4cloud.tosca.model.templates.Topology;
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

    private Map<String, DataType> dataTypes = Maps.newHashMap();
    private Map<String, ArtifactType> artifactTypes = Maps.newHashMap();
    private Map<String, CapabilityType> capabilityTypes = Maps.newHashMap();
    private Map<String, RelationshipType> relationshipTypes = Maps.newHashMap();
    private Map<String, NodeType> nodeTypes = Maps.newHashMap();

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