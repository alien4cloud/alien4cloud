package alien4cloud.security.users.rest;

import alien4cloud.security.users.JwtTokenService;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.Jws;
import io.jsonwebtoken.JwtException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.GenericFilterBean;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Optional;

/**
 * A filter that checks if a JWT authentication token is present in the header. If found, it's verified.
 */
@Component
public class JwtAuthenticationTokenFilter extends GenericFilterBean {

    private static final String BEARER = "Bearer";

    @Autowired
    private JwtTokenService jwtTokenService;

    @Override
    public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse, FilterChain chain) throws IOException, ServletException {

        final HttpServletRequest request = (HttpServletRequest) servletRequest;
        final HttpServletResponse response = (HttpServletResponse) servletResponse;

        // Assume we have only one Authorization header value
        final Optional<String> token = Optional.ofNullable(request.getHeader(HttpHeaders.AUTHORIZATION));

        Authentication authentication;
        boolean securityContextSet = false;
        if(token.isPresent() && token.get().startsWith(BEARER)) {
            String bearerToken = token.get().substring(BEARER.length()+1);

            try {
                Jws<Claims> claims = jwtTokenService.validateJwtToken(bearerToken);
                authentication = jwtTokenService.buildAuthenticationFromClaim(claims);
                SecurityContextHolder.getContext().setAuthentication(authentication);
                securityContextSet = true;
            } catch (ExpiredJwtException exception) {
                response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "error.jwt.expired");
                return;
            } catch (JwtException exception) {
                response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "error.jwt.invalid");
                return;
            }

        }

        chain.doFilter(servletRequest, servletResponse);
        if (securityContextSet) {
            SecurityContextHolder.getContext().setAuthentication(null);
        }

    }


}
