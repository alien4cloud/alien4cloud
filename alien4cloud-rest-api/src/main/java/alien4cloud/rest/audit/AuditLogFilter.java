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
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerExecutionChain;
import org.springframework.web.servlet.HandlerMapping;

import alien4cloud.audit.AuditService;
import alien4cloud.audit.annotation.Audit;
import alien4cloud.audit.model.AuditConfiguration;
import alien4cloud.audit.model.AuditTrace;
import alien4cloud.security.AuthorizationUtil;
import alien4cloud.security.User;

import com.google.common.base.Charsets;
import com.wordnik.swagger.annotations.ApiOperation;

/**
 * This filter is used to intercept all rest call that need to be audited
 */
@Component
public class AuditLogFilter extends OncePerRequestFilter implements Ordered {

    @Resource
    private AuditService auditService;

    @Resource
    private HandlerMapping mapping;

    private HandlerMethod getHandlerMethod(HttpServletRequest request) {
        HandlerExecutionChain handlerChain;
        try {
            handlerChain = mapping.getHandler(request);
        } catch (Exception e) {
            logger.warn("Unable to get handler method", e);
            return null;
        }
        if (handlerChain == null) {
            return null;
        }
        if (!(handlerChain.getHandler() instanceof HandlerMethod)) {
            return null;
        }
        HandlerMethod handlerMethod = (HandlerMethod) handlerChain.getHandler();
        return handlerMethod;
    }

    private ApiOperation getApiDoc(HandlerMethod method) {
        return method.getMethodAnnotation(ApiOperation.class);
    }

    private boolean isRequestContainingJson(HttpServletRequest request) {
        String contentType = request.getHeader(HttpHeaders.CONTENT_TYPE);
        return contentType != null && contentType.startsWith(MediaType.APPLICATION_JSON_VALUE);
    }

    @Override
    public int getOrder() {
        return Ordered.LOWEST_PRECEDENCE - 10;
    }

    private AuditTrace getAuditTrace(HttpServletRequest request, HttpServletResponse response, HandlerMethod method, User user, boolean requestContainsJson)
            throws IOException {
        Audit audit = auditService.getAuditAnnotation(method);
        // trace user info only when he is logged
        AuditTrace auditTrace = new AuditTrace();
        auditTrace.setTimestamp(System.currentTimeMillis());
        auditTrace.setAction(auditService.getAuditActionName(method, audit));
        ApiOperation apiDoc = getApiDoc(method);
        if (apiDoc != null) {
            auditTrace.setActionDescription(apiDoc.value());
        }
        auditTrace.setCategory(auditService.getAuditCategoryName(method, audit));
        auditTrace.setUserName(user.getUsername());
        auditTrace.setUserFirstName(user.getFirstName());
        auditTrace.setUserLastName(user.getLastName());
        auditTrace.setUserEmail(user.getEmail());
        // request details
        auditTrace.setPath(request.getRequestURI());
        auditTrace.setMethod(request.getMethod());
        auditTrace.setRequestParameters(request.getParameterMap());
        auditTrace.setSourceIp(request.getRemoteAddr());
        // request body
        if (requestContainsJson) {
            auditTrace.setRequestBody(StreamUtils.copyToString(request.getInputStream(), Charsets.UTF_8));
        }
        // response details
        auditTrace.setResponseStatus(response.getStatus());
        return auditTrace;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
        AuditConfiguration configuration = auditService.getAuditConfiguration();
        if (configuration == null || !configuration.isEnabled()) {
            filterChain.doFilter(request, response);
            return;
        }
        User user = AuthorizationUtil.getCurrentUser();
        if (user == null) {
            filterChain.doFilter(request, response);
            return;
        }
        HandlerMethod method = getHandlerMethod(request);
        if (method == null) {
            filterChain.doFilter(request, response);
            return;
        }
        if (!auditService.isMethodAudited(configuration, method)) {
            filterChain.doFilter(request, response);
            return;
        }
        boolean requestContainsJson = isRequestContainingJson(request);
        if (requestContainsJson) {
            request = new MultiReadHttpServletRequest(request);
        }
        try {
            filterChain.doFilter(request, response);
        } finally {
            AuditTrace auditTrace = null;
            try {
                auditTrace = getAuditTrace(request, response, method, user, requestContainsJson);
            } catch (Exception e) {
                logger.warn("Unable to construct audit trace", e);
            }
            if (auditTrace != null) {
                if (logger.isDebugEnabled()) {
                    logger.debug(auditTrace.toString());
                }
                try {
                    auditService.saveAuditTrace(auditTrace);
                } catch (Exception e) {
                    logger.warn("Unable to save audit trace " + auditTrace, e);
                }
            }
        }
    }
}
