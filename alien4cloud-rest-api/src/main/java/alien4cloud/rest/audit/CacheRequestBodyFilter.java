package alien4cloud.rest.audit;

import java.io.IOException;

import javax.annotation.Resource;
import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.core.Ordered;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import alien4cloud.security.AuthorizationUtil;
import alien4cloud.security.User;

@Component
public class CacheRequestBodyFilter extends OncePerRequestFilter implements Ordered {

    @Resource
    private AuditLogFilter auditLogFilter;

    @Override
    public int getOrder() {
        return auditLogFilter.getOrder() - 1;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
        User user = AuthorizationUtil.getCurrentUser();
        String contentType = request.getHeader(HttpHeaders.CONTENT_TYPE);
        // only cache if the content is JSON
        if (user != null && contentType != null && contentType.startsWith(MediaType.APPLICATION_JSON_VALUE)) {
            MultiReadHttpServletRequest multiReadRequest = new MultiReadHttpServletRequest(request);
            filterChain.doFilter(multiReadRequest, response);
        } else {
            filterChain.doFilter(request, response);
        }
    }
}
