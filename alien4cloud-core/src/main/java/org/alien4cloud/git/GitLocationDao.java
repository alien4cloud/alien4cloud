package org.alien4cloud.git;

import javax.inject.Inject;
import javax.inject.Named;

import org.alien4cloud.git.model.GitLocation;
import org.springframework.stereotype.Component;

import alien4cloud.dao.IGenericSearchDAO;

@Component
public class GitLocationDao {

    @Inject
    @Named("alien-es-dao")
    private IGenericSearchDAO alienDao;
    @Inject
    private GitLocationService gitLocationService;

    public GitLocation findById(String id) {
        return alienDao.findById(GitLocation.class, id);
    }

    public GitLocation findDeploymentSetupLocation(String applicationId, String environmentId) {
        String id = GitLocation.IdBuilder.forDeploymentSetup(applicationId, environmentId);

        // does a remote git has been configured ?
        GitLocation gitLocation = alienDao.findById(GitLocation.class, id);
        if (gitLocation == null) {
            // return alien managed git
            gitLocation = gitLocationService.getDeploymentSetupLocalGitLocation(applicationId, environmentId);
        }

        return gitLocation;
    }

    public GitLocation findApplicationVariablesLocation(String applicationId) {
        String id = GitLocation.IdBuilder.forApplicationVariables(applicationId);

        // does a remote git has been configured ?
        GitLocation gitLocation = alienDao.findById(GitLocation.class, id);
        if (gitLocation == null) {
            // return alien managed git
            gitLocation = gitLocationService.getApplicationVariablesLocalGitLocation(applicationId);
        }

        return gitLocation;
    }

    public void save(GitLocation location) {
        alienDao.save(location);
    }

    public void delete(String id) {
        alienDao.delete(GitLocation.class, id);
    }
}
