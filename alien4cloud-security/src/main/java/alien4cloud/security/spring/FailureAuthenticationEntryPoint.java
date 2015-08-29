package alien4cloud.security.spring;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import alien4cloud.rest.model.RestResponse;
import lombok.extern.slf4j.Slf4j;

import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;

import alien4cloud.rest.model.RestErrorBuilder;
import alien4cloud.rest.model.RestErrorCode;
import alien4cloud.rest.model.RestResponseBuilder;
import alien4cloud.rest.utils.JsonUtil;

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
        log.debug("Authentication required for this request : {}", request.getRequestURL());
        response.setContentType("application/json");
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        // return a RestResponse json in the response
        response.getOutputStream().println(JsonUtil.toString(getAuthenticationRequired(request.getRequestURI())));
    }

    public static RestResponse<Void> getAuthenticationRequired(String url) {
        return RestResponseBuilder
                .<Void> builder()
                .data(null)
                .error(RestErrorBuilder.builder(RestErrorCode.AUTHENTICATION_REQUIRED_ERROR).message("Authentication required for this request : " + url)
                        .build()).build();
    }
}
