package alien4cloud.security;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import lombok.extern.slf4j.Slf4j;

import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;

import alien4cloud.rest.model.RestErrorBuilder;
import alien4cloud.rest.model.RestErrorCode;
import alien4cloud.rest.model.RestResponseBuilder;

/**
 * Triggered when the client makes a call to a resource without being authenticated
 * (client not logged in)
 * 
 * @author mourouvi
 *
 */
@Slf4j
public class FailureAuthenticationEntryPoint implements AuthenticationEntryPoint {
    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response, AuthenticationException authException) throws IOException, ServletException {
        log.info("Authentication required for this request : {}", request.getRequestURL());

        RestResponseBuilder.<Void> builder()
                .error(RestErrorBuilder.builder(RestErrorCode.UNAUTHORIZED_ERROR).message("Current user has no sufficient rights.").build()).build();
        response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Authentication required");
    }
}
