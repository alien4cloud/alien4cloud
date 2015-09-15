package alien4cloud.rest.application;

import lombok.extern.slf4j.Slf4j;

import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import alien4cloud.rest.model.RestResponse;
import alien4cloud.rest.model.RestResponseBuilder;

import com.wordnik.swagger.annotations.Api;
import com.wordnik.swagger.annotations.ApiOperation;

@Slf4j
@RestController
@RequestMapping("/rest/applications/{appId}/deployment")
@Api(value = "", description = "Manage configuration of an application before deploying it.")
public class ApplicationDeploymentConfigurationController {

    /**
     * set the location for a deployment
     *
     * @param appId application Id
     * @param configRequest {@link ApplicationDeploymentConfigRequest} object: deployment configuration
     * @return
     */
    @ApiOperation(value = "Get active deployment for the given application on the given cloud.", notes = "Application role required [ APPLICATION_MANAGER | APPLICATION_DEVOPS ] and Application environment role required [ DEPLOYMENT_MANAGER ]")
    @RequestMapping(value = "/configure", method = RequestMethod.PUT, consumes = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("isAuthenticated()")
    public RestResponse<Void> setLocation(@PathVariable String appId, @RequestBody ApplicationDeploymentConfigRequest configRequest) {

        // TODO populate the configRequest
        // takes as param and return the TopologyDeployment instead of Void ??

        return RestResponseBuilder.<Void> builder().build();
    }

}
