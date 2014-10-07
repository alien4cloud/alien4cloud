package alien4cloud.security;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;

import alien4cloud.rest.model.RestErrorBuilder;
import alien4cloud.rest.model.RestErrorCode;
import alien4cloud.rest.model.RestResponse;
import alien4cloud.rest.model.RestResponseBuilder;

import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Custom Access Denied Handler to dispatch the correct error.
 * 
 * @author luc boutier
 */
@Component
public class Alien4CloudAccessDeniedHandler implements AccessDeniedHandler {

    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response, AccessDeniedException ade) throws IOException, ServletException {
        RestResponse<Void> restResponse = getUnauthorizedRestError();
        ObjectMapper mapper = new ObjectMapper();
        response.getWriter().write(mapper.writeValueAsString(restResponse));
        response.setStatus(HttpStatus.FORBIDDEN.value());
    }

    public RestResponse<Void> getUnauthorizedRestError() {
        return RestResponseBuilder.<Void> builder()
                .error(RestErrorBuilder.builder(RestErrorCode.UNAUTHORIZED_ERROR).message("Current user has no sufficient rights.").build()).build();

    }
}