package alien4cloud.audit.model;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class AuditedMethod extends Method {

    private boolean enabled;

    public AuditedMethod(String signature, String method, String category, String action, String[] bodyHiddenFields, boolean enabled) {
        super(signature, method, category, action, bodyHiddenFields);
        this.enabled = enabled;
    }
}
