package alien4cloud.model.deployment;

import java.util.Map;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import org.elasticsearch.annotation.ESObject;

import alien4cloud.model.topology.Topology;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.google.common.collect.Maps;

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
    /** Id of the initial topology that this topology completes. */
    private String initialTopologyId;
    /**
     * The map that contains the user selected matching for nodes of the topology. key is the initial topology node, value is the id of an orchestrator resource
     * (on-demand or service).
     */
    private Map<String, String> nodeTemplateMatching = Maps.newHashMap();
}