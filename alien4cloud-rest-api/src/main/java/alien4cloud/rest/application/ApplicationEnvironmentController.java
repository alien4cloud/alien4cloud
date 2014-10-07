package alien4cloud.rest.application;

import javax.annotation.Resource;

import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import alien4cloud.application.ApplicationEnvironmentService;
import alien4cloud.dao.IGenericSearchDAO;
import alien4cloud.model.application.Application;
import alien4cloud.model.application.ApplicationEnvironment;
import alien4cloud.rest.model.RestResponse;
import alien4cloud.rest.model.RestResponseBuilder;
import alien4cloud.security.ApplicationRole;
import alien4cloud.security.AuthorizationUtil;

import com.wordnik.swagger.annotations.Api;
import com.wordnik.swagger.annotations.ApiOperation;

@RestController
@RequestMapping("/rest/applications/{applicationId:.+}/environments")
@Api(value = "", description = "Manages application's environments")
public class ApplicationEnvironmentController {
    @Resource(name = "alien-es-dao")
    private IGenericSearchDAO alienDAO;
    @Resource
    private ApplicationEnvironmentService applicationEnvironmentService;

    /**
     * Get the single application environment from the application id.
     *
     * @param applicationId The application id.
     */
    @ApiOperation(value = "Get an application based from its id.", notes = "Returns the application details. Application role required [ APPLICATION_MANAGER | APPLICATION_USER | APPLICATION_DEVOPS | DEPLOYMENT_MANAGER ]")
    @RequestMapping(method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    public RestResponse<ApplicationEnvironment> get(@PathVariable String applicationId) {
        Application application = alienDAO.findById(Application.class, applicationId);
        AuthorizationUtil.checkAuthorizationForApplication(application, ApplicationRole.values());

        ApplicationEnvironment[] environments = applicationEnvironmentService.getByApplicationId(applicationId);
        return RestResponseBuilder.<ApplicationEnvironment> builder().data(environments[0]).build();
    }
}
