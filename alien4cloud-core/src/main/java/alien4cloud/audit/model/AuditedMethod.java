package alien4cloud.audit.model;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class AuditedMethod extends Method {

    private boolean enabled;

    public AuditedMethod(String path, String method, boolean enabled) {
        super(path, method);
        this.enabled = enabled;
    }
}
