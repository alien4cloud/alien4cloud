package alien4cloud.model.topology;

import java.util.Map;
import java.util.Set;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import alien4cloud.model.components.AbstractPropertyValue;
import alien4cloud.model.components.DeploymentArtifact;
import alien4cloud.model.components.Interface;
import alien4cloud.utils.jackson.ConditionalAttributes;
import alien4cloud.utils.jackson.ConditionalOnAttribute;
import alien4cloud.utils.jackson.JSonMapEntryArrayDeSerializer;
import alien4cloud.utils.jackson.JSonMapEntryArraySerializer;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;

/**
 * Specifies a kind of a component making up the cloud application.
 *
 * @author luc boutier
 */
@Getter
@Setter
@NoArgsConstructor
public class NodeTemplate extends AbstractTemplate {
    /**
     * Id in the map is name.replaceAll(" ", "").toLowerCase();
     */
    private String name;

    /**
     * Attributes of the node template
     */
    @ConditionalOnAttribute(ConditionalAttributes.REST)
    @JsonDeserialize(using = JSonMapEntryArrayDeSerializer.class)
    @JsonSerialize(using = JSonMapEntryArraySerializer.class)
    private Map<String, String> attributes;

    /**
     * The requirement that this node template defines
     */
    @ConditionalOnAttribute(ConditionalAttributes.REST)
    @JsonDeserialize(using = JSonMapEntryArrayDeSerializer.class)
    @JsonSerialize(using = JSonMapEntryArraySerializer.class)
    private Map<String, Requirement> requirements;

    /**
     * Relationships between node templates
     */
    @ConditionalOnAttribute(ConditionalAttributes.REST)
    @JsonDeserialize(using = JSonMapEntryArrayDeSerializer.class)
    @JsonSerialize(using = JSonMapEntryArraySerializer.class)
    private Map<String, RelationshipTemplate> relationships;

    /**
     * The capabilities that this node template defines
     */
    @ConditionalOnAttribute(ConditionalAttributes.REST)
    @JsonDeserialize(using = JSonMapEntryArrayDeSerializer.class)
    @JsonSerialize(using = JSonMapEntryArraySerializer.class)
    private Map<String, Capability> capabilities;

    @ConditionalOnAttribute(ConditionalAttributes.REST)
    @JsonDeserialize(using = JSonMapEntryArrayDeSerializer.class)
    @JsonSerialize(using = JSonMapEntryArraySerializer.class)
    private Map<String, Interface> interfaces;
    
    /**
     * The {@link NodeGroup}s this template is member of.
     */
    private Set<String> groups;

    public NodeTemplate(String type, Map<String, AbstractPropertyValue> properties, Map<String, String> attributes,
            Map<String, RelationshipTemplate> relationships, Map<String, Requirement> requirements, Map<String, Capability> capabilities,
            Map<String, Interface> interfaces, Map<String, DeploymentArtifact> artifacts) {
        this.setType(type);
        this.setProperties(properties);
        this.setArtifacts(artifacts);
        this.attributes = attributes;
        this.relationships = relationships;
        this.requirements = requirements;
        this.capabilities = capabilities;
        this.interfaces = interfaces;
    }
}