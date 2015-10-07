package alien4cloud.model.deployment;

import java.util.Date;
import java.util.Map;
import java.util.Set;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import org.elasticsearch.annotation.ESObject;
import org.elasticsearch.annotation.StringField;
import org.elasticsearch.annotation.query.TermFilter;
import org.elasticsearch.mapping.IndexType;

import alien4cloud.exception.IndexingServiceException;
import alien4cloud.model.components.CSARDependency;
import alien4cloud.model.topology.NodeGroup;
import alien4cloud.model.topology.NodeTemplate;
import alien4cloud.model.topology.Topology;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;

/**
 * Deployment topology is the topology for a given environment.
 *
 * It contains the location matching policies as well as node matching. Users can also add additional node templates (like network specific settings for
 * example).
 *
 * Anything can be added to this topology, nodes matched from the initial topology are specified here so additional properties can be configured.
 */
@Getter
@Setter
@ESObject
@NoArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public class DeploymentTopology extends Topology {
    @TermFilter
    @StringField(includeInAll = false, indexType = IndexType.not_analyzed)
    private String versionId;
    @TermFilter
    @StringField(includeInAll = false, indexType = IndexType.not_analyzed)
    private String environmentId;
    /** Id of the initial topology that this topology completes. */
    private String initialTopologyId;

    /** Save the last update date of the original topology **/
    private Date lastInitialTopologyUpdateDate = new Date();

    private Map<String, String> providerDeploymentProperties;

    // TODO add also the input artifacts here. /-> Note that they should/could be repository based.
    private Map<String, String> inputProperties;

    /**
     * The map that contains the user selected matching for nodes of the topology. key is the initial topology node id, value is the
     * (on-demand or service) location resource id.
     */
    private Map<String, String> substitutedNodes = Maps.newHashMap();

    private Map<String, NodeTemplate> originalNodes = Maps.newHashMap();

    private String orchestratorId;

    private Map<String, NodeGroup> locationGroups = Maps.newHashMap();

    private Set<CSARDependency> locationDependencies = Sets.newHashSet();

    /**
     * Utility method to generate an id for a deployment topology by concatenating version id and environment id
     * 
     * @param versionId id of the version
     * @param environmentId id of the environment
     * @return concatenated id
     */
    public static String generateId(String versionId, String environmentId) {
        if (versionId == null) {
            throw new IndexingServiceException("version id is mandatory");
        }
        if (environmentId == null) {
            throw new IndexingServiceException("environment id is mandatory");
        }
        return versionId + "::" + environmentId;
    }
}