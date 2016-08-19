package alien4cloud.model.topology;

import java.util.Date;
import java.util.Map;
import java.util.Set;

import org.elasticsearch.annotation.*;
import org.elasticsearch.annotation.query.TermFilter;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.google.common.collect.Sets;

import alien4cloud.model.components.CSARDependency;
import alien4cloud.model.components.DeploymentArtifact;
import alien4cloud.model.components.PropertyDefinition;
import alien4cloud.paas.wf.Workflow;
import alien4cloud.security.IManagedSecuredResource;
import alien4cloud.utils.jackson.ConditionalAttributes;
import alien4cloud.utils.jackson.ConditionalOnAttribute;
import alien4cloud.utils.jackson.JSonMapEntryArrayDeSerializer;
import alien4cloud.utils.jackson.JSonMapEntryArraySerializer;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@ESObject
@NoArgsConstructor
@AllArgsConstructor(suppressConstructorProperties = true)
@JsonInclude(Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public class Topology implements IManagedSecuredResource {
    @Id
    private String id;

    /** Id of the application or topology template. */
    private String delegateId;
    /** Type of the delegate (application or topology template) */
    private String delegateType;
    /** Last update date of the topology to verify if the topology has been changed **/
    private Date lastUpdateDate = new Date();

    /** Path of the yaml file in the archive (relative to the root). */
    private String yamlFilePath;

    /** The list of dependencies of this topology. */
    @TermFilter(paths = { "name", "version" })
    @NestedObject(nestedClass = CSARDependency.class)
    private Set<CSARDependency> dependencies = Sets.newHashSet();

    @MapKeyValue
    @TermFilter(paths = "value.type")
    @ConditionalOnAttribute(ConditionalAttributes.ES)
    @JsonDeserialize(using = JSonMapEntryArrayDeSerializer.class)
    @JsonSerialize(using = JSonMapEntryArraySerializer.class)
    private Map<String, NodeTemplate> nodeTemplates;

    @ObjectField(enabled = false)
    private Map<String, PropertyDefinition> inputs;

    /**
     * Outputs coming from node properties:
     * <ul>
     * <li>key is the node template name.
     * <li>value is a list of node template property names.
     * </ul>
     */
    @ObjectField(enabled = false)
    private Map<String, Set<String>> outputProperties;

    /**
     * Outputs coming from node template capability properties:
     * <ul>
     * <li>key is the node template name.
     * <li>key is the capability name.
     * <li>value is a list of output property names.
     * </ul>
     */
    @ObjectField(enabled = false)
    private Map<String, Map<String, Set<String>>> outputCapabilityProperties;

    /**
     * Outputs coming from node attributes:
     * <ul>
     * <li>key is the node template name.
     * <li>value is a list of node template attribute names.
     * </ul>
     */
    @ObjectField(enabled = false)
    private Map<String, Set<String>> outputAttributes;

    /**
     * These artifacts will be given at deployment time and can be shared by several nodes.
     */
    private Map<String, DeploymentArtifact> inputArtifacts;

    private Map<String, NodeGroup> groups;

    /**
     * When not null, describe how this topology can be used to substitute a node type in another topology (topology composition).
     */
    @ObjectField(enabled = false)
    private SubstitutionMapping substitutionMapping;

    /**
     * All the workflows associated with this topology.
     */
    @ObjectField(enabled = false)
    private Map<String, Workflow> workflows;

    /**
     * /**
     * Return true if the topology is an empty topology (won't be saved on import).
     *
     * @return True if the topology is empty (doesn't contains any node).
     */
    public boolean isEmpty() {
        return nodeTemplates == null || nodeTemplates.isEmpty();
    }
}