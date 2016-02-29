package alien4cloud.rest.internal;

import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import alien4cloud.model.application.EnvironmentType;
import alien4cloud.rest.model.RestResponse;
import alien4cloud.rest.model.RestResponseBuilder;

import springfox.documentation.annotations.ApiIgnore;

/**
 * Handle enum to list services
 *
 * @author mourouvi
 *
 */
@RestController
@RequestMapping({"/rest/enums", "/rest/v1/enums", "/rest/latest/enums"})
public class MiscEnumResourcesController {
    @ApiIgnore
    @RequestMapping(value = "/environmenttype", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("isAuthenticated()")
    public RestResponse<EnvironmentType[]> getEnvironmentTypes() {
        return RestResponseBuilder.<EnvironmentType[]> builder().data(EnvironmentType.values()).build();
    }
}
