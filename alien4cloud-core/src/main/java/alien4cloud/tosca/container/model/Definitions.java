package alien4cloud.tosca.container.model;

import java.util.List;
import java.util.Map;

import javax.validation.Valid;

import lombok.Getter;
import lombok.Setter;
import alien4cloud.tosca.container.model.type.ArtifactType;
import alien4cloud.tosca.container.model.type.CapabilityType;
import alien4cloud.tosca.container.model.type.NodeType;
import alien4cloud.tosca.container.model.type.RelationshipType;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

/**
 * TOSCA Definitions.
 * 
 * @author luc boutier
 */
@Getter
@Setter
@SuppressWarnings("PMD.UnusedPrivateField")
public final class Definitions {
    // meta-data
    private String name;
    private String namespace;
    private String toscaDefinitionsVersion;

    private String description;
    private List<String> imports = Lists.newArrayList();

    // Types
    @Valid
    private Map<String, NodeType> nodeTypes = Maps.newHashMap();
    @Valid
    private Map<String, RelationshipType> relationshipTypes = Maps.newHashMap();
    @Valid
    private Map<String, CapabilityType> capabilityTypes = Maps.newHashMap();
    @Valid
    private Map<String, ArtifactType> artifactTypes = Maps.newHashMap();
}