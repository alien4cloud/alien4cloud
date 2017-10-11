package org.alien4cloud.alm.deployment.configuration.flow;

import java.util.Date;
import java.util.Map;
import java.util.Optional;

import org.alien4cloud.alm.deployment.configuration.model.AbstractDeploymentConfig;
import org.alien4cloud.tosca.model.templates.Topology;
import org.elasticsearch.annotation.ESObject;

import com.google.common.collect.Maps;

import alien4cloud.dao.IGenericSearchDAO;
import alien4cloud.model.application.ApplicationEnvironment;
import lombok.Getter;
import lombok.Setter;

/**
 * Flow execution context.
 */
@Getter
@Setter
public class FlowExecutionContext {
    /** A4C OOB keys of elements in the context cache. */
    public static final String LOCATION_MATCH_CACHE_KEY = "location_matches";
    public static final String DEPLOYMENT_LOCATIONS_MAP_CACHE_KEY = "deployment_locations";
    /** Location resource template candidates per node template key. */
    public static final String MATCHED_NODE_LOCATION_TEMPLATES_BY_NODE_ID_MAP = "matched_node_location_templates_by_node_id_map";
    public static final String MATCHED_NODE_LOCATION_TEMPLATES_BY_ID_MAP = "matched_node_location_templates_by_id_map";
    public static final String SELECTED_MATCH_NODE_LOCATION_TEMPLATE_BY_NODE_ID_MAP = "selected_match_node_location_template_by_node_id_map";
    public static final String MATCHING_ORIGINAL_NODES = "matching_original_nodes";
    public static final String MATCHING_SUBSTITUTION_REQUEST = "matching_substitution_request";
    /** Location resource template candidates per policy template key. */
    public static final String MATCHED_POLICY_LOCATION_TEMPLATES_BY_NODE_ID_MAP = "matched_policy_location_templates_by_node_id_map";
    public static final String MATCHED_POLICY_LOCATION_TEMPLATES_BY_ID_MAP = "matched_policy_location_templates_by_id_map";
    public static final String SELECTED_MATCH_POLICY_LOCATION_TEMPLATE_BY_NODE_ID_MAP = "selected_match_policy_location_template_by_node_id_map";
    public static final String MATCHING_ORIGINAL_POLICIES = "matching_original_policies";

    /** Injected dao for configuration retrieval management. */
    private final IGenericSearchDAO alienDAO;
    /** The topology after impact from the various topology modifiers. */
    private final Topology topology;
    /** Optional environment context for modifiers that runs in the context of an environment. */
    private final Optional<EnvironmentContext> environmentContext;
    /**
     * Execution cache may be used by topology modifier to store some results for usage later in the flow.
     * It is also used to store location and node matching results to be sent back to UI.
     */
    private Map<String, Object> executionCache = Maps.newHashMap();
    /** Logger kind of object so topology modifiers may provide user feedback. */
    private FlowExecutionLog log = new FlowExecutionLog();
    /** Date of the last updated topology or configuration in the current processed flow. */
    private Date lastFlowParamUpdate;

    public FlowExecutionContext(IGenericSearchDAO alienDAO, Topology topology, EnvironmentContext environmentContext) {
        this.alienDAO = alienDAO;
        this.topology = topology;
        this.environmentContext = Optional.of(environmentContext);
        lastFlowParamUpdate = topology.getLastUpdateDate();
    }

    /**
     * Shorthand getter to get the context log.
     * 
     * @return The Flow execution log.
     */
    public FlowExecutionLog log() {
        return log;
    }

    /**
     * Get a configuration object related to the deployment flow.
     *
     * This operation also updates the lastFlowParamUpdate that may be used by later processor to skip some processing when nothing has changed.
     *
     * The operation is also caching aware to avoid requesting multiple times the same object from elasticsearch.
     *
     * The configuration object is not annotated with {@link ESObject}, no request to elasticsearch will be made.
     * 
     * @param cfgClass The class of the configuration object.
     * @param modifierName Name of the modifier that tries to access a deployment configuration object (related to the environment).
     * @param <T> The type of the configuration object.
     * @return An instance of the requested configuration object.
     */
    public <T extends AbstractDeploymentConfig> Optional<T> getConfiguration(Class<T> cfgClass, String modifierName) {
        environmentContext.orElseThrow(() -> new EnvironmentContextRequiredException(modifierName));
        ApplicationEnvironment env = environmentContext.get().getEnvironment();
        String cfgId = AbstractDeploymentConfig.generateId(env.getTopologyVersion(), env.getId());
        String configCacheId = cfgClass.getSimpleName() + "/" + cfgId;
        T config = (T) executionCache.get(configCacheId);
        // If the config object is annotated with ESObject then it may be cached in ElasticSearch
        if (config == null && cfgClass.isAnnotationPresent(ESObject.class)) {
            config = alienDAO.findById(cfgClass, cfgId);
            executionCache.put(configCacheId, config);
        }
        if (config == null) {
            return Optional.empty();
        }
        if (lastFlowParamUpdate.before(config.getLastUpdateDate())) {
            lastFlowParamUpdate = config.getLastUpdateDate();
        }
        return Optional.of(config);
    }

    /**
     * Counterparty method of the getConfiguration to actually update a configuration both in elasticsearch
     * (configuration object MUST be annotated with {@link ESObject} and the local configuration cache.
     * 
     * @param configuration The configuration to update.
     */
    public void saveConfiguration(AbstractDeploymentConfig configuration) {
        String configCacheId = configuration.getClass().getSimpleName() + "/" + configuration.getId();
        executionCache.put(configCacheId, configuration);
        if (configuration.getClass().isAnnotationPresent(ESObject.class)) {
            alienDAO.save(configuration); // This also updates the date.
        }
        lastFlowParamUpdate = configuration.getLastUpdateDate();
    }

}