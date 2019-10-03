package alien4cloud.security.users.rest;

import alien4cloud.rest.model.*;
import alien4cloud.security.users.JwtTokenService;
import alien4cloud.security.spring.Alien4CloudAuthenticationProvider;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.AuthenticationServiceException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import java.util.Collection;

/**
 * A endpoint to authenticate and that returns a JWT token.
 */
@RestController
@RequestMapping({"/rest/jwtauth"})
public class JwtAuthController {

    @Resource
    private Alien4CloudAuthenticationProvider authenticationProvider;

    @Resource
    private JwtTokenService jwtTokenService;

    @RequestMapping(method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
    public RestResponse<JwtToken> authenticate(@RequestParam("user") String user, @RequestParam("password") String password) {
        Authentication authentication = authenticationProvider.authenticate(new Authentication() {
            @Override
            public Collection<? extends GrantedAuthority> getAuthorities() {
                return null;
            }

            @Override
            public Object getCredentials() {
                return password;
            }

            @Override
            public Object getDetails() {
                return null;
            }

            @Override
            public Object getPrincipal() {
                return null;
            }

            @Override
            public boolean isAuthenticated() {
                return false;
            }

            @Override
            public void setAuthenticated(boolean b) throws IllegalArgumentException {

            }

            @Override
            public String getName() {
                return user;
            }
        });

        if(authentication != null && authentication.isAuthenticated()) {
            JwtToken token = jwtTokenService.createTokens(authentication);
            return RestResponseBuilder.<JwtToken> builder().data(token).build();
        }

        throw new AuthenticationServiceException("Not able to authenticate using JWT");
    }

}
