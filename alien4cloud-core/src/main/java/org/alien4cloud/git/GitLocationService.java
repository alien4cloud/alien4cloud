package org.alien4cloud.git;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.UUID;

import javax.inject.Inject;

import org.alien4cloud.git.model.GitLocation;
import org.alien4cloud.git.model.GitLocation.GitType;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Required;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import alien4cloud.security.AuthorizationUtil;
import alien4cloud.security.model.User;
import alien4cloud.utils.FileUtil;
import lombok.SneakyThrows;

@Service
public class GitLocationService {
    private static final String DEFAULT_BRANCH = "master";
    private static final String DEFAULT_PATH = "/";

    @Inject
    private GitLocationDao gitLocationDao;
    @Inject
    private LocalGitManager localGitManager;
    @Inject
    private LocalGitRepositoryPathResolver localGitRepositoryPathResolver;

    private Path tempDirPath;

    @Required
    @Value("${directories.alien}/tmp")
    public void setTempDirPath(String tempDirPath) throws IOException {
        this.tempDirPath = FileUtil.createDirectoryIfNotExists(tempDirPath);
    }

    @Inject
    public GitLocationService(LocalGitRepositoryPathResolver localGitRepositoryPathResolver) {
        this.localGitRepositoryPathResolver = localGitRepositoryPathResolver;
    }

    /**
     * Create or update a git location to a remote git repository.
     * 
     * @param gitLocation The git location to update.
     */
    public void updateToRemoteGit(GitLocation gitLocation) {
        // path cannot leave the git root directory
        boolean incorrectPath = Paths.get(gitLocation.getPath()).normalize().startsWith("..");
        if (incorrectPath) {
            throw new IllegalStateException("Incorrect path <" + gitLocation.getPath() + ">");
        }

        String protocol = StringUtils.substringBefore(gitLocation.getUrl(), "://");
        if (StringUtils.isBlank(protocol)) {
            throw new IllegalStateException("Incorrect URL. Missing protocol <" + gitLocation.getUrl() + ">");
        }
        // file:// protocol is not allowed for security reason and only supported for alien managed repository
        if (protocol.startsWith("file")) {
            throw new IllegalStateException("Protocol <" + protocol + "> is not allowed");
        }

        updateGitLocation(gitLocation);
    }

    /**
     * Reset the git that stores deployment setup to be managed by alien 4 cloud.
     * 
     * @param applicationId The id of the application of the deployment setup git to reset.
     * @param environmentId The id of the environment of the deployment setup git to reset.
     */
    public void resetDeploymentSetupToLocalGit(String applicationId, String environmentId) {
        updateGitLocation(getDeploymentSetupLocalGitLocation(applicationId, environmentId));
    }

    /**
     * Initialize a git location to store deployment setup data for an environment.
     * 
     * @param applicationId The id of the application.
     * @param environmentId The id of the environment.
     * @return a git location instance that contains repository data to store deployment setup.
     */
    public GitLocation getDeploymentSetupLocalGitLocation(String applicationId, String environmentId) {
        return GitLocation.builder().id(GitLocation.IdBuilder.forDeploymentSetup(applicationId, environmentId)).branch(DEFAULT_BRANCH)
                .gitType(GitType.DeploymentConfig).credential(null)
                .url(localGitRepositoryPathResolver.findEnvironmentSetupLocalPath(applicationId, environmentId).toUri().toString()).path(DEFAULT_PATH).build();
    }

    /**
     * Reset the git that stores application variables to be managed by alien 4 cloud.
     * 
     * @param applicationId The if of the application of the variables git to reset.
     */
    public void resetApplicationVariablesToLocalGit(String applicationId) {
        updateGitLocation(getApplicationVariablesLocalGitLocation(applicationId));
    }

    /**
     * Initialize a git location to store application variables.
     *
     * @param applicationId The id of the application.
     * @return a git location instance that contains repository data to store deployment setup.
     */
    public GitLocation getApplicationVariablesLocalGitLocation(String applicationId) {
        return GitLocation.builder().id(GitLocation.IdBuilder.forApplicationVariables(applicationId)).branch(DEFAULT_BRANCH).gitType(GitType.DeploymentConfig)
                .credential(null).url(localGitRepositoryPathResolver.findApplicationVariableLocalPath(applicationId).toUri().toString()).path(DEFAULT_PATH)
                .build();
    }

    @SneakyThrows(IOException.class)
    private void updateGitLocation(GitLocation newGitLocation) {
        Path tempPath = tempDirPath.resolve(UUID.randomUUID().toString());
        GitLocation previousLocation = gitLocationDao.findById(newGitLocation.getId());
        if (previousLocation != null) {
            // move data for copy to a temporary location
            FileUtil.copy(localGitManager.getLocalGitPath(previousLocation), tempPath);
            // delte the repository
            localGitManager.deleteLocalGit(previousLocation);
        }

        // create the new one
        localGitManager.checkout(newGitLocation);
        gitLocationDao.save(newGitLocation);

        // copy all files that where defined and does not exist in the new repository.
        if (tempPath != null) {
            User currentUser = AuthorizationUtil.getCurrentUser();
            FileUtil.copy(tempPath, localGitManager.getLocalGitPath(newGitLocation), true);
            localGitManager.commitAndPush(newGitLocation, currentUser.getUsername(), currentUser.getEmail(),
                    "a4c: Copy existing deployment configuration to associated repository.");
            FileUtil.delete(tempPath);
        }
    }
}