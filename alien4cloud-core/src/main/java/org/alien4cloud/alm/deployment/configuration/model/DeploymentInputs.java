package org.alien4cloud.alm.deployment.configuration.model;

import java.util.Map;

import lombok.NoArgsConstructor;
import org.alien4cloud.tosca.model.definitions.AbstractPropertyValue;
import org.alien4cloud.tosca.model.definitions.DeploymentArtifact;
import org.alien4cloud.tosca.model.definitions.PropertyValue;
import org.elasticsearch.annotation.ESObject;
import org.elasticsearch.annotation.ObjectField;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.google.common.collect.Maps;

import alien4cloud.json.deserializer.PropertyValueDeserializer;
import lombok.Getter;
import lombok.Setter;

/**
 * Object that stores input values as specified by a deployer user.
 */
@Getter
@Setter
@ESObject
@NoArgsConstructor
public class DeploymentInputs extends AbstractDeploymentConfig {
    @ObjectField(enabled = false)
    @JsonDeserialize(contentUsing = PropertyValueDeserializer.class)
    private Map<String, AbstractPropertyValue> inputs = Maps.newHashMap();
    @ObjectField(enabled = false)
    private Map<String, DeploymentArtifact> inputArtifacts = Maps.newHashMap();

    public DeploymentInputs(String versionId, String environmentId) {
        super(versionId, environmentId);
    }
}