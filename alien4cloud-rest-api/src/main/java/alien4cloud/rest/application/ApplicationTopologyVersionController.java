package alien4cloud.rest.application;

import javax.inject.Inject;

import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import alien4cloud.application.ApplicationService;
import alien4cloud.application.ApplicationVersionService;
import alien4cloud.audit.annotation.Audit;
import alien4cloud.model.application.Application;
import alien4cloud.rest.application.model.CreateApplicationTopologyVersionRequest;
import alien4cloud.rest.model.RestResponse;
import alien4cloud.rest.model.RestResponseBuilder;
import alien4cloud.security.AuthorizationUtil;
import alien4cloud.security.model.ApplicationRole;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;

@RestController
@RequestMapping({ "/rest/applications/{applicationId:.+}/versions/{applicationVersionId:.+}/topologyVersions",
        "/rest/v1/applications/{applicationId:.+}/versions/{applicationVersionId:.+}/topologyVersions",
        "/rest/latest/applications/{applicationId:.+}/versions/{applicationVersionId:.+}/topologyVersions" })
@Api(value = "", description = "Manages application topology versions for a given application version")
public class ApplicationTopologyVersionController {
    @Inject
    private ApplicationService applicationService;
    @Inject
    private ApplicationVersionService applicationVersionService;

    @ApiOperation(value = "Create a new application topology version", notes = "The logged-in user must have the application manager role for this application. Application role required [ APPLICATION_MANAGER ]")
    @RequestMapping(method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("isAuthenticated()")
    @Audit
    public RestResponse<Void> create(@PathVariable String applicationId, @PathVariable String applicationVersionId,
            @RequestBody CreateApplicationTopologyVersionRequest request) {
        Application application = applicationService.getOrFail(applicationId);
        AuthorizationUtil.checkAuthorizationForApplication(application, ApplicationRole.APPLICATION_MANAGER);
        String originalId = request.getTopologyTemplateId();
        boolean originalIsAppVersion = false;
        if (originalId == null) {
            originalId = request.getApplicationTopologyVersion();
            originalIsAppVersion = true;
        } else if (request.getApplicationTopologyVersion() != null) {
            throw new IllegalArgumentException("topologyTemplateId and applicationTopologyVersion are mutually exclusive.");
        }

        String qualifier = request.getQualifier() == null ? null : request.getQualifier().trim();
        applicationVersionService.createTopologyVersion(applicationId, applicationVersionId, qualifier, request.getDescription(), originalId,
                originalIsAppVersion);

        return RestResponseBuilder.<Void> builder().build();
    }

    @ApiOperation(value = "Delete an application topology version from its id", notes = "The logged-in user must have the application manager role for this application. Application role required [ APPLICATION_MANAGER ]")
    @RequestMapping(value = "/{topologyVersion:.+}", method = RequestMethod.DELETE, produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("isAuthenticated()")
    @Audit
    public RestResponse<Void> delete(@PathVariable String applicationId, @PathVariable String applicationVersionId, @PathVariable String topologyVersion) {
        Application application = applicationService.getOrFail(applicationId);
        AuthorizationUtil.checkAuthorizationForApplication(application, ApplicationRole.APPLICATION_MANAGER);

        applicationVersionService.deleteTopologyVersion(applicationId, applicationVersionId, topologyVersion);
        return RestResponseBuilder.<Void> builder().build();
    }
}