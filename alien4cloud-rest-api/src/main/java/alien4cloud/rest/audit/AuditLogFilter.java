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
import org.springframework.util.StreamUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import alien4cloud.audit.model.AuditTrace;
import alien4cloud.security.AuthorizationUtil;
import alien4cloud.security.User;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.google.common.base.Charsets;

@Component
public class AuditLogFilter extends OncePerRequestFilter implements Ordered {

    @Resource
    private AuditLogRepository logRepository;

    @Override
    public int getOrder() {
        return Ordered.LOWEST_PRECEDENCE - 10;
    }

    /**
     * Prepare the trace you want to log
     * 
     * @param request
     * @param response
     * @return
     * @throws IOException
     * @throws JsonProcessingException
     */
    private AuditTrace getAuditTrace(HttpServletRequest request, HttpServletResponse response) throws JsonProcessingException, IOException {
        // user details
        User user = AuthorizationUtil.getCurrentUser();
        if (user == null) {
            return null;
        }
        // trace user info only when he is logged
        AuditTrace auditTrace = new AuditTrace();
        auditTrace.setUserName(user.getUsername());
        auditTrace.setUserFirstName(user.getFirstName());
        auditTrace.setUserLastName(user.getLastName());
        auditTrace.setUserEmail(user.getEmail());

        // request details
        auditTrace.setMethod(request.getMethod());
        auditTrace.setPath(request.getRequestURI());

        // response details
        auditTrace.setResponseStatus(response.getStatus());

        String contentType = request.getHeader(HttpHeaders.CONTENT_TYPE);
        if (contentType != null && contentType.startsWith(MediaType.APPLICATION_JSON_VALUE)) {
            auditTrace.setRequestBody(StreamUtils.copyToString(request.getInputStream(), Charsets.UTF_8));
        }
        auditTrace.setRequestParameters(request.getParameterMap());
        return auditTrace;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
        try {
            filterChain.doFilter(request, response);
        } finally {
            AuditTrace auditTrace = getAuditTrace(request, response);
            // username exists only if it's real service (not css, js, html files)
            if (auditTrace != null) {
                this.logRepository.add(auditTrace);
            }
        }
    }
}
