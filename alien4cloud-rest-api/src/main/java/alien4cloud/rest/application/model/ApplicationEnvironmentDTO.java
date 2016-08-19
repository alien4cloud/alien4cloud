package alien4cloud.rest.application.model;

import java.util.Map;
import java.util.Set;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import alien4cloud.model.application.EnvironmentType;
import alien4cloud.paas.model.DeploymentStatus;

import com.fasterxml.jackson.annotation.JsonInclude;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor(suppressConstructorProperties = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ApplicationEnvironmentDTO {
    private String id;
    private DeploymentStatus status;
    private String name;
    private String description;
    private String applicationId;
    private EnvironmentType environmentType;
    private String currentVersionName;
    private Map<String, Set<String>> userRoles;
    private Map<String, Set<String>> groupRoles;
}
