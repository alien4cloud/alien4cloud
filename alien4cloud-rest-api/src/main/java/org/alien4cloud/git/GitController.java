package org.alien4cloud.git;

import javax.inject.Inject;
import javax.validation.Valid;

import org.alien4cloud.git.model.GitHardcodedCredential;
import org.alien4cloud.git.model.GitLocation;
import org.apache.commons.lang3.StringUtils;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import alien4cloud.application.ApplicationEnvironmentService;
import alien4cloud.application.ApplicationService;
import alien4cloud.audit.annotation.Audit;
import alien4cloud.model.application.Application;
import alien4cloud.model.application.ApplicationEnvironment;
import alien4cloud.rest.model.RestResponse;
import alien4cloud.rest.model.RestResponseBuilder;
import alien4cloud.security.AuthorizationUtil;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;

import java.nio.file.Paths;

@RestController
@RequestMapping(value = {"/rest/git", "/rest/v1/git", "/rest/latest/git"})
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
        if(StringUtils.isBlank(protocol)){
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

        return RestResponseBuilder.<Void>builder().build();
    }

    @ApiOperation(value = "Update the git repository parameters for storing deployment config")
    @RequestMapping(value = "/deployment/managed/{environmentId}", method = RequestMethod.POST, consumes = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasAnyAuthority('ADMIN', 'COMPONENTS_MANAGER', 'ARCHITECT')")
    @Audit
    public RestResponse<Void> updateToAlienManaged(@Valid @PathVariable String environmentId) {
        checkEnvironmentAuthorization(environmentId);

        GitLocation managedGit = alienManagedGitLocationBuilder.forDeploymentConfig(environmentId);
        updateGitLocation(managedGit);
        return RestResponseBuilder.<Void>builder().build();
    }

    @ApiOperation(value = "Retrieve information about a git repository using environment Id.")
    @RequestMapping(value = "/deployment/environment/{environmentId}", method = RequestMethod.GET)
    @PreAuthorize("hasAnyAuthority('ADMIN', 'COMPONENTS_MANAGER', 'ARCHITECT')")
    public RestResponse<GitLocation> getByEnvironmentId(
            @ApiParam(value = "Environment id", required = true) @PathVariable String environmentId) {
        checkEnvironmentAuthorization(environmentId);

        GitLocation gitLocation = gitLocationDao.forDeploymentConfig.findByEnvironmentId(environmentId);
        return RestResponseBuilder.<GitLocation>builder().data(gitLocation).build();
    }

    private void checkEnvironmentAuthorization(String environmentId) {
        ApplicationEnvironment environment = environmentService.getOrFail(environmentId);
        Application application = applicationService.getOrFail(environment.getApplicationId());
        AuthorizationUtil.checkAuthorizationForEnvironment(application, environment);
    }

    private void updateGitLocation(GitLocation newGitLocation) {
        // delete previous
        GitLocation previousLocation = gitLocationDao.findById(newGitLocation.getId());
        if(previousLocation != null) {
            localGitManager.deleteLocalGit(previousLocation);
        }

        // create the new one
        localGitManager.createLocalGitIfNeeded(newGitLocation);
        gitLocationDao.save(newGitLocation);
    }
}
