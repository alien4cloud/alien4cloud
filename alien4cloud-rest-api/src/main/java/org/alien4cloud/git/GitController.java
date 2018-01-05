package org.alien4cloud.git;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.UUID;

import javax.inject.Inject;
import javax.validation.Valid;

import org.alien4cloud.git.model.GitHardcodedCredential;
import org.alien4cloud.git.model.GitLocation;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Required;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import alien4cloud.application.ApplicationEnvironmentService;
import alien4cloud.application.ApplicationService;
import alien4cloud.audit.annotation.Audit;
import alien4cloud.model.application.Application;
import alien4cloud.model.application.ApplicationEnvironment;
import alien4cloud.rest.model.RestResponse;
import alien4cloud.rest.model.RestResponseBuilder;
import alien4cloud.security.AuthorizationUtil;
import alien4cloud.security.model.User;
import alien4cloud.utils.FileUtil;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import lombok.SneakyThrows;

@RestController
@RequestMapping(value = { "/rest/git", "/rest/v1/git", "/rest/latest/git" })
@Api(value = "", description = "Operations to manage custom Git repositories")
public class GitController {

    @Inject
    private LocalGitManager localGitManager;
    @Inject
    private GitLocationDao gitLocationDao;
    @Inject
    private ApplicationService applicationService;
    @Inject
    private ApplicationEnvironmentService environmentService;
    @Inject
    private AlienManagedGitLocationBuilder alienManagedGitLocationBuilder;

    private Path tempDirPath;

    @Required
    @Value("${directories.alien}/tmp")
    public void setTempDirPath(String tempDirPath) throws IOException {
        this.tempDirPath = FileUtil.createDirectoryIfNotExists(tempDirPath);
    }

    @ApiOperation(value = "Update the git repository parameters for storing deployment config")
    @RequestMapping(value = "/deployment/custom", method = RequestMethod.POST, consumes = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasAnyAuthority('ADMIN', 'COMPONENTS_MANAGER', 'ARCHITECT')")
    @Audit
    public RestResponse<Void> updateToCustomGit(@Valid @RequestBody UpdateDeploymentConfigGitConfig request) {
        checkEnvironmentAuthorization(request.getEnvironmentId());

        // path cannot leave the git root directory
        boolean incorrectPath = Paths.get(request.getPath()).normalize().startsWith("..");
        if (incorrectPath) {
            throw new IllegalStateException("Incorrect path <" + request.getPath() + ">");
        }

        String protocol = StringUtils.substringBefore(request.getUrl(), "://");
        if (StringUtils.isBlank(protocol)) {
            throw new IllegalStateException("Incorrect URL. Missing protocol <" + request.getUrl() + ">");
        }
        // file:// protocol is not allowed for security reason and only supported for alien managed repository
        if (protocol.startsWith("file")) {
            throw new IllegalStateException("Protocol <" + protocol + "> is not allowed");
        }

        String id = GitLocation.IdBuilder.DeploymentConfig.build(request.getEnvironmentId());
        GitLocation gitLocation = new GitLocation();
        gitLocation.setId(id);
        gitLocation.setGitType(GitLocation.GitType.DeploymentConfig);
        gitLocation.setBranch(request.getBranch());
        gitLocation.setCredential(new GitHardcodedCredential(request.getUsername(), request.getPassword()));
        gitLocation.setPath(request.getPath());
        gitLocation.setUrl(request.getUrl());

        updateGitLocation(gitLocation);

        return RestResponseBuilder.<Void> builder().build();
    }

    @ApiOperation(value = "Update the git repository parameters for storing deployment config")
    @RequestMapping(value = "/deployment/managed/{environmentId}", method = RequestMethod.POST, consumes = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasAnyAuthority('ADMIN', 'COMPONENTS_MANAGER', 'ARCHITECT')")
    @Audit
    public RestResponse<Void> updateToAlienManaged(@Valid @PathVariable String environmentId) {
        checkEnvironmentAuthorization(environmentId);

        GitLocation managedGit = alienManagedGitLocationBuilder.forDeploymentConfig(environmentId);
        updateGitLocation(managedGit);
        return RestResponseBuilder.<Void> builder().build();
    }

    @ApiOperation(value = "Retrieve information about a git repository using environment Id.")
    @RequestMapping(value = "/deployment/environment/{environmentId}", method = RequestMethod.GET)
    @PreAuthorize("hasAnyAuthority('ADMIN', 'COMPONENTS_MANAGER', 'ARCHITECT')")
    public RestResponse<GitLocation> getByEnvironmentId(@ApiParam(value = "Environment id", required = true) @PathVariable String environmentId) {
        checkEnvironmentAuthorization(environmentId);

        GitLocation gitLocation = gitLocationDao.forDeploymentConfig.findByEnvironmentId(environmentId);
        if (gitLocation.isLocal()) {
            gitLocation.setUrl(GitLocation.LOCAL_PREFIX);
        }
        gitLocation.setCredential(new GitHardcodedCredential());
        return RestResponseBuilder.<GitLocation> builder().data(gitLocation).build();
    }

    private void checkEnvironmentAuthorization(String environmentId) {
        ApplicationEnvironment environment = environmentService.getOrFail(environmentId);
        Application application = applicationService.getOrFail(environment.getApplicationId());
        AuthorizationUtil.checkAuthorizationForEnvironment(application, environment);
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
