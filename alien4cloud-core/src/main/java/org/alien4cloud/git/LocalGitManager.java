package org.alien4cloud.git;

import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import javax.inject.Inject;

import lombok.extern.slf4j.Slf4j;
import org.alien4cloud.alm.deployment.configuration.model.AbstractDeploymentConfig;
import org.alien4cloud.git.model.GitLocation;
import org.apache.commons.io.FileUtils;
import org.eclipse.jgit.api.Git;
import org.springframework.stereotype.Component;

import alien4cloud.git.RepositoryManager;
import alien4cloud.model.application.ApplicationEnvironment;
import lombok.SneakyThrows;

@Slf4j
@Component
public class LocalGitManager {

    private LocalGitRepositoryPathResolver localGitRepositoryPathResolver;
    private GitLocationDao gitLocationDao;

    @Inject
    public LocalGitManager(LocalGitRepositoryPathResolver localGitRepositoryPathResolver, GitLocationDao gitLocationDao) {
        this.localGitRepositoryPathResolver = localGitRepositoryPathResolver;
        this.gitLocationDao = gitLocationDao;
    }

    @SneakyThrows
    public Path getLocalGitPath(GitLocation gitLocation) {
        if (gitLocation.isLocal()) {
            return Paths.get(new URI(gitLocation.getUrl()));
        } else {
            switch (gitLocation.getGitType()) {
                case DeploymentConfig:
                    String environmentId = GitLocation.IdExtractor.DeploymentConfig.extractEnvironmentId(gitLocation.getId());
                    return localGitRepositoryPathResolver.forDeploymentConfig().resolveGitRoot(environmentId);

                default:
                    throw new IllegalArgumentException("GitType <" + gitLocation.getGitType() + "> is unknown");
            }
        }
    }

    @SneakyThrows
    public void deleteLocalGit(GitLocation location) {
        Path localGitPath = getLocalGitPath(location);
        if (Files.exists(localGitPath)) {
            FileUtils.deleteDirectory(localGitPath.toFile());
        }
    }

    public boolean hasLocalGit(GitLocation location) {
        Path localGitPath = getLocalGitPath(location);
        return Files.exists(localGitPath) && RepositoryManager.isGitRepository(localGitPath);
    }

    public void pull() {
        throw new RuntimeException("TODO");
    }

    public void createLocalGitIfNeeded(ApplicationEnvironment environment) {
        GitLocation location = gitLocationDao.forDeploymentConfig.findByEnvironmentId(environment.getId());
        createLocalGitIfNeeded(location);
    }


    @SneakyThrows
    public void createLocalGitIfNeeded(GitLocation location) {
        if (!hasLocalGit(location)) {
            Path localGitPath = getLocalGitPath(location);
            if (Files.exists(localGitPath)) {
                FileUtils.deleteDirectory(localGitPath.toFile());
            }

            if (location.isLocal()) {
                // create local git only
                log.debug("Creating local git under <" + localGitPath + ">");
                Files.createDirectories(localGitPath);
                RepositoryManager.create(localGitPath, null);
            } else {
                // clone remote git locally
                log.debug("About to clone the remote git <" + location.getUrl() + "> under <" + localGitPath + ">");
                Git git = null;
                try {
                    git = RepositoryManager.cloneOrCheckout(localGitPath, location.getUrl(), location.getCredential().getUsername(),
                            location.getCredential().getPassword(), location.getBranch(), ".");
                } finally {
                    if (git != null) {
                        git.close();
                    }
                }
            }
        }
    }

    public void commitAndPush(GitLocation location, String userName, String email, String commitMessage) {
        Path localGitPath = getLocalGitPath(location);
        RepositoryManager.commitAll(localGitPath, userName, email, commitMessage);
        if (!location.isLocal()) {
            RepositoryManager.push(localGitPath, location.getCredential().getUsername(), location.getCredential().getPassword(), location.getBranch());
            log.debug("commit and push changes to remote git <" + location.getUrl() + "> made by <" + userName + "/" + email + "> with commit message <" + commitMessage + ">");
        }else{
            log.debug("commit change locally at <" + localGitPath + "> made by <" + userName + "/" + email + "> with commit message <" + commitMessage + ">");
        }
    }

    public void reset(GitLocation location) {
        Path localGitPath = getLocalGitPath(location);
        RepositoryManager.reset(localGitPath);
        log.debug("Reset git " + localGitPath);

    }

    public void switchBranch(ApplicationEnvironment environment, String branch) {
        GitLocation location = gitLocationDao.forDeploymentConfig.findByDeploymentConfigId(AbstractDeploymentConfig.generateId(environment.getTopologyVersion(), environment.getId()));
        log.debug("About to checkout branch <" + branch + "> from repository " + location.getUrl());

        // FIXME: me ? branch should be stored if we need to change it depending on context?
        location.setBranch(branch);
        createLocalGitIfNeeded(location);
    }
}
