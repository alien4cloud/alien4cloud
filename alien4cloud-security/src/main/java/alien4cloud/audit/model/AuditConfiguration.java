package alien4cloud.audit.model;

import java.util.Map;
import java.util.Set;

import lombok.Getter;
import lombok.Setter;

import org.elasticsearch.annotation.ESObject;
import org.elasticsearch.annotation.Id;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;

@ESObject
@Getter
@Setter
public class AuditConfiguration {

    public static final String ID = "singleton";

    @Id
    private String id;

    /**
     * Global flag to enable/disable audit
     */
    private boolean enabled = true;

    /**
     * List of all audited methods if enable flag was to true
     */
    private Set<AuditedMethod> auditedMethods;

    @JsonIgnore
    public Map<Method, Boolean> getAuditedMethodsMap() {
        Map<Method, Boolean> methodsMap = Maps.newHashMap();
        if (auditedMethods == null) {
            return methodsMap;
        }
        for (AuditedMethod auditedMethod : auditedMethods) {
            methodsMap.put(new Method(auditedMethod.getMethod(), auditedMethod.getCategory(), auditedMethod.getAction()),
                    auditedMethod.isEnabled());
        }
        return methodsMap;
    }

    @JsonIgnore
    public void setAuditedMethodsMap(Map<Method, Boolean> auditedMethodsMap) {
        auditedMethods = Sets.newHashSet();
        if (auditedMethodsMap == null) {
            return;
        }
        for (Map.Entry<Method, Boolean> auditedMethodsMapEntry : auditedMethodsMap.entrySet()) {
            auditedMethods.add(new AuditedMethod(auditedMethodsMapEntry.getKey().getMethod(),
                    auditedMethodsMapEntry
                    .getKey().getCategory(), auditedMethodsMapEntry.getKey().getAction(), auditedMethodsMapEntry.getValue()));
        }
    }
}
