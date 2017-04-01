package org.alien4cloud.alm.model;

import java.util.Date;
import java.util.Map;

import org.alien4cloud.tosca.model.definitions.DeploymentArtifact;
import org.alien4cloud.tosca.model.definitions.PropertyValue;
import org.alien4cloud.tosca.model.templates.NodeGroup;
import org.alien4cloud.tosca.model.templates.NodeTemplate;
import org.elasticsearch.annotation.ObjectField;
import org.elasticsearch.annotation.StringField;
import org.elasticsearch.annotation.query.TermFilter;
import org.elasticsearch.mapping.IndexType;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.google.common.collect.Maps;

import alien4cloud.json.deserializer.PropertyValueDeserializer;
import alien4cloud.model.deployment.DeploymentTopology;

/**
 * <p>
 * For every couple ApplicationTopologyVersion/ApplicationEnvironment alien4cloud holds a deployment configuration.
 * </p>
 * <p>
 * The deployment configuration contains all parameters that are required so that a TOSCA topology can be deployed in alien4cloud. This includes the following:
 * </p>
 * <ul>
 * <li>Topology input properties</li>
 * <li>Topology input artifacts</li>
 * <li>Location matching configuration</li>
 * <li>Node matching configuration (including defined properties for the matched nodes)</li>
 * <li>Orchestrator deployment options if any</li>
 * <li>Service configuration</li>
 * </ul>
 *
 * The topology that alien4cloud actually deploys is basically the sum of all these elements.
 */
public class DeploymentConfiguration {
    @TermFilter
    @StringField(includeInAll = false, indexType = IndexType.not_analyzed)
    private String versionId;
    @TermFilter
    @StringField(includeInAll = false, indexType = IndexType.not_analyzed)
    private String environmentId;

    /** Inputs configurations as defined by the user. */
    @ObjectField(enabled = false)
    @JsonDeserialize(contentUsing = PropertyValueDeserializer.class)
    private Map<String, PropertyValue> inputProperties;
    @ObjectField(enabled = false)
    private Map<String, DeploymentArtifact> uploadedInputArtifacts;

    /** Location matching definitions. */
    @ObjectField(enabled = false)
    private Map<String, NodeGroup> locationGroups = Maps.newHashMap();

    /** Node matching configuration. */
    @ObjectField(enabled = false)
    private Map<String, NodeTemplate> matchedNodes = Maps.newHashMap();

    /** Configuration of the deployment properties specific to the orchestrator if any. */
    @ObjectField(enabled = false)
    private Map<String, String> providerDeploymentProperties;

    /**
     * The deployment configuration is related to an actual topology, if the topology changes elements of the configuration as inputs, matched nodes etc. may
     * change. This field is used for technical synchronizations purpose.
     */
    private Date originalTopologySyncDate = new Date();

    /**
     * Return the id of the deployment.
     * 
     * @return
     */
    public String getId() {
        return DeploymentTopology.generateId(versionId, environmentId);
    }

    public void setId() {
        // do nothing as id is generated from version and environment.
    }
}