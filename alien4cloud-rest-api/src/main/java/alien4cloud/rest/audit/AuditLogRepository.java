package alien4cloud.rest.audit;

import java.util.List;

import javax.annotation.Resource;

import lombok.extern.slf4j.Slf4j;

import org.springframework.stereotype.Component;

import alien4cloud.audit.model.AuditTrace;
import alien4cloud.dao.IGenericSearchDAO;

@Component
@Slf4j
public class AuditLogRepository {

    @Resource(name = "alien-es-dao")
    private IGenericSearchDAO alienDAO;

    public List<AuditTrace> findAll() {
        return alienDAO.customFindAll(AuditTrace.class, null);
    }

    public void add(AuditTrace auditTrace) {
        log.info(auditTrace.toString());
        // save in ES
        alienDAO.save(auditTrace);
    }
}
