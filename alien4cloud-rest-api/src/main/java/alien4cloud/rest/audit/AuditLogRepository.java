package alien4cloud.rest.audit;

import java.util.List;

import lombok.extern.slf4j.Slf4j;

import org.springframework.stereotype.Component;

import alien4cloud.audit.model.AuditTrace;

import com.google.common.collect.Lists;

@Component
@Slf4j
public class AuditLogRepository {

    public List<AuditTrace> findAll() {
        return Lists.newArrayList();
    }

    public void add(AuditTrace auditTrace) {
        log.info(auditTrace.toString());
    }
}
