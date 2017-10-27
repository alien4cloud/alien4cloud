package org.alien4cloud.alm.deployment.configuration.services;

import alien4cloud.deployment.OrchestratorPropertiesValidationService;
import alien4cloud.model.application.ApplicationEnvironment;
import org.alien4cloud.alm.deployment.configuration.events.OnDeploymentConfigCopyEvent;
import org.alien4cloud.alm.deployment.configuration.events.OnMatchedLocationChangedEvent;
import org.alien4cloud.alm.deployment.configuration.model.AbstractDeploymentConfig;
import org.alien4cloud.alm.deployment.configuration.model.DeploymentMatchingConfiguration;
import org.alien4cloud.alm.deployment.configuration.model.OrchestratorDeploymentProperties;
import org.alien4cloud.tosca.exceptions.ConstraintTechnicalException;
import org.alien4cloud.tosca.exceptions.ConstraintValueDoNotMatchPropertyTypeException;
import org.alien4cloud.tosca.exceptions.ConstraintViolationException;
import org.apache.commons.collections4.MapUtils;
import org.springframework.context.event.EventListener;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Service;

import javax.inject.Inject;
import java.util.Map;

/**
 * Manage configuration of orchestrator specific properties.
 */
@Service
public class OrchestratorPropertiesService {
    @Inject
    private DeploymentConfigurationDao deploymentConfigurationDao;
    @Inject
    private OrchestratorPropertiesValidationService orchestratorPropertiesValidationService;

    @EventListener
    public void onLocationChanged(OnMatchedLocationChangedEvent locationChangedEvent) {
        // Remove the values of the orchestrator specific properties.
        OrchestratorDeploymentProperties properties = deploymentConfigurationDao.findById(OrchestratorDeploymentProperties.class,
                AbstractDeploymentConfig.generateId(locationChangedEvent.getEnvironment().getTopologyVersion(), locationChangedEvent.getEnvironment().getId()));
        if (properties == null || !locationChangedEvent.getOrchestratorId().equals(properties.getOrchestratorId())) {
            // Either no orchestrator properties set until now or orchestrator has changed, so reset properties
            properties = new OrchestratorDeploymentProperties(locationChangedEvent.getEnvironment().getTopologyVersion(),
                    locationChangedEvent.getEnvironment().getId(), locationChangedEvent.getOrchestratorId());
            deploymentConfigurationDao.save(properties);
        }
    }

    public void setOrchestratorProperties(ApplicationEnvironment environment, Map<String, String> providerDeploymentProperties) {
        if (MapUtils.isNotEmpty(providerDeploymentProperties)) {
            OrchestratorDeploymentProperties properties = deploymentConfigurationDao.findById(OrchestratorDeploymentProperties.class,
                    AbstractDeploymentConfig.generateId(environment.getTopologyVersion(), environment.getId()));

            try {
                orchestratorPropertiesValidationService.checkConstraints(properties.getOrchestratorId(), providerDeploymentProperties);
            } catch (ConstraintValueDoNotMatchPropertyTypeException | ConstraintViolationException e) {
                throw new ConstraintTechnicalException("Error on deployer user orchestrator properties validation", e);
            }

            if (properties.getProviderDeploymentProperties() == null) {
                properties.setProviderDeploymentProperties(providerDeploymentProperties);
            } else {
                properties.getProviderDeploymentProperties().putAll(providerDeploymentProperties);
            }

            deploymentConfigurationDao.save(properties);
        }
    }

    @EventListener
    @Order(40) // This is one of the last elements to process to place it's order quite far, after location match copy anyway.
    public void onCopyConfiguration(OnDeploymentConfigCopyEvent onDeploymentConfigCopyEvent) {
        ApplicationEnvironment source = onDeploymentConfigCopyEvent.getSourceEnvironment();
        ApplicationEnvironment target = onDeploymentConfigCopyEvent.getTargetEnvironment();
        DeploymentMatchingConfiguration sourceMatchingConfiguration = deploymentConfigurationDao.findById(DeploymentMatchingConfiguration.class,
                AbstractDeploymentConfig.generateId(source.getTopologyVersion(), source.getId()));
        if (sourceMatchingConfiguration == null || MapUtils.isEmpty(sourceMatchingConfiguration.getLocationIds())) {
            return;
        }
        DeploymentMatchingConfiguration targetMatchingConfiguration = deploymentConfigurationDao.findById(DeploymentMatchingConfiguration.class,
                AbstractDeploymentConfig.generateId(source.getTopologyVersion(), source.getId()));
        if (targetMatchingConfiguration == null || MapUtils.isEmpty(targetMatchingConfiguration.getLocationIds())
                || !sourceMatchingConfiguration.getOrchestratorId().equals(targetMatchingConfiguration.getOrchestratorId())) {
            // If the target does not have the same orchestrator as the source then don't copy the inputs
            return;
        }

        OrchestratorDeploymentProperties sourceProperties = deploymentConfigurationDao.findById(OrchestratorDeploymentProperties.class,
                AbstractDeploymentConfig.generateId(source.getTopologyVersion(), source.getId()));
        if (sourceProperties == null || MapUtils.isEmpty(sourceProperties.getProviderDeploymentProperties())) {
            return;
        }
        OrchestratorDeploymentProperties targetProperties = new OrchestratorDeploymentProperties(target.getTopologyVersion(), target.getId(),
                sourceProperties.getOrchestratorId());
        targetProperties.setProviderDeploymentProperties(sourceProperties.getProviderDeploymentProperties());
        deploymentConfigurationDao.save(targetProperties);
    }

}