package alien4cloud.rest.application;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;

import javax.inject.Inject;

import org.alien4cloud.git.GitLocationDao;
import org.alien4cloud.git.LocalGitManager;
import org.alien4cloud.git.model.GitLocation;
import org.alien4cloud.tosca.variable.service.QuickFileStorageService;
import org.alien4cloud.tosca.variable.model.Variable;
import org.alien4cloud.tosca.variable.service.VariableExpressionService;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import alien4cloud.application.ApplicationService;
import alien4cloud.rest.application.model.UpdateVariableFileContentRequest;
import alien4cloud.rest.model.RestResponse;
import alien4cloud.rest.model.RestResponseBuilder;
import alien4cloud.security.AuthorizationUtil;
import alien4cloud.security.model.ApplicationRole;
import alien4cloud.security.model.User;
import lombok.extern.slf4j.Slf4j;
import springfox.documentation.annotations.ApiIgnore;

/**
 * Controller to support upload of the
 */
@Slf4j
@RestController
@RequestMapping({ "/rest/applications/{applicationId:.+}/variables", "/rest/v1/applications/{applicationId:.+}/variables",
        "/rest/latest/applications/{applicationId:.+}/variables" })
public class ApplicationVariableController {
    @Inject
    private ApplicationService applicationService;
    @Inject
    private QuickFileStorageService quickFileStorageService;
    @Inject
    private VariableExpressionService variableExpressionService;
    @Inject
    private LocalGitManager localGitManager;
    @Inject
    private GitLocationDao gitLocationDao;

    @PreAuthorize("isAuthenticated()")
    @RequestMapping(method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    public RestResponse<String> getContent(@PathVariable String applicationId) {
        applicationService.checkAndGetApplication(applicationId, ApplicationRole.APPLICATION_MANAGER);

        GitLocation gitLocation = gitLocationDao.findApplicationVariablesLocation(applicationId);
        localGitManager.checkout(gitLocation);

        return RestResponseBuilder.<String> builder().data(quickFileStorageService.getApplicationVariables(applicationId)).build();
    }

    @PreAuthorize("isAuthenticated()")
    @RequestMapping(method = RequestMethod.POST, consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public RestResponse<Void> upload(@PathVariable String applicationId, @RequestBody UpdateVariableFileContentRequest request) {
        applicationService.checkAndGetApplication(applicationId, ApplicationRole.APPLICATION_MANAGER);

        GitLocation gitLocation = gitLocationDao.findApplicationVariablesLocation(applicationId);
        localGitManager.checkout(gitLocation);

        quickFileStorageService.saveApplicationVariables(applicationId, new ByteArrayInputStream(request.getContent().getBytes(StandardCharsets.UTF_8)));
        User user = AuthorizationUtil.getCurrentUser();
        localGitManager.commitAndPush(gitLocation, user.getUsername(), user.getEmail(), "Update application variables.");
        return new RestResponse<>();
    }

    @ApiIgnore
    @PreAuthorize("isAuthenticated()")
    @RequestMapping(value = "/{varName}", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    public RestResponse<Variable> getVariable(@PathVariable String applicationId, @PathVariable String varName) {
        applicationService.checkAndGetApplication(applicationId, ApplicationRole.APPLICATION_DEVOPS, ApplicationRole.APPLICATION_MANAGER);
        return RestResponseBuilder.<Variable> builder().data(variableExpressionService.getInApplicationScope(varName, applicationId)).build();
    }
}