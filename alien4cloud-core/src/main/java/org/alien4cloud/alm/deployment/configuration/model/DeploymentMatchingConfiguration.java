package org.alien4cloud.alm.deployment.configuration.model;

import static alien4cloud.utils.AlienUtils.safe;

import java.util.Map;

import org.alien4cloud.tosca.model.definitions.AbstractPropertyValue;
import org.alien4cloud.tosca.model.templates.AbstractPolicy;
import org.alien4cloud.tosca.model.templates.LocationPlacementPolicy;
import org.alien4cloud.tosca.model.templates.NodeGroup;
import org.elasticsearch.annotation.ESObject;
import org.elasticsearch.annotation.ObjectField;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.google.common.collect.Maps;

import alien4cloud.json.deserializer.PropertyValueDeserializer;
import alien4cloud.utils.jackson.ConditionalAttributes;
import alien4cloud.utils.jackson.ConditionalOnAttribute;
import alien4cloud.utils.jackson.JSonMapEntryArrayDeSerializer;
import alien4cloud.utils.jackson.JSonMapEntryArraySerializer;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Object that stores matching configuration as specified by a deployer user.
 */
@Getter
@Setter
@NoArgsConstructor
@ESObject
public class DeploymentMatchingConfiguration extends AbstractDeploymentConfig {
    /**  */
    private String orchestratorId;
    /** Location matching definitions. */
    @ObjectField(enabled = false)
    private Map<String, NodeGroup> locationGroups = Maps.newHashMap();
    /** Map of policy template id -> policy resource id for the policies that are substituted. */
    @ObjectField(enabled = false)
    private Map<String, String> matchedPolicies = Maps.newHashMap();
    /** Map of node template id -> location resource id for the nodes that are substituted. */
    @ObjectField(enabled = false)
    private Map<String, String> matchedLocationResources = Maps.newHashMap();
    /**
     * Configuration of the node template resulting from the matching.
     * This contains post-matching deployer defined properties.
     */
    @ObjectField(enabled = false)
    private Map<String, NodePropsOverride> matchedNodesConfiguration = Maps.newHashMap();

    @ObjectField(enabled = false)
    private Map<String, NodePropsOverride> matchedPoliciesConfiguration = Maps.newHashMap();

    public DeploymentMatchingConfiguration(String versionId, String environmentId) {
        super(versionId, environmentId);
    }

    /**
     * Get the location ids out of the location groups settings.
     *
     * @return map of location group to location id
     */
    public Map<String, String> getLocationIds() {
        Map<String, String> locationIds = Maps.newHashMap();

        for (NodeGroup group : safe(locationGroups).values()) {
            for (AbstractPolicy policy : safe(group.getPolicies())) {
                if (policy instanceof LocationPlacementPolicy) {
                    locationIds.put(group.getName(), ((LocationPlacementPolicy) policy).getLocationId());
                }
            }
        }

        return locationIds;
    }

    @Getter
    @Setter
    public static class NodePropsOverride {
        @ObjectField(enabled = false)
        @ConditionalOnAttribute(ConditionalAttributes.REST)
        @JsonDeserialize(using = JSonMapEntryArrayDeSerializer.class, contentUsing = PropertyValueDeserializer.class)
        @JsonSerialize(using = JSonMapEntryArraySerializer.class)
        /** Properties that are configured post-matching */
        private Map<String, AbstractPropertyValue> properties = Maps.newHashMap();
        /** Capabilities properties that are configured post-matching */
        @ObjectField(enabled = false)
        @ConditionalOnAttribute(ConditionalAttributes.REST)
        @JsonDeserialize(using = JSonMapEntryArrayDeSerializer.class)
        @JsonSerialize(using = JSonMapEntryArraySerializer.class)
        private Map<String, NodeCapabilitiesPropsOverride> capabilities = Maps.newHashMap();
    }

    @Getter
    @Setter
    public static class NodeCapabilitiesPropsOverride {
        @ObjectField(enabled = false)
        @ConditionalOnAttribute(ConditionalAttributes.REST)
        @JsonDeserialize(using = JSonMapEntryArrayDeSerializer.class, contentUsing = PropertyValueDeserializer.class)
        @JsonSerialize(using = JSonMapEntryArraySerializer.class)
        /** Properties that are configured post-matching */
        private Map<String, AbstractPropertyValue> properties = Maps.newHashMap();
    }
}
