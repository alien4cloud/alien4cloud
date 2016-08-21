package alien4cloud.audit;

import java.util.Map;

import javax.annotation.Resource;

import lombok.extern.slf4j.Slf4j;

import org.apache.commons.lang3.StringUtils;
import org.elasticsearch.index.query.FilterBuilder;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.method.HandlerMethod;

import alien4cloud.audit.annotation.Audit;
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

    public static final String CONTROLLER_SUFFIX = "Controller";

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
            this.auditConfiguration = alienDAO.findById(AuditConfiguration.class, AuditConfiguration.ID);
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
        return alienDAO.facetedSearch(AuditTrace.class, query, filters, authorizationFilter, null, from, size, "timestamp", true);
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

    public Method getAuditedMethod(HandlerMethod controllerMethod) {
        RequestMapping methodMapping = AnnotationUtils.findAnnotation(controllerMethod.getMethod(), RequestMapping.class);
        RequestMapping controllerMapping = AnnotationUtils.findAnnotation(controllerMethod.getMethod().getDeclaringClass(), RequestMapping.class);
        String httpMethod = null;
        if (controllerMapping != null) {
            httpMethod = getRequestMappingMethod(controllerMapping);
            if (methodMapping != null) {
                String methodHttpMethod = getRequestMappingMethod(methodMapping);
                if (httpMethod == null) {
                    // Controller http method override method http method
                    httpMethod = methodHttpMethod;
                }
            }
        } else if (methodMapping != null) {
            httpMethod = getRequestMappingMethod(methodMapping);
        }
        if (httpMethod == null) {
            return null;
        }
        Audit audit = getAuditAnnotation(controllerMethod);
        return new Method(controllerMethod.getMethod().toGenericString(), httpMethod, getAuditCategoryName(controllerMethod, audit), getAuditActionName(
                controllerMethod, audit));
    }

    public boolean isMethodAudited(AuditConfiguration auditConfiguration, HandlerMethod controllerMethod) {
        Method method = getAuditedMethod(controllerMethod);
        return method != null && Boolean.TRUE.equals(auditConfiguration.getAuditedMethodsMap().get(method));
    }

    public String getAuditCategoryName(HandlerMethod method, Audit audit) {
        if (audit != null && StringUtils.isNotBlank(audit.category())) {
            return audit.category();
        }
        String auditCategory = method.getMethod().getDeclaringClass().getSimpleName();
        if (auditCategory.endsWith(CONTROLLER_SUFFIX) && auditCategory.length() > CONTROLLER_SUFFIX.length()) {
            auditCategory = auditCategory.substring(0, auditCategory.length() - CONTROLLER_SUFFIX.length());
        }
        return auditCategory;
    }

    public String getAuditActionName(HandlerMethod method, Audit audit) {
        if (audit != null && StringUtils.isNotBlank(audit.action())) {
            return audit.action();
        }
        return method.getMethod().getName();
    }

    public Audit getAuditAnnotation(HandlerMethod method) {
        Audit audit = method.getMethodAnnotation(Audit.class);
        if (audit == null) {
            audit = AnnotationUtils.findAnnotation(method.getMethod().getDeclaringClass(), Audit.class);
        }
        return audit;
    }

}
