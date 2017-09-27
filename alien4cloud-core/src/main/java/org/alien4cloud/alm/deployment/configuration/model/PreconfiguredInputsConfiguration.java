package org.alien4cloud.alm.deployment.configuration.model;

import java.util.Map;

import org.alien4cloud.tosca.model.definitions.PropertyValue;

import com.google.common.collect.Maps;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class PreconfiguredInputsConfiguration extends AbstractDeploymentConfig {
    private Map<String, PropertyValue> inputs = Maps.newHashMap();

    public PreconfiguredInputsConfiguration(String versionId, String environmentId) {
        super(versionId, environmentId);
    }
}