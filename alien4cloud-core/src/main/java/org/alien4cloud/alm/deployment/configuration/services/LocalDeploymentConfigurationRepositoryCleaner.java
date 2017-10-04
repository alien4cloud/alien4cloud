package org.alien4cloud.alm.deployment.configuration.services;

import org.alien4cloud.alm.events.BeforeApplicationEnvironmentDeleted;
import org.alien4cloud.alm.events.BeforeApplicationTopologyVersionDeleted;
import org.alien4cloud.tosca.model.Csar;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import javax.inject.Inject;

/**
 * Clean local git repository when environment or topology is deleted
 */
@Component
public class LocalDeploymentConfigurationRepositoryCleaner {

    @Inject
    DeploymentConfigurationDao deploymentConfigurationDao;

    /**
     * This will clean up deployment setup when user promote to a new version.
     *
     * @param event the event fired
     */
    /*
    @EventListener
    public void handleEnvironmentTopologyVersionChanged(AfterEnvironmentTopologyVersionChanged event) {
        alienDAO.delete(DeploymentMatchingConfiguration.class,
                QueryBuilders.boolQuery().must(QueryBuilders.termQuery("versionId", Csar.createId(event.getApplicationId(), event.getOldVersion())))
                        .must(QueryBuilders.termQuery("environmentId", event.getEnvironmentId())));
    }*/
    @EventListener
    public void handleDeleteTopologyVersion(BeforeApplicationTopologyVersionDeleted event) {
        deploymentConfigurationDao.deleteAllByVersionId(Csar.createId(event.getApplicationId(), event.getTopologyVersion()));
    }

    @EventListener
    public void handleDeleteEnvironment(BeforeApplicationEnvironmentDeleted event) {
        deploymentConfigurationDao.deleteAllByEnvironmentId(event.getApplicationEnvironmentId());
    }
}
