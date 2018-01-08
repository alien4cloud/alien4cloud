package org.alien4cloud.git;

import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;

import javax.inject.Inject;

import org.alien4cloud.git.model.GitLocation;
import org.alien4cloud.git.model.GitLocation.AppEnvIds;
import org.apache.commons.io.FileUtils;
import org.eclipse.jgit.api.Git;
import org.springframework.stereotype.Component;

import alien4cloud.git.RepositoryManager;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
public class LocalGitManager {
    @Inject
    private LocalGitRepositoryPathResolver localGitRepositoryPathResolver;

    @SneakyThrows
    public Path getLocalGitPath(GitLocation gitLocation) {
        if (gitLocation.isLocal()) {
            return Paths.get(new URI(gitLocation.getUrl()));
        } else {
            switch (gitLocation.getGitType()) {
            case DeploymentConfig:
                AppEnvIds appEnvIds = GitLocation.IdExtractor.fromDeploymentSetup(gitLocation.getId());
                return localGitRepositoryPathResolver.findEnvironmentSetupLocalPath(appEnvIds.getApplicationId(), appEnvIds.getEnvironmentId());
            case ApplicationVariables:
                String applicationId = GitLocation.IdExtractor.fromApplicationVariables(gitLocation.getId());
                return localGitRepositoryPathResolver.findApplicationVariableLocalPath(applicationId);
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

    public void pull() {
        throw new RuntimeException("TODO");
    }

    @SneakyThrows
    public void checkout(GitLocation location) {
        Path localGitPath = getLocalGitPath(location);

        if (!gitRepositoryExistLocally(location)) {
            if (Files.exists(localGitPath)) {
                FileUtils.deleteDirectory(localGitPath.toFile());
            }

            if (location.isLocal()) {
                // create local git only
                log.debug("Creating local git under <" + localGitPath + ">");
                Files.createDirectories(localGitPath);
                RepositoryManager.create(localGitPath, null);
                RepositoryManager.checkoutExistingBranchOrCreateOrphan(localGitPath, location.isLocal(), location.usernameOrNull(), location.passwordOrNull(),
                        location.getBranch());
            } else {
                // clone remote git locally
                log.debug("About to clone the remote git <" + location.getUrl() + "> under <" + localGitPath + ">");
                safeCloneOrCheckout(location, localGitPath);
            }
        } else {
            checkoutBranchWithAutoStashIfNeeded(location, location.getBranch());
        }
    }

    public void commitAndPush(GitLocation location, String userName, String email, String commitMessage) {
        Path localGitPath = getLocalGitPath(location);
        RepositoryManager.commitAll(localGitPath, userName, email, commitMessage);
        if (!location.isLocal()) {
            RepositoryManager.push(localGitPath, location.usernameOrNull(), location.passwordOrNull(), location.getBranch());
            log.debug("commit and push changes to remote git <" + location.getUrl() + "> made by <" + userName + "/" + email + "> with commit message <"
                    + commitMessage + ">");
        } else {
            log.debug("commit change locally at <" + localGitPath + "> made by <" + userName + "/" + email + "> with commit message <" + commitMessage + ">");
        }
    }

    public void clean(GitLocation location) {
        Path localGitPath = getLocalGitPath(location);
        RepositoryManager.clean(localGitPath);
        log.debug("Reset git " + localGitPath);
    }

    public void deleteBranch(GitLocation location, String branch) {
        Path localGitPath = getLocalGitPath(location);

        // The current behavior is to NOT delete remote branch
        // only local branch will be deleted
        // boolean deleteRemoteBranch = !location.isLocal();
        boolean deleteRemoteBranch = false;

        RepositoryManager.deleteBranch(localGitPath, branch, deleteRemoteBranch);
        log.debug("Deleted branch <" + branch + "> from " + location.getUrl());
    }

    /**
     * Check if a git repository exists locally AND check remote git url.
     *
     * In case {@link GitLocation} describe a remote git, then a extra check is made:
     * the remote origin url need to match the remote git url defined into {@link GitLocation}
     *
     * @param location git location information to check
     * @return true if a git repository exists locally
     */
    private boolean gitRepositoryExistLocally(GitLocation location) {
        Path localGitPath = getLocalGitPath(location);
        if (Files.exists(localGitPath)) {
            if (location.isLocal()) {
                return RepositoryManager.isGitRepository(localGitPath);
            } else {
                return RepositoryManager.isGitRepository(localGitPath, location.getUrl());
            }
        }

        return false;
    }

    private void checkoutBranchWithAutoStashIfNeeded(GitLocation location, String branch) {
        Path localGitPath = getLocalGitPath(location);
        String currentBranchName = RepositoryManager.getCurrentBranchName(localGitPath);
        if (!currentBranchName.equals(branch)) {
            log.debug("About to checkout from <" + currentBranchName + "> to <" + branch + "> from repository " + location.getUrl());
            RepositoryManager.stash(localGitPath, "a4c_stash_" + currentBranchName);
            RepositoryManager.checkoutExistingBranchOrCreateOrphan(localGitPath, location.isLocal(), location.usernameOrNull(), location.passwordOrNull(),
                    branch);
            RepositoryManager.applyStashThenDrop(localGitPath, "a4c_stash_" + branch);
        }
    }

    private void safeCloneOrCheckout(GitLocation location, Path localGitPath) {
        Git git = null;
        try {
            git = RepositoryManager.cloneOrCheckout(localGitPath, location.getUrl(), location.usernameOrNull(), location.passwordOrNull(), location.getBranch(),
                    ".");
        } finally {
            if (git != null) {
                git.close();
            }
        }
    }

    public void renameBranches(GitLocation location, Map<String, String> branchNameFromTo) {
        Path localGitPath = getLocalGitPath(location);
        RepositoryManager.renameBranches(localGitPath, branchNameFromTo);
    }
}
