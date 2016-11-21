package alien4cloud.rest.application;

import javax.annotation.Resource;

import io.swagger.annotations.Api;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import alien4cloud.application.ApplicationService;
import alien4cloud.audit.annotation.Audit;
import alien4cloud.common.TagService;
import alien4cloud.model.application.Application;
import alien4cloud.rest.component.UpdateTagRequest;
import alien4cloud.rest.model.RestResponse;
import alien4cloud.rest.model.RestResponseBuilder;
import alien4cloud.security.model.ApplicationRole;
import alien4cloud.security.AuthorizationUtil;

import io.swagger.annotations.ApiOperation;

@RestController
@RequestMapping({"/rest/applications/{applicationId:.+}/tags", "/rest/v1/applications/{applicationId:.+}/tags", "/rest/latest/applications/{applicationId:.+}/tags"})
@Api(value = "", description = "Operations on application's tags")
public class ApplicationTagsController {
    @Resource
    private ApplicationService applicationService;
    @Resource
    private TagService tagService;

    /**
     * Update or create a tag for a given application
     * 
     * @param applicationId The id of the application for which to update/create a tag.
     * @param updateApplicationTagRequest The object that contains the tag's key and value.
     * @return An empty rest response.
     */
    @ApiOperation(value = "Update/Create a tag for the application.", notes = "The logged-in user must have the application manager role for this application. Application role required [ APPLICATION_MANAGER ]")
    @RequestMapping(method = RequestMethod.POST, consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("isAuthenticated()")
    @Audit
    public RestResponse<Void> upsertTag(@PathVariable String applicationId, @RequestBody UpdateTagRequest updateApplicationTagRequest) {
        Application application = applicationService.getOrFail(applicationId);
        AuthorizationUtil.checkAuthorizationForApplication(application, ApplicationRole.APPLICATION_MANAGER);

        tagService.upsertTag(application, updateApplicationTagRequest.getTagKey(), updateApplicationTagRequest.getTagValue());

        return RestResponseBuilder.<Void> builder().build();
    }

    /**
     * Delete a tag for the application.
     * 
     * @param applicationId Id of the application for which to remove the tag.
     * @param tagId The key of the tag to remove.
     * @return An empty {@link RestResponse}.
     */
    @ApiOperation(value = "Delete a tag for the application.", notes = "The logged-in user must have the application manager role for this application. Application role required [ APPLICATION_MANAGER ]")
    @RequestMapping(value = "/{tagId}", method = RequestMethod.DELETE, produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("isAuthenticated()")
    @Audit
    public RestResponse<Void> deleteTag(@PathVariable String applicationId, @PathVariable String tagId) {
        Application application = applicationService.getOrFail(applicationId);
        AuthorizationUtil.checkAuthorizationForApplication(application, ApplicationRole.APPLICATION_MANAGER);

        tagService.removeTag(application, tagId);

        return RestResponseBuilder.<Void> builder().build();
    }
}