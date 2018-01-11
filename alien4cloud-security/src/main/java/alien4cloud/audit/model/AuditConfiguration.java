package alien4cloud.audit.model;

import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.elasticsearch.annotation.ESObject;
import org.elasticsearch.annotation.Id;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.google.common.collect.Maps;

import lombok.Getter;
import lombok.Setter;

@ESObject
@Getter
@Setter
public class AuditConfiguration {

    public static final String ID = "singleton";
    public static final String FORMATTER = "*";

    @Id
    private String id = ID;

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
            methodsMap.put(new Method(auditedMethod.getSignature(), auditedMethod.getMethod(), auditedMethod.getCategory(), auditedMethod.getAction(),
                    auditedMethod.getBodyHiddenFields()), auditedMethod.isEnabled());
        }
        return methodsMap;
    }

    @JsonIgnore
    public void setAuditedMethodsMap(Map<Method, Boolean> auditedMethodsMap) {
        if (auditedMethodsMap == null) {
            return;
        }
        auditedMethods = auditedMethodsMap.entrySet().stream()
                .map(m -> new AuditedMethod(m.getKey().getSignature(), m.getKey().getMethod(), m.getKey().getCategory(),
                        m.getKey().getAction(), m.getKey().getBodyHiddenFields(), m.getValue()))
                .collect(Collectors.toSet());
    }
}
