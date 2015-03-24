package alien4cloud.audit;

import java.util.Map;

import javax.annotation.Resource;

import lombok.extern.slf4j.Slf4j;

import org.elasticsearch.index.query.FilterBuilder;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

import alien4cloud.audit.model.AuditConfiguration;
import alien4cloud.audit.model.AuditTrace;
import alien4cloud.audit.model.Method;
import alien4cloud.dao.IGenericSearchDAO;
import alien4cloud.dao.model.FacetedSearchResult;
import alien4cloud.exception.NotFoundException;
import alien4cloud.security.AuthorizationUtil;

@Component
@Slf4j
public class AuditService {

    @Resource(name = "alien-audit-dao")
    private IGenericSearchDAO alienDAO;

    /**
     * Cache the instance of audit configuration bad idea ?
     */
    private AuditConfiguration auditConfiguration;

    /**
     * Get the audit configuration, this method will cache the configuration in memory
     * 
     * @return the audit configuration
     */
    public synchronized AuditConfiguration getAuditConfiguration() {
        if (this.auditConfiguration == null) {
            this.auditConfiguration = alienDAO.customFind(AuditConfiguration.class, null);
        }
        return this.auditConfiguration;
    }

    public synchronized void saveAuditConfiguration(AuditConfiguration auditConfiguration) {
        alienDAO.save(auditConfiguration);
        this.auditConfiguration = auditConfiguration;
    }

    public void saveAuditTrace(AuditTrace auditTrace) {
        alienDAO.save(auditTrace);
    }

    public AuditConfiguration getMandatoryAuditConfiguration() {
        AuditConfiguration auditConfiguration = getAuditConfiguration();
        if (auditConfiguration == null) {
            throw new NotFoundException("Audit configuration not found");
        }
        return auditConfiguration;
    }

    public FacetedSearchResult searchAuditTrace(String query, Map<String, String[]> filters, int from, int size) {
        FilterBuilder authorizationFilter = AuthorizationUtil.getResourceAuthorizationFilters();
        return alienDAO.facetedSearch(AuditTrace.class, query, filters, authorizationFilter, null, from, size);
    }

    private String getRequestMappingPath(RequestMapping requestMapping) {
        String[] paths = requestMapping.value();
        if (paths.length == 0) {
            return null;
        }
        if (paths.length > 1) {
            log.error("Audit does not support mapping http path more than once to the same Spring Controller method");
            return null;
        }
        return paths[0];
    }

    private String getRequestMappingMethod(RequestMapping requestMapping) {
        RequestMethod[] methods = requestMapping.method();
        if (methods.length == 0) {
            return null;
        }
        if (methods.length > 1) {
            log.error("Audit does not support mapping http method more than once to the same Spring Controller method");
            return null;
        }
        return methods[0].toString();
    }

    public Method getAuditedMethod(java.lang.reflect.Method controllerMethod) {
        RequestMapping methodMapping = AnnotationUtils.findAnnotation(controllerMethod, RequestMapping.class);
        if (methodMapping == null) {
            return null;
        }
        RequestMapping controllerMapping = AnnotationUtils.findAnnotation(controllerMethod.getDeclaringClass(), RequestMapping.class);
        String contextPath = null;
        String httpMethod = null;
        if (controllerMapping != null) {
            httpMethod = getRequestMappingMethod(controllerMapping);
            contextPath = getRequestMappingPath(controllerMapping);
            if (methodMapping != null) {
                String methodContextPath = getRequestMappingPath(methodMapping);
                String methodHttpMethod = getRequestMappingMethod(methodMapping);
                if (contextPath == null) {
                    contextPath = methodContextPath;
                } else if (methodContextPath != null) {
                    // Concatenate controller context path to the method context path
                    contextPath += methodContextPath;
                }
                if (httpMethod == null) {
                    // Controller http method override method http method
                    httpMethod = methodHttpMethod;
                }
            }
        } else if (methodMapping != null) {
            contextPath = getRequestMappingPath(methodMapping);
            httpMethod = getRequestMappingMethod(methodMapping);
        }
        if (contextPath == null || httpMethod == null) {
            return null;
        }
        return new Method(contextPath, httpMethod);
    }

    public boolean isMethodAudited(AuditConfiguration auditConfiguration, java.lang.reflect.Method javaMethod) {
        Method method = getAuditedMethod(javaMethod);
        if (method == null) {
            return false;
        }
        return Boolean.TRUE.equals(auditConfiguration.getAuditedMethodsMap().get(method));
    }
}
