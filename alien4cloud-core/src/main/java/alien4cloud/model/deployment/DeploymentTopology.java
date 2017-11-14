package alien4cloud.model.deployment;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.alien4cloud.tosca.model.CSARDependency;
import org.alien4cloud.tosca.model.definitions.DeploymentArtifact;
import org.alien4cloud.tosca.model.definitions.PropertyValue;
import org.alien4cloud.tosca.model.templates.NodeGroup;
import org.alien4cloud.tosca.model.templates.NodeTemplate;
import org.alien4cloud.tosca.model.templates.PolicyTemplate;
import org.alien4cloud.tosca.model.templates.Topology;
import org.elasticsearch.annotation.BooleanField;
import org.elasticsearch.annotation.ESObject;
import org.elasticsearch.annotation.ObjectField;
import org.elasticsearch.annotation.StringField;
import org.elasticsearch.annotation.query.TermFilter;
import org.elasticsearch.mapping.IndexType;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;

import alien4cloud.json.deserializer.PropertyValueDeserializer;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Deployment topology is the topology for a given environment.
 * <p/>
 * It contains the location matching policies as well as node matching. Users can also add additional node templates (like network specific settings for
 * example).
 * <p/>
 * Anything can be added to this topology, nodes matched from the initial topology are specified here so additional properties can be configured.
 */
@Getter
@Setter
@ESObject
@NoArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public class DeploymentTopology extends Topology {
    private String id;
    /** This flag is used to prevent update of a deployed archive as this may have consequences. */
    @TermFilter
    @BooleanField
    private boolean isDeployed = false;
    @TermFilter
    @StringField(includeInAll = false, indexType = IndexType.not_analyzed)
    private String versionId;
    @TermFilter
    @StringField(includeInAll = false, indexType = IndexType.not_analyzed)
    private String environmentId;
    /** Id of the initial topology that this topology completes. */
    private String initialTopologyId;

    // Location Matching data

    /** Id of the orchestrator (single one) that will manage the deployment. */
    private String orchestratorId;
    /** Id of the locations on which the orchestrator should deploy. */
    @ObjectField(enabled = false)
    private Map<String, NodeGroup> locationGroups = Maps.newHashMap();
    /** List of dependencies introduced by the locations - specific location types. */
    @ObjectField(enabled = false)
    private Set<CSARDependency> locationDependencies = Sets.newHashSet();

    // Node matching data

    /**
     * Date of the last matching update.
     * Used to make sure that we update the matching if the portable topology is updated or if the location resources are updated.
     */
    private Date lastDeploymentTopologyUpdateDate = new Date();

    /**
     * The map that contains the user selected matching for nodes of the topology. key is the initial topology node id, value is the
     * (on-demand or service) location resource id.
     */
    @ObjectField(enabled = false)
    private Map<String, String> substitutedNodes = Maps.newHashMap();

    @ObjectField(enabled = false)
    private Map<String, NodeTemplate> originalNodes = Maps.newHashMap();

    @ObjectField(enabled = false)
    private Map<String, NodeTemplate> matchReplacedNodes = Maps.newHashMap();

    /** Configuration of the deployment properties specific to the orchestrator if any. */
    @ObjectField(enabled = false)
    private Map<String, String> providerDeploymentProperties;
    /** Values of the input properties as configured by the deployer. */
    /** Migration Note: all data previously stored into 'inputProperties' should be moved into 'deployerInputProperties' */
    @ObjectField(enabled = false)
    @JsonDeserialize(contentUsing = PropertyValueDeserializer.class)
    private Map<String, PropertyValue> deployerInputProperties;

    @JsonIgnore
    public Map<String, PropertyValue> getAllInputProperties() {
        HashMap<String, PropertyValue> map = Maps.newHashMap();
        if (deployerInputProperties != null) {
            map.putAll(deployerInputProperties);
        }

        if (preconfiguredInputProperties != null) {
            map.putAll(preconfiguredInputProperties);
        }

        return map;
    }

    /** Values of the input properties configured with the input file */
    @ObjectField(enabled = false)
    @JsonDeserialize(contentUsing = PropertyValueDeserializer.class)
    private Map<String, PropertyValue> preconfiguredInputProperties;

    @ObjectField(enabled = false)
    private Map<String, DeploymentArtifact> uploadedInputArtifacts;

    /**
     * The map that contains the user selected matching for policies of the topology. key is the initial topology policy node id, value is the
     * location policy resource id.
     */
    @ObjectField(enabled = false)
    private Map<String, String> substitutedPolicies = Maps.newHashMap();

    @ObjectField(enabled = false)
    private Map<String, PolicyTemplate> originalPolicies = Maps.newHashMap();
}