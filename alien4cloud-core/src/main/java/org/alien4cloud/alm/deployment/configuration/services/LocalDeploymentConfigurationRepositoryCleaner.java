package org.alien4cloud.alm.deployment.configuration.services;

import javax.inject.Inject;

import org.alien4cloud.alm.events.BeforeApplicationEnvironmentDeleted;
import org.alien4cloud.alm.events.BeforeApplicationTopologyVersionDeleted;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/**
 * Clean local git repository when environment or topology is deleted
 */
@Component
public class LocalDeploymentConfigurationRepositoryCleaner {

    @Inject
    private DeploymentConfigurationDao deploymentConfigurationDao;

    @EventListener
    public void handleDeleteTopologyVersion(BeforeApplicationTopologyVersionDeleted event) {
        deploymentConfigurationDao.deleteAllByTopologyVersionId(event.getTopologyVersion());
    }

    @EventListener
    public void handleDeleteEnvironment(BeforeApplicationEnvironmentDeleted event) {
        deploymentConfigurationDao.deleteAllByEnvironmentId(event.getApplicationEnvironmentId());
    }
}
