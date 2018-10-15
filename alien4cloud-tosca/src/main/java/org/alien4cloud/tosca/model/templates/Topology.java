package org.alien4cloud.tosca.model.templates;

import static alien4cloud.dao.model.FetchContext.SUMMARY;

import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import alien4cloud.model.common.IMetaProperties;
import org.alien4cloud.tosca.model.CSARDependency;
import org.alien4cloud.tosca.model.Csar;
import org.alien4cloud.tosca.model.definitions.DeploymentArtifact;
import org.alien4cloud.tosca.model.definitions.PropertyDefinition;
import org.alien4cloud.tosca.model.workflow.Workflow;
import org.elasticsearch.annotation.ESObject;
import org.elasticsearch.annotation.Id;
import org.elasticsearch.annotation.MapKeyValue;
import org.elasticsearch.annotation.NestedObject;
import org.elasticsearch.annotation.ObjectField;
import org.elasticsearch.annotation.StringField;
import org.elasticsearch.annotation.query.FetchContext;
import org.elasticsearch.annotation.query.TermFilter;
import org.elasticsearch.mapping.IndexType;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.google.common.collect.Sets;

import alien4cloud.json.deserializer.NodeTemplateDeserializer;
import alien4cloud.model.common.IDatableResource;
import alien4cloud.model.common.IWorkspaceResource;
import alien4cloud.model.common.Tag;
import alien4cloud.utils.jackson.ConditionalAttributes;
import alien4cloud.utils.jackson.ConditionalOnAttribute;
import alien4cloud.utils.jackson.JSonMapEntryArrayDeSerializer;
import alien4cloud.utils.jackson.JSonMapEntryArraySerializer;
import alien4cloud.utils.version.Version;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@ESObject
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public class Topology implements IDatableResource, IWorkspaceResource, IMetaProperties {
    @StringField(indexType = IndexType.not_analyzed)
    @TermFilter
    private String archiveName;

    @StringField(indexType = IndexType.not_analyzed)
    @TermFilter
    private String archiveVersion;

    @ObjectField
    private Version nestedVersion;

    @TermFilter
    @StringField(indexType = IndexType.not_analyzed)
    private String workspace;

    @StringField(indexType = IndexType.no)
    private String description;

    private Date creationDate;

    private Date lastUpdateDate = new Date();

    /** The list of dependencies of this topology. */
    @NestedObject(nestedClass = CSARDependency.class)
    @FetchContext(contexts = { SUMMARY }, include = { false })
    private Set<CSARDependency> dependencies = Sets.newHashSet();

    @MapKeyValue
    @ConditionalOnAttribute(ConditionalAttributes.ES)
    @JsonDeserialize(using = JSonMapEntryArrayDeSerializer.class, contentUsing = NodeTemplateDeserializer.class)
    @JsonSerialize(using = JSonMapEntryArraySerializer.class)
    @FetchContext(contexts = { SUMMARY }, include = { false })
    private Map<String, NodeTemplate> nodeTemplates;

    @MapKeyValue
    @ConditionalOnAttribute(ConditionalAttributes.ES)
    @JsonDeserialize(using = JSonMapEntryArrayDeSerializer.class)
    @JsonSerialize(using = JSonMapEntryArraySerializer.class)
    @FetchContext(contexts = { SUMMARY }, include = { false })
    private Map<String, PolicyTemplate> policies;

    @ObjectField(enabled = false)
    @FetchContext(contexts = { SUMMARY }, include = { false })
    private Map<String, PropertyDefinition> inputs;

    /**
     * Outputs coming from node properties:
     * <ul>
     * <li>key is the node template name.</li>
     * <li>value is a list of node template property names.</li>
     * </ul>
     */
    @ObjectField(enabled = false)
    @FetchContext(contexts = { SUMMARY }, include = { false })
    private Map<String, Set<String>> outputProperties;

    /**
     * Outputs coming from node template capability properties:
     * <ul>
     * <li>key is the node template name.</li>
     * <li>key is the capability name.</li>
     * <li>value is a list of output property names.</li>
     * </ul>
     */
    @ObjectField(enabled = false)
    @FetchContext(contexts = { SUMMARY }, include = { false })
    private Map<String, Map<String, Set<String>>> outputCapabilityProperties;

    /**
     * Outputs coming from node attributes:
     * <ul>
     * <li>key is the node template name.</li>
     * <li>value is a list of node template attribute names.</li>
     * </ul>
     */
    @ObjectField(enabled = false)
    @FetchContext(contexts = { SUMMARY }, include = { false })
    private Map<String, Set<String>> outputAttributes;

    /**
     * These artifacts will be given at deployment time and can be shared by several nodes.
     */
    @ObjectField(enabled = false)
    @FetchContext(contexts = { SUMMARY }, include = { false })
    private Map<String, DeploymentArtifact> inputArtifacts;

    @ObjectField(enabled = false)
    @FetchContext(contexts = { SUMMARY }, include = { false })
    private Map<String, NodeGroup> groups;

    /**
     * When not null, describe how this topology can be used to substitute a node type in another topology (topology composition).
     */
    @ObjectField(enabled = false)
    @FetchContext(contexts = { SUMMARY }, include = { false })
    private SubstitutionMapping substitutionMapping;

    /**
     * All the workflows associated with this topology.
     */
    @ObjectField(enabled = false)
    @FetchContext(contexts = { SUMMARY }, include = { false })
    private Map<String, Workflow> workflows;

    /**
     * This fields save workflows as it's declared in declarative workflows without any post processing.
     * This is necessary as post processing (flatten, remove unnecessary links, nodes) may change the workflow and make the declarative workflows not working.
     */
    @ObjectField(enabled = false)
    @FetchContext(contexts = { SUMMARY }, include = { false })
    private Map<String, Workflow> unprocessedWorkflows = new HashMap<>();

    /* Archive meta-data are also set as topology tags. */
    @NestedObject(nestedClass = Tag.class)
    private List<Tag> tags;

    @Id
    public String getId() {
        return Csar.createId(archiveName, archiveVersion);
    }

    public void setId(String id) {
        // Not authorized to set id as it's auto-generated from name and version
    }

    public void setArchiveVersion(String version) {
        this.archiveVersion = version;
        this.nestedVersion = new Version(version);
    }

    /**
     * /**
     * Return true if the topology is an empty topology (won't be saved on import).
     *
     * @return True if the topology is empty (doesn't contains any node).
     */
    public boolean isEmpty() {
        return nodeTemplates == null || nodeTemplates.isEmpty();
    }

	public Workflow getWorkflow(String name) {
		return workflows.get(name);
	}

    private Map<String, String> metaProperties;
}