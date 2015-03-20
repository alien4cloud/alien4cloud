package alien4cloud.rest.audit;

import java.io.IOException;
import java.util.Date;

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
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerExecutionChain;
import org.springframework.web.servlet.HandlerMapping;

import alien4cloud.audit.model.AuditTrace;
import alien4cloud.security.AuthorizationUtil;
import alien4cloud.security.User;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.google.common.base.Charsets;

@Component
public class AuditLogFilter extends OncePerRequestFilter implements Ordered {

    @Resource
    private AuditLogRepository logRepository;
    @Resource
    private HandlerMapping mapping;

    private HandlerMethod getHandlerMethod(HttpServletRequest request) throws Exception {
        HandlerExecutionChain handlerChain = mapping.getHandler(request);
        if (handlerChain == null) {
            return null;
        }
        if (!(handlerChain.getHandler() instanceof HandlerMethod)) {
            return null;
        }
        HandlerMethod handlerMethod = (HandlerMethod) handlerChain.getHandler();
        return handlerMethod;
    }

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
    private AuditTrace getAuditTrace(HttpServletRequest request, HttpServletResponse response, User user) throws Exception {
        if (user == null) {
            return null;
        }
        HandlerMethod method = getHandlerMethod(request);
        if (method == null) {
            return null;
        }
        Object controllerBean = method.getBean();
        if (controllerBean == null) {
            return null;
        }
        String auditCategory = controllerBean.getClass().getSimpleName();
        // trace user info only when he is logged
        AuditTrace auditTrace = new AuditTrace();
        auditTrace.setCategory(auditCategory);
        auditTrace.setUserName(user.getUsername());
        auditTrace.setUserFirstName(user.getFirstName());
        auditTrace.setUserLastName(user.getLastName());
        auditTrace.setUserEmail(user.getEmail());

        // request details
        auditTrace.setMethod(request.getMethod());
        auditTrace.setPath(request.getRequestURI());
        auditTrace.setTimestamp((new Date()).getTime());
        auditTrace.setSourceIp(request.getRemoteAddr());

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
        User user = AuthorizationUtil.getCurrentUser();
        String contentType = request.getHeader(HttpHeaders.CONTENT_TYPE);
        if (user != null && contentType != null && contentType.startsWith(MediaType.APPLICATION_JSON_VALUE)) {
            request = new MultiReadHttpServletRequest(request);
        }
        try {
            filterChain.doFilter(request, response);
        } finally {
            AuditTrace auditTrace = null;
            try {
                auditTrace = getAuditTrace(request, response, user);
            } catch (Exception e) {
                logger.warn("Unable to get audit trace", e);
            }
            // username exists only if it's real service (not css, js, html files)
            if (auditTrace != null) {
                this.logRepository.add(auditTrace);
            }
        }
    }
}
