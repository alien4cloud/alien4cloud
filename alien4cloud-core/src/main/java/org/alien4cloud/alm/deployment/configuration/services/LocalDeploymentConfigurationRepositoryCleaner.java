package org.alien4cloud.alm.deployment.configuration.services;

import javax.inject.Inject;

import org.alien4cloud.alm.events.BeforeApplicationEnvironmentDeleted;
import org.alien4cloud.alm.events.BeforeApplicationTopologyVersionDeleted;
import org.alien4cloud.git.GitLocationDao;
import org.alien4cloud.git.LocalGitManager;
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
        deploymentConfigurationDao.deleteAllByVersionId(event.getVersionId());
    }

    @EventListener
    public void handleDeleteEnvironment(BeforeApplicationEnvironmentDeleted event) {
        deploymentConfigurationDao.deleteAllByEnvironmentId(event.getApplicationEnvironmentId());
    }
}
