package org.alien4cloud.git;

import alien4cloud.dao.IGenericSearchDAO;
import org.alien4cloud.git.model.GitHardcodedCredential;
import org.alien4cloud.git.model.GitLocation;
import org.springframework.stereotype.Component;

import javax.inject.Inject;
import javax.inject.Named;

@Component
public class AlienManagedGitLocationBuilder {
    private LocalGitRepositoryPathResolver localGitRepositoryPathResolver;

    @Inject
    public AlienManagedGitLocationBuilder(LocalGitRepositoryPathResolver localGitRepositoryPathResolver) {
        this.localGitRepositoryPathResolver = localGitRepositoryPathResolver;
    }

    public GitLocation forDeploymentConfig(String environmentId) {
        GitLocation gitLocation = new GitLocation();
        gitLocation.setId(GitLocation.IdBuilder.DeploymentConfig.build(environmentId));
        gitLocation.setBranch("master");
        gitLocation.setGitType(GitLocation.GitType.DeploymentConfig);
        gitLocation.setCredential(new GitHardcodedCredential(null, null));
        gitLocation.setUrl(localGitRepositoryPathResolver.forDeploymentConfig().resolveGitRoot(environmentId).toUri().toString());
        gitLocation.setPath("/");
        return gitLocation;
    }


}
