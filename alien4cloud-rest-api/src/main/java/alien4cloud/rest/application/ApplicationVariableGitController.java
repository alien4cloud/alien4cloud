package alien4cloud.rest.application;

import javax.inject.Inject;
import javax.validation.Valid;

import org.alien4cloud.git.GitLocationDao;
import org.alien4cloud.git.GitLocationService;
import org.alien4cloud.git.model.GitHardcodedCredential;
import org.alien4cloud.git.model.GitLocation;
import org.alien4cloud.git.model.GitLocation.GitType;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import alien4cloud.application.ApplicationService;
import alien4cloud.audit.annotation.Audit;
import alien4cloud.rest.application.model.UpdateGitLocationRequest;
import alien4cloud.rest.model.RestResponse;
import alien4cloud.rest.model.RestResponseBuilder;
import alien4cloud.security.model.ApplicationRole;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;

@RestController
@RequestMapping({ "/rest/applications/{applicationId:.+}/variables/git", "/rest/v1/applications/{applicationId:.+}/variables/git",
        "/rest/latest/applications/{applicationId:.+}/variables/git" })
@Api(value = "", description = "Controller to retrieve and setup git configuration for application variables.")
public class ApplicationVariableGitController {
    @Inject
    private ApplicationService applicationService;
    @Inject
    private GitLocationService gitLocationService;
    @Inject
    private GitLocationDao gitLocationDao;

    @ApiOperation(value = "Update the remote git repository parameters for storing application variables.")
    @RequestMapping(method = RequestMethod.POST, consumes = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("isAuthenticated()")
    @Audit
    public RestResponse<Void> updateToCustomGit(@ApiParam(value = "Application id", required = true) @PathVariable String applicationId,
            @Valid @RequestBody UpdateGitLocationRequest request) {
        applicationService.checkAndGetApplication(applicationId, ApplicationRole.APPLICATION_MANAGER);

        String id = GitLocation.IdBuilder.forApplicationVariables(applicationId);
        GitLocation gitLocation = GitLocation.builder().id(id).gitType(GitType.ApplicationVariables).branch(request.getBranch())
                .credential(new GitHardcodedCredential(request.getUsername(), request.getPassword())).path(request.getPath()).url(request.getUrl()).build();

        gitLocationService.updateToRemoteGit(gitLocation);

        return RestResponseBuilder.<Void> builder().build();
    }

    @ApiOperation(value = "Update the git repository parameters for storing deployment config")
    @RequestMapping(method = RequestMethod.DELETE)
    @PreAuthorize("isAuthenticated()")
    @Audit
    public RestResponse<Void> updateToAlienManaged(@ApiParam(value = "Application id", required = true) @PathVariable String applicationId) {
        applicationService.checkAndGetApplication(applicationId, ApplicationRole.APPLICATION_MANAGER);

        gitLocationService.resetApplicationVariablesToLocalGit(applicationId);

        return RestResponseBuilder.<Void> builder().build();
    }

    @ApiOperation(value = "Retrieve information about a git repository using environment Id.")
    @RequestMapping(method = RequestMethod.GET)
    @PreAuthorize("isAuthenticated()")
    public RestResponse<GitLocation> getByEnvironmentId(@ApiParam(value = "Application id", required = true) @PathVariable String applicationId) {
        applicationService.checkAndGetApplication(applicationId, ApplicationRole.APPLICATION_MANAGER);

        GitLocation gitLocation = gitLocationDao.findApplicationVariablesLocation(applicationId);
        if (gitLocation.isLocal()) {
            gitLocation.setUrl(GitLocation.LOCAL_PREFIX);
        }
        gitLocation.setCredential(new GitHardcodedCredential());
        return RestResponseBuilder.<GitLocation> builder().data(gitLocation).build();
    }
}