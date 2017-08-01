package org.alien4cloud.alm.deployment.configuration.services;

import java.util.Map;

import javax.annotation.Resource;
import javax.inject.Inject;

import org.alien4cloud.alm.deployment.configuration.events.OnDeploymentConfigCopyEvent;
import org.alien4cloud.alm.deployment.configuration.events.OnMatchedLocationChangedEvent;
import org.alien4cloud.alm.deployment.configuration.model.AbstractDeploymentConfig;
import org.alien4cloud.alm.deployment.configuration.model.DeploymentMatchingConfiguration;
import org.alien4cloud.alm.deployment.configuration.model.OrchestratorDeploymentProperties;
import org.alien4cloud.alm.events.AfterEnvironmentTopologyVersionChanged;
import org.alien4cloud.alm.events.BeforeApplicationEnvironmentDeleted;
import org.alien4cloud.alm.events.BeforeApplicationTopologyVersionDeleted;
import org.alien4cloud.tosca.exceptions.ConstraintTechnicalException;
import org.alien4cloud.tosca.exceptions.ConstraintValueDoNotMatchPropertyTypeException;
import org.alien4cloud.tosca.exceptions.ConstraintViolationException;
import org.alien4cloud.tosca.model.Csar;
import org.apache.commons.collections4.MapUtils;
import org.elasticsearch.index.query.QueryBuilders;
import org.springframework.context.event.EventListener;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Service;

import alien4cloud.dao.IGenericSearchDAO;
import alien4cloud.deployment.OrchestratorPropertiesValidationService;
import alien4cloud.model.application.ApplicationEnvironment;

/**
 * Manage configuration of orchestrator specific properties.
 */
@Service
public class OrchestratorPropertiesService {
    @Resource(name = "alien-es-dao")
    private IGenericSearchDAO alienDAO;
    @Inject
    private OrchestratorPropertiesValidationService orchestratorPropertiesValidationService;

    @EventListener
    public void onLocationChanged(OnMatchedLocationChangedEvent locationChangedEvent) {
        // Remove the values of the orchestrator specific properties.
        OrchestratorDeploymentProperties properties = alienDAO.findById(OrchestratorDeploymentProperties.class,
                AbstractDeploymentConfig.generateId(locationChangedEvent.getEnvironment().getTopologyVersion(), locationChangedEvent.getEnvironment().getId()));
        if (properties == null) {
            properties = new OrchestratorDeploymentProperties(locationChangedEvent.getEnvironment().getTopologyVersion(),
                    locationChangedEvent.getEnvironment().getId(), locationChangedEvent.getOrchestratorId());
            alienDAO.save(properties);
        } else {
            if (!locationChangedEvent.getOrchestratorId().equals(properties.getOrchestratorId())) {
                // orchestrator has changed so reset properties
                properties.setProviderDeploymentProperties(null);
                alienDAO.save(properties);
            }
        }
    }

    public void setOrchestratorProperties(ApplicationEnvironment environment, Map<String, String> providerDeploymentProperties) {
        if (MapUtils.isNotEmpty(providerDeploymentProperties)) {
            OrchestratorDeploymentProperties properties = alienDAO.findById(OrchestratorDeploymentProperties.class,
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

            alienDAO.save(properties);
        }
    }

    @EventListener
    @Order(40) // This is one of the last elements to process to place it's order quite far, after location match copy anyway.
    public void onCopyConfiguration(OnDeploymentConfigCopyEvent onDeploymentConfigCopyEvent) {
        ApplicationEnvironment source = onDeploymentConfigCopyEvent.getSourceEnvironment();
        ApplicationEnvironment target = onDeploymentConfigCopyEvent.getTargetEnvironment();
        DeploymentMatchingConfiguration sourceMatchingConfiguration = alienDAO.findById(DeploymentMatchingConfiguration.class,
                AbstractDeploymentConfig.generateId(source.getTopologyVersion(), source.getId()));
        if (sourceMatchingConfiguration == null || MapUtils.isEmpty(sourceMatchingConfiguration.getLocationIds())) {
            return;
        }
        DeploymentMatchingConfiguration targetMatchingConfiguration = alienDAO.findById(DeploymentMatchingConfiguration.class,
                AbstractDeploymentConfig.generateId(source.getTopologyVersion(), source.getId()));
        if (targetMatchingConfiguration == null || MapUtils.isEmpty(targetMatchingConfiguration.getLocationIds())
                || !sourceMatchingConfiguration.getOrchestratorId().equals(targetMatchingConfiguration.getOrchestratorId())) {
            // If the target does not have the same orchestrator as the source then don't copy the inputs
            return;
        }

        OrchestratorDeploymentProperties sourceProperties = alienDAO.findById(OrchestratorDeploymentProperties.class,
                AbstractDeploymentConfig.generateId(source.getTopologyVersion(), source.getId()));
        if (sourceProperties == null || MapUtils.isEmpty(sourceProperties.getProviderDeploymentProperties())) {
            return;
        }
        OrchestratorDeploymentProperties targetProperties = new OrchestratorDeploymentProperties(target.getTopologyVersion(), target.getId(),
                sourceProperties.getOrchestratorId());
        targetProperties.setProviderDeploymentProperties(sourceProperties.getProviderDeploymentProperties());
        alienDAO.save(targetProperties);
    }

    /**
     * This will clean up deployment setup when user promote to a new version.
     *
     * @param event the event fired
     */
    @EventListener
    public void handleEnvironmentTopologyVersionChanged(AfterEnvironmentTopologyVersionChanged event) {
        alienDAO.delete(OrchestratorDeploymentProperties.class,
                QueryBuilders.boolQuery().must(QueryBuilders.termQuery("versionId", Csar.createId(event.getApplicationId(), event.getOldVersion())))
                        .must(QueryBuilders.termQuery("environmentId", event.getEnvironmentId())));
    }

    @EventListener
    public void handleDeleteTopologyVersion(BeforeApplicationTopologyVersionDeleted event) {
        alienDAO.delete(OrchestratorDeploymentProperties.class,
                QueryBuilders.termQuery("versionId", Csar.createId(event.getApplicationId(), event.getTopologyVersion())));
    }

    @EventListener
    public void handleDeleteEnvironment(BeforeApplicationEnvironmentDeleted event) {
        alienDAO.delete(OrchestratorDeploymentProperties.class, QueryBuilders.termQuery("environmentId", event.getApplicationEnvironmentId()));
    }
}