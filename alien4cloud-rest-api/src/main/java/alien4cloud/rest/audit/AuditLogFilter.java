package alien4cloud.rest.audit;

import java.io.IOException;

import javax.annotation.Resource;
import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.core.Ordered;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import alien4cloud.rest.audit.model.AuditTrace;
import alien4cloud.security.AuthorizationUtil;
import alien4cloud.security.User;

@Component
public class AuditLogFilter extends OncePerRequestFilter implements Ordered {

    @Resource
    private AuditLogRepository logRepository;

    @Override
    public int getOrder() {
        return Ordered.LOWEST_PRECEDENCE - 10;
    }

    private AuditTrace getAuditTrace(HttpServletRequest request, HttpServletResponse response) {
        AuditTrace auditTrace = new AuditTrace();
        auditTrace.setMethod(request.getMethod());
        auditTrace.setPath(request.getRequestURI());
        User user = AuthorizationUtil.getCurrentUser();
        return auditTrace;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {

        try {
            filterChain.doFilter(request, response);
        } finally {
            this.logRepository.add(getAuditTrace(request, response));
        }
    }
}
