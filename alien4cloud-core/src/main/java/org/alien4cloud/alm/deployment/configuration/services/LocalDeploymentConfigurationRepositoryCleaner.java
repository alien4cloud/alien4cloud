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
    DeploymentConfigurationDao deploymentConfigurationDao;

    @EventListener
    public void handleDeleteTopologyVersion(BeforeApplicationTopologyVersionDeleted event) {
        // TODO: we need to delete a branch - should we do it only for A4C managed git ?
        //deploymentConfigurationDao.deleteAllByVersionId(Csar.createId(event.getApplicationId(), event.getTopologyVersion()));
    }

    @EventListener
    public void handleDeleteEnvironment(BeforeApplicationEnvironmentDeleted event) {
        deploymentConfigurationDao.deleteAllByEnvironmentId(event.getApplicationEnvironmentId());
    }
}
