package alien4cloud.rest.security;

import javax.annotation.Resource;

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

import alien4cloud.Constants;
import alien4cloud.rest.model.RestErrorBuilder;
import alien4cloud.rest.model.RestErrorCode;
import alien4cloud.rest.model.RestResponse;
import alien4cloud.rest.model.RestResponseBuilder;
import alien4cloud.rest.model.UserStatus;
import alien4cloud.security.ApplicationRole;
import alien4cloud.security.CloudRole;
import alien4cloud.security.Role;
import alien4cloud.security.User;
import alien4cloud.security.groups.Group;
import alien4cloud.security.groups.IAlienGroupDao;

import com.mangofactory.swagger.annotations.ApiIgnore;
import com.wordnik.swagger.annotations.ApiOperation;

/**
 * Authentication service manages security related operations including Authentication and Authorization management.
 * 
 * @author luc boutier
 */
@RestController
@RequestMapping("/rest/auth")
public class AuthController {

    @Resource
    private IAlienGroupDao alienGroupDao;

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
                userStatus.setGroups(((User) auth.getPrincipal()).getGroups());
            }
            for (GrantedAuthority role : auth.getAuthorities()) {
                userStatus.getRoles().add(role.getAuthority());
            }
        }

        return RestResponseBuilder.<UserStatus> builder().data(userStatus).build();
    }

    @ApiIgnore
    @RequestMapping(value = "/authenticationrequired", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseStatus(value = HttpStatus.UNAUTHORIZED)
    public RestResponse<Void> authenticationRequired() {
        return RestResponseBuilder.<Void> builder()
                .error(RestErrorBuilder.builder(RestErrorCode.AUTHENTICATION_REQUIRED_ERROR).message("Authentication is required.").build()).build();
    }

    @ApiIgnore
    @RequestMapping(value = "/authenticationfailed", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseStatus(value = HttpStatus.UNAUTHORIZED)
    public RestResponse<Void> authenticationFailed() {
        return RestResponseBuilder
                .<Void> builder()
                .error(RestErrorBuilder.builder(RestErrorCode.AUTHENTICATION_FAILED_ERROR).message("Authentication failed, check username and password.")
                        .build()).build();
    }

    @ApiIgnore
    @RequestMapping(value = "/roles", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    public RestResponse<Role[]> getAllRoles() {
        return RestResponseBuilder.<Role[]> builder().data(Role.values()).build();
    }

    @ApiIgnore
    @RequestMapping(value = "/roles/application", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    public RestResponse<ApplicationRole[]> getApplicationRoles() {
        return RestResponseBuilder.<ApplicationRole[]> builder().data(ApplicationRole.values()).build();
    }

    @ApiIgnore
    @RequestMapping(value = "/roles/cloud", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    public RestResponse<CloudRole[]> getCloudRoles() {
        return RestResponseBuilder.<CloudRole[]> builder().data(CloudRole.values()).build();
    }

    @ApiIgnore
    @RequestMapping(value = "/groups/allusers", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    public RestResponse<Group> getAllUsersGroup() {
        Group group = alienGroupDao.findByName(Constants.GROUP_NAME_ALL_USERS);
        return RestResponseBuilder.<Group> builder().data(group).build();
    }

}