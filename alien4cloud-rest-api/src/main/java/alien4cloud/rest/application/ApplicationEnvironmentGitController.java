package alien4cloud.rest.application;

import javax.inject.Inject;
import javax.validation.Valid;

import org.alien4cloud.git.GitLocationDao;
import org.alien4cloud.git.GitLocationService;
import org.alien4cloud.git.model.GitHardcodedCredential;
import org.alien4cloud.git.model.GitLocation;
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
import alien4cloud.exception.NotFoundException;
import alien4cloud.model.application.Application;
import alien4cloud.model.application.ApplicationEnvironment;
import alien4cloud.rest.application.model.UpdateGitLocationRequest;
import alien4cloud.rest.model.RestResponse;
import alien4cloud.rest.model.RestResponseBuilder;
import alien4cloud.security.AuthorizationUtil;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;

@RestController
@RequestMapping({ "/rest/applications/{applicationId:.+}/environments/{environmentId}/git",
        "/rest/v1/applications/{applicationId:.+}/environments/{environmentId}/git",
        "/rest/latest/applications/{applicationId:.+}/environments/{environmentId}/git" })
@Api(value = "", description = "Controller to retrieve and setup git configuration for environment deployment configuration.")
public class ApplicationEnvironmentGitController {
    @Inject
    private ApplicationService applicationService;
    @Inject
    private ApplicationEnvironmentService environmentService;
    @Inject
    private GitLocationService gitLocationService;
    @Inject
    private GitLocationDao gitLocationDao;

    @ApiOperation(value = "Update the remote git repository parameters for storing environment deployment config")
    @RequestMapping(method = RequestMethod.POST, consumes = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("isAuthenticated()")
    @Audit
    public RestResponse<Void> updateToCustomGit(@ApiParam(value = "Application id", required = true) @PathVariable String applicationId,
            @ApiParam(value = "Environment id", required = true) @PathVariable String environmentId, @Valid @RequestBody UpdateGitLocationRequest request) {
        checkEnvironmentAuthorization(applicationId, environmentId);

        String id = GitLocation.IdBuilder.forDeploymentSetup(applicationId, environmentId);
        GitLocation gitLocation = GitLocation.builder().id(id).gitType(GitLocation.GitType.DeploymentConfig).branch(request.getBranch())
                .credential(new GitHardcodedCredential(request.getUsername(), request.getPassword())).path(request.getPath()).url(request.getUrl()).build();

        gitLocationService.updateToRemoteGit(gitLocation);

        return RestResponseBuilder.<Void> builder().build();
    }

    @ApiOperation(value = "Update the git repository parameters for storing deployment config")
    @RequestMapping(method = RequestMethod.DELETE)
    @PreAuthorize("isAuthenticated()")
    @Audit
    public RestResponse<Void> updateToAlienManaged(@ApiParam(value = "Application id", required = true) @PathVariable String applicationId,
            @ApiParam(value = "Environment id", required = true) @PathVariable String environmentId) {
        checkEnvironmentAuthorization(applicationId, environmentId);

        gitLocationService.resetDeploymentSetupToLocalGit(applicationId, environmentId);

        return RestResponseBuilder.<Void> builder().build();
    }

    @ApiOperation(value = "Retrieve information about a git repository using environment Id.")
    @RequestMapping(method = RequestMethod.GET)
    @PreAuthorize("isAuthenticated()")
    public RestResponse<GitLocation> getByEnvironmentId(@ApiParam(value = "Application id", required = true) @PathVariable String applicationId,
            @ApiParam(value = "Environment id", required = true) @PathVariable String environmentId) {
        checkEnvironmentAuthorization(applicationId, environmentId);

        GitLocation gitLocation = gitLocationDao.findDeploymentSetupLocation(applicationId, environmentId);
        if (gitLocation.isLocal()) {
            gitLocation.setUrl(GitLocation.LOCAL_PREFIX);
        }
        gitLocation.setCredential(new GitHardcodedCredential());
        return RestResponseBuilder.<GitLocation> builder().data(gitLocation).build();
    }

    private void checkEnvironmentAuthorization(String applicationId, String environmentId) {
        ApplicationEnvironment environment = environmentService.getOrFail(environmentId);
        if (!environment.getApplicationId().equals(applicationId)) {
            throw new NotFoundException("Cannot find environement <" + environmentId + "> for application <" + applicationId + ">.");
        }
        Application application = applicationService.getOrFail(applicationId);
        AuthorizationUtil.checkAuthorizationForEnvironment(application, environment);
    }
}