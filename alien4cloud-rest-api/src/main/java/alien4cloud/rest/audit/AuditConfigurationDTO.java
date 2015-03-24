package alien4cloud.rest.audit;

import java.util.List;
import java.util.Map;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import alien4cloud.audit.model.AuditedMethod;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class AuditConfigurationDTO {

    private boolean enabled;

    private Map<String, List<AuditedMethodDTO>> methodsConfiguration;

    @Getter
    @Setter
    @NoArgsConstructor
    public static class AuditedMethodDTO extends AuditedMethod {

        private String category;

        private String action;

        public AuditedMethodDTO(String path, String method, boolean enabled, String category, String action) {
            super(path, method, enabled);
            this.category = category;
            this.action = action;
        }
    }
}
