package alien4cloud.security;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import lombok.extern.slf4j.Slf4j;

import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;

@Slf4j
public class MyAuthenticationEntryPoint implements AuthenticationEntryPoint {

    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response, AuthenticationException authException) throws IOException, ServletException {

        log.info("+++++++++++++++++++++++++++++++++++++++++++++++++ LOGIN execption, {}", response.isCommitted());
        // if ( response.) {
        //
        // }
        // throw new AccessDeniedException("API ENTRE ERROR");

    }
}
