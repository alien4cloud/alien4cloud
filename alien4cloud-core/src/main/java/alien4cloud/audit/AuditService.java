package alien4cloud.audit;

import java.util.Map;

import javax.annotation.Resource;

import lombok.extern.slf4j.Slf4j;

import org.elasticsearch.index.query.FilterBuilder;
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
        if (auditConfiguration == null) {
            this.auditConfiguration = alienDAO.customFind(AuditConfiguration.class, null);
        }
        return this.auditConfiguration;
    }

    public synchronized void saveAuditConfiguration(AuditConfiguration auditConfiguration) {
        this.auditConfiguration = auditConfiguration;
        alienDAO.save(auditConfiguration);
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

    public Method getAuditedMethod(RequestMapping requestMapping) {
        if (requestMapping == null) {
            return null;
        }
        String[] requestMappingPaths = requestMapping.value();
        RequestMethod[] requestMethods = requestMapping.method();
        if (requestMappingPaths.length == 0 || requestMethods.length == 0) {
            return null;
        }
        if (requestMappingPaths.length > 1 || requestMethods.length > 1) {
            log.error("Audit does not support mapping http path more than once to the same Spring Controller method " + requestMapping.value());
            return null;
        }
        return new Method(requestMappingPaths[0], requestMethods[0].name());
    }

    public boolean isMethodAudited(AuditConfiguration auditConfiguration, RequestMapping requestMapping) {
        Method method = getAuditedMethod(requestMapping);
        if (method == null) {
            return false;
        }
        return Boolean.TRUE.equals(auditConfiguration.getAuditedMethodsMap().get(method));
    }
}
