package alien4cloud.model.topology;

import java.util.Map;

import alien4cloud.model.components.DeploymentArtifact;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Specifies a kind of a component making up the cloud application.
 * 
 * @author luc boutier
 */
@Getter
@Setter
@NoArgsConstructor
@SuppressWarnings("PMD.UnusedPrivateField")
public class NodeTemplate extends AbstractTemplate {
    /**
     * Id in the map is name.replaceAll(" ", "").toLowerCase();
     */
    private String name;

    /**
     * Properties of the node template
     */
    private Map<String, String> properties;

    /**
     * Attributes of the node template
     */
    private Map<String, String> attributes;

    /**
     * Relationships between node templates
     */
    private Map<String, RelationshipTemplate> relationships;

    /**
     * The requirement that this node template defines
     */
    private Map<String, Requirement> requirements;

    /**
     * The capabilities that this node template defines
     */
    private Map<String, Capability> capabilities;

    /**
     * The deployment artifacts
     */
    private Map<String, DeploymentArtifact> artifacts;

    public NodeTemplate(String type, Map<String, String> properties, Map<String, String> attributes, Map<String, RelationshipTemplate> relationships,
            Map<String, Requirement> requirements, Map<String, Capability> capabilities, Map<String, DeploymentArtifact> artifacts) {
        this.setType(type);
        this.properties = properties;
        this.attributes = attributes;
        this.relationships = relationships;
        this.requirements = requirements;
        this.capabilities = capabilities;
        this.artifacts = artifacts;
    }
}