package alien4cloud.rest.application;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

import javax.inject.Inject;

import org.alien4cloud.tosca.variable.QuickFileStorageService;
import org.alien4cloud.tosca.variable.model.Variable;
import org.alien4cloud.tosca.variable.service.VariableDefinitionService;
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
import alien4cloud.security.model.ApplicationRole;
import lombok.extern.slf4j.Slf4j;
import springfox.documentation.annotations.ApiIgnore;

/**
 * Controller to support upload of the
 */
@Slf4j
@RestController
@RequestMapping({ "/rest/applications/{applicationId:.+}/variables", "/rest/v1/applications/{applicationId:.+}/variables",
        "/rest/latest/applications/{applicationId:.+}/variables" })
@ApiIgnore
public class ApplicationVariableController {
    @Inject
    private ApplicationService applicationService;
    @Inject
    private QuickFileStorageService quickFileStorageService;
    @Inject
    private VariableDefinitionService variableDefinitionService;

    @ApiIgnore
    @PreAuthorize("isAuthenticated()")
    @RequestMapping(method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    public RestResponse<String> getContent(@PathVariable String applicationId) {
        applicationService.checkAndGetApplication(applicationId, ApplicationRole.APPLICATION_MANAGER);
        return RestResponseBuilder.<String> builder().data(quickFileStorageService.getApplicationVariables(applicationId)).build();
    }

    @ApiIgnore
    @PreAuthorize("isAuthenticated()")
    @RequestMapping(value = "/{varName}", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    public RestResponse<Variable> getVariable(@PathVariable String applicationId, @PathVariable String varName) {
        return RestResponseBuilder.<Variable> builder().data(variableDefinitionService.getInApplicationScope(varName, applicationId)).build();
    }

    @ApiIgnore
    @PreAuthorize("isAuthenticated()")
    @RequestMapping(method = RequestMethod.POST, consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public RestResponse<Void> upload(@PathVariable String applicationId, @RequestBody UpdateVariableFileContentRequest request) throws IOException {
        applicationService.checkAndGetApplication(applicationId, ApplicationRole.APPLICATION_MANAGER);
        quickFileStorageService.saveApplicationVariables(applicationId, new ByteArrayInputStream(request.getContent().getBytes(StandardCharsets.UTF_8)));
        return new RestResponse<>();
    }
}