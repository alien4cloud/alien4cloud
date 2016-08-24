package alien4cloud.audit;

import java.beans.IntrospectionException;
import java.io.IOException;

import javax.annotation.PostConstruct;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import alien4cloud.audit.model.AuditConfiguration;
import alien4cloud.audit.model.AuditTrace;
import alien4cloud.dao.ESGenericSearchDAO;
import alien4cloud.exception.IndexingServiceException;

@Component("alien-audit-dao")
public class AuditESDAO extends ESGenericSearchDAO {

    public static final String ALIEN_AUDIT_INDEX = "alienaudit";

    @Value("${audit.ttl}")
    private String auditTtl;

    @PostConstruct
    public void init() {
        try {
            getMappingBuilder().initialize("alien4cloud.audit.model");
        } catch (IntrospectionException | IOException e) {
            throw new IndexingServiceException("Could not initialize elastic search mapping builder", e);
        }
        // Audit trace index
        initIndices(ALIEN_AUDIT_INDEX, auditTtl, AuditTrace.class, AuditConfiguration.class);
        initCompleted();
    }

}