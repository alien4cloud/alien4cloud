package org.alien4cloud.git;

import alien4cloud.dao.IGenericSearchDAO;
import org.alien4cloud.alm.deployment.configuration.model.AbstractDeploymentConfig;
import org.alien4cloud.git.model.GitLocation;
import org.springframework.stereotype.Component;

import javax.inject.Inject;
import javax.inject.Named;

@Component
public class GitLocationDao {

    private IGenericSearchDAO alienDao;
    private AlienManagedGitLocationBuilder alienManagedGitLocationBuilder;

    public final DeploymentConfig forDeploymentConfig = new DeploymentConfig();

    @Inject
    public GitLocationDao(@Named("alien-es-dao") IGenericSearchDAO alienDao, AlienManagedGitLocationBuilder alienManagedGitLocationBuilder) {
        this.alienDao = alienDao;
        this.alienManagedGitLocationBuilder = alienManagedGitLocationBuilder;
    }

    public GitLocation findById(String id) {
        return alienDao.findById(GitLocation.class, id);
    }

    public class DeploymentConfig {
        private DeploymentConfig() {
        }

        public GitLocation findByEnvironmentId(String environmentId) {
            String id = GitLocation.IdBuilder.DeploymentConfig.build(environmentId);

            // does a remote git has been configured ?
            GitLocation gitLocation = alienDao.findById(GitLocation.class, id);
            if (gitLocation == null) {
                // return alien managed git
                gitLocation = alienManagedGitLocationBuilder.forDeploymentConfig(environmentId);
            }

            return gitLocation;
        }

        public GitLocation findByDeploymentConfigId(String abstractDeploymentConfigId) {
            String environmentId = AbstractDeploymentConfig.extractInfoFromId(abstractDeploymentConfigId).getEnvironmentId();
            return findByEnvironmentId(environmentId);
        }

    }

    public void save(GitLocation location) {
        alienDao.save(location);
    }

    public void delete(String id) {
        alienDao.delete(GitLocation.class, id);
    }
}
