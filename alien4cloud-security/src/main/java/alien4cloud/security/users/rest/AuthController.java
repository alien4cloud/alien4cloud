package alien4cloud.security.users.rest;

import java.util.List;

import javax.annotation.Resource;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.Environment;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.google.common.collect.Lists;

import alien4cloud.rest.model.RestErrorBuilder;
import alien4cloud.rest.model.RestErrorCode;
import alien4cloud.rest.model.RestResponse;
import alien4cloud.rest.model.RestResponseBuilder;
import alien4cloud.rest.model.UserStatus;
import alien4cloud.security.groups.IAlienGroupDao;
import alien4cloud.security.model.ApplicationEnvironmentRole;
import alien4cloud.security.model.ApplicationRole;
import alien4cloud.security.model.CloudRole;
import alien4cloud.security.model.DeployerRole;
import alien4cloud.security.model.Group;
import alien4cloud.security.model.Role;
import alien4cloud.security.model.User;
import alien4cloud.utils.AlienConstants;
import io.swagger.annotations.ApiOperation;
import springfox.documentation.annotations.ApiIgnore;

/**
 * Authentication service manages security related operations including Authentication and Authorization management.
 *
 * @author luc boutier
 */
@RestController
@RequestMapping({"/rest/auth", "/rest/v1/auth", "/rest/latest/auth"})
public class AuthController {
    @Resource
    private IAlienGroupDao alienGroupDao;
    @Value("${saml.enabled:false}")
    private boolean samlEnabled;
    @Resource
    private Environment env;

    /**
     * Get the current user's status (login, roles etc.).
     *
     * @return The current user's status wrapped in a {@link RestResponse} object.
     */
    @ApiOperation(value = "Get the current authentication status and user's roles.", notes = "Return the current user's status and it's roles.")
    @RequestMapping(value = "/status", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    public RestResponse<UserStatus> getLoginStatus() {
        final Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        final UserStatus userStatus = new UserStatus();

        if (auth == null) {
            userStatus.setIsLogged(false);
        } else {
            userStatus.setIsLogged(auth.isAuthenticated() && !(auth instanceof AnonymousAuthenticationToken));
            userStatus.setUsername(auth.getName());
            if (auth.getPrincipal() instanceof User) {
                userStatus.setGithubUsername(((User) auth.getPrincipal()).getFirstName());
                userStatus.setGroups(((User) auth.getPrincipal()).getGroups());
            }
            for (GrantedAuthority role : auth.getAuthorities()) {
                userStatus.getRoles().add(role.getAuthority());
            }
        }

        if (env.acceptsProfiles("github-auth")) {
            userStatus.setAuthSystem("github");
        } else if (samlEnabled) {
            userStatus.setAuthSystem("saml");
        } else {
            userStatus.setAuthSystem("alien");
        }

        return RestResponseBuilder.<UserStatus> builder().data(userStatus).build();
    }

    @ApiIgnore
    @RequestMapping(value = "/authenticationfailed", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseStatus(value = HttpStatus.UNAUTHORIZED)
    public RestResponse<Void> authenticationFailed() {
        return RestResponseBuilder.<Void> builder().error(
                RestErrorBuilder.builder(RestErrorCode.AUTHENTICATION_FAILED_ERROR).message("Authentication failed, check username and password.").build())
                .build();
    }

    @ApiIgnore
    @RequestMapping(value = "/roles", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    public RestResponse<Role[]> getAllRoles() {
        return RestResponseBuilder.<Role[]> builder().data(Role.values()).build();
    }

    @ApiIgnore
    @RequestMapping(value = "/roles/application", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    public RestResponse<ApplicationRole[]> getApplicationRoles() {
        // APPLICATION_USER is a technical role in ApplicationRole enum (do not return it)
        List<ApplicationRole> applicationRoleList = Lists.newArrayList(ApplicationRole.values());
        applicationRoleList.remove(ApplicationRole.APPLICATION_USER);
        return RestResponseBuilder.<ApplicationRole[]> builder().data(applicationRoleList.toArray(new ApplicationRole[applicationRoleList.size()])).build();
    }

    @ApiIgnore
    @RequestMapping(value = "/roles/environment", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    public RestResponse<ApplicationEnvironmentRole[]> getApplicationEnvironmentRoles() {
        return RestResponseBuilder.<ApplicationEnvironmentRole[]> builder().data(ApplicationEnvironmentRole.values()).build();
    }

    @ApiIgnore
    @RequestMapping(value = "/roles/cloud", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    public RestResponse<CloudRole[]> getCloudRoles() {
        return RestResponseBuilder.<CloudRole[]> builder().data(CloudRole.values()).build();
    }

    @ApiIgnore
    @RequestMapping(value = "/roles/deployer", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    public RestResponse<DeployerRole[]> getLocationRoles() {
        return RestResponseBuilder.<DeployerRole[]> builder().data(DeployerRole.values()).build();
    }

    @ApiIgnore
    @RequestMapping(value = "/groups/allusers", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    public RestResponse<Group> getAllUsersGroup() {
        Group group = alienGroupDao.findByName(AlienConstants.GROUP_NAME_ALL_USERS);
        return RestResponseBuilder.<Group> builder().data(group).build();
    }

}
