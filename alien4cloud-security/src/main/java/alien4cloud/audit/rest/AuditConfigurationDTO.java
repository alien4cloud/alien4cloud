package alien4cloud.audit.rest;

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
@AllArgsConstructor(suppressConstructorProperties = true)
public class AuditConfigurationDTO {

    private boolean enabled;

    private Map<String, List<AuditedMethod>> methodsConfiguration;

}
