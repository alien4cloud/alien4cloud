package alien4cloud.rest.audit;

import java.io.IOException;
import java.util.Map;

import javax.annotation.Resource;
import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.elasticsearch.common.collect.Maps;
import org.springframework.core.Ordered;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import alien4cloud.rest.audit.model.AuditTrace;
import alien4cloud.rest.utils.JsonUtil;
import alien4cloud.security.AuthorizationUtil;
import alien4cloud.security.User;

import com.fasterxml.jackson.core.JsonProcessingException;

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
        AuditTrace auditTrace = new AuditTrace();

        // request details
        auditTrace.setMethod(request.getMethod());
        auditTrace.setPath(request.getRequestURI());

        // user details
        User user = AuthorizationUtil.getCurrentUser();
        // trace user info only when he is logged
        if (user != null) {
            auditTrace.setUserName(user.getUsername());
            auditTrace.setUserFirstName(user.getFirstName());
            auditTrace.setUserLastName(user.getLastName());
            auditTrace.setUserEmail(user.getEmail());
        }

        // response details
        auditTrace.setResponseStatus(response.getStatus());
        // WIP : recover the body multi time
        auditTrace.setRequestBody(JsonUtil.toString(request.getInputStream().toString()));
        // auditTrace.setRequestParameters(getRequestParams(request));

        return auditTrace;
    }

    private Map<String, String> getRequestParams(HttpServletRequest request) {
        Map<String, String> parameters = Maps.newHashMap();
        while (request.getParameterNames().hasMoreElements()) {
            String paramterName = request.getParameterNames().nextElement();
            parameters.put(paramterName, request.getParameter(paramterName));
        }
        return parameters;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {

        try {
            filterChain.doFilter(request, response);
        } finally {
            AuditTrace auditTrace = getAuditTrace(request, response);
            // username exists only if it's real service (not css, js, html files)
            if (auditTrace.getUserName() != null) {
                this.logRepository.add(auditTrace);
            }
        }
    }
}
